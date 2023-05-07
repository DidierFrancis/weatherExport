package sun.wheatherExport.vdlFiles


import grails.converters.JSON
import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.apache.commons.lang.time.DateUtils
import org.springframework.http.HttpStatus
import sun.constante.Constantes
import sun.constante.TypeVDL
import sun.exception.SaqException
import sun.helpers.DateUtilExtensions
import sun.security.User

class VdlFileController {

    public static final String SERVER_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    def vdlFileService
    def springSecurityService
    def csvToolService
    def dataSource
    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]


    @Transactional
    def importFile() {
        try {
            def username = springSecurityService?.getPrincipal()?.username
            User connectedUser = User.findByUsername(username)
            if (!request.getParameter('typeFile'))
                throw new SaqException("4000", "veuillez renseigner le typeFile")

            def type = TypeVDL.valueOf(request.getParameter('typeFile'))
            def file = new VDLFile()
            file.typeVDL = type
            file.importedBy = connectedUser
            def line = 0
            def rows = []
            def fichier = request.getFile("fichier")
            file.fileName = fichier.getOriginalFilename()
            if (VDLFile.findByFileName(file.fileName)) {
                throw new SaqException("4000", "Le fichier ${file.fileName} a déjà été uploader !")
            }
            fichier.inputStream.eachLine { l ->
                l = l.replaceAll('"', "")

                List<String> l1 = l.split(',')
                if (type != TypeVDL.TREND) {
                    if (l1[0] == 'RecipeName:') {
                        file.recipeName = l1[1]
                    }
                    if (l1[0] == 'setSize:') {
                        if (l1[0] && l1[0] != "")
                            file.sizeSet = l1[1].toLong()
                    }
                    if (l1[0] == 'id:') {
                        if (l1[0] && l1[0] != "")
                            file.fileId = l1[1].toLong()
                    }
                    if (line > 3) {
                        rows << l1
                    }
                    line++

                } else {

                    rows << l1
                }
            }
            file.fileName = fichier.getOriginalFilename()
            file.save()

            def serviceResponse = vdlFileService.createVdlData(rows, file)
            if (!serviceResponse.serviceOk)
                throw new Exception(serviceResponse.message)

            def responseData = [
                    id        : file.id,
                    fileName  : file.fileName,
                    recipeName: file.recipeName,
                    user      : file?.importedBy?.displayName
            ]
            render getJsonSuccess(responseData, "Fichier ${file.fileName} traité avec succès !") as JSON
            return

        } catch (SaqException ex) {
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            render getJsonError(4000, ex.message) as JSON
            return
        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            render getJsonError(5000, "Une erreur est survenu lors du traitement de donnée.") as JSON
            return
        }

    }

    @Transactional
    def getFileData() {
        try {
            def type
            def vdlBase = grailsApplication.mainContext.getResource("/excelFixtures/VDL_DB_Base.csv").getFile()
            def vdlCustomer = grailsApplication.mainContext.getResource("/excelFixtures/VDL_DB_Customer.csv").getFile()
            def vdlMaster = grailsApplication.mainContext.getResource("/excelFixtures/VDL_DB_Master.csv").getFile()
            def vdlSite = grailsApplication.mainContext.getResource("/excelFixtures/VDL_DB_Site.csv").getFile()
            def files = [
                    [file: "vdlBase", type: TypeVDL.BASE],
                    [file: "vdlCustomer", type: TypeVDL.CUSTOMER],
                    [file: "vdlMaster", type: TypeVDL.MASTER],
                    [file: "vdlSite", type: TypeVDL.SITE]
            ]
            files.each { fl ->
                def file = new VDLFile()
                def line = 0
                def rows = []
                def fichier
                if (fl.type == TypeVDL.BASE) {
                    fichier = vdlBase
                    type = TypeVDL.BASE
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.CUSTOMER) {
                    fichier = vdlCustomer
                    type = TypeVDL.CUSTOMER
                    file.typeVDL = type


                }
                if (fl.type == TypeVDL.MASTER) {
                    fichier = vdlMaster
                    type = TypeVDL.MASTER
                    file.typeVDL = type


                }
                if (fl.type == TypeVDL.SITE) {
                    fichier = vdlSite
                    type = TypeVDL.SITE
                    file.typeVDL = type
                }
                file.fileName = fichier.getOriginalFilename()
                if (!VDLFile.findByFileName(file.fileName)) {

                    fichier.inputStream.eachLine { l ->
                        l = l.replaceAll('"', "")

                        List<String> l1 = l.split(',')
                        if (type != TypeVDL.TREND) {
                            if (l1[0] == 'RecipeName:') {
                                file.recipeName = l1[1]
                            }
                            if (l1[0] == 'setSize:') {
                                if (l1[0] && l1[0] != "")
                                    file.sizeSet = l1[1].toLong()
                            }
                            if (l1[0] == 'id:') {
                                if (l1[0] && l1[0] != "")
                                    file.fileId = l1[1].toLong()
                            }
                            if (line > 3) {
                                rows << l1
                            }
                            line++

                        } else {

                            rows << l1
                        }
                    }
                    file.fileName = fichier.getOriginalFilename()
                    file.save()

                    def serviceResponse = vdlFileService.createVdlData(rows, file)
                    if (!serviceResponse.serviceOk)
                        throw new Exception(serviceResponse.message)

                    def responseData = [
                            id        : file.id,
                            fileName  : file.fileName,
                            recipeName: file.recipeName,
                    ]
                }

            }
        } catch (SaqException ex) {
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            return
        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            return
        }

    }

//    def generateMatrix() {
//        try {
//
//            def matrixName = "matrix_" + DateUtilExtensions.format(new Date(), "ddMMyyyyhhmm") + ".xlsx"
//            def fileName = Constantes.FILE_MATRIX_PATH + matrixName
//            def file = new File(fileName)
//
//            // def voyages =  BruteData.executeQuery( "select count(distinct(voyageId)) from BruteData ")[0] ?: 1
//            if (file.exists() && !params.force) {
//                render(file: file, fileName: file.name, contentType: "text/csv")
//                return
//            }
//
//            def matrixTemplate = grailsApplication.mainContext.getResource("/excelFixtures/matrix.xlsx").getFile()
//            vdlFileService.generateMatrix(matrixTemplate, fileName, 1)
//
//            def file1 = new File(fileName)
//            if (!file.exists()) {
//                throw new Exception("Une erreur est survenu lors du telechargement !")
//            }
//
//            render(file: file1, fileName: fileName, contentType: "application/vnd.ms-excel")
//            return
//
//        } catch (Exception e) {
//            e.printStackTrace()
//            render getJsonError(5000, e.message) as JSON
//            return
//        }
//    }


//    def generateManuelyBruteFile() {
//        try {
//
//            def date = DateUtilExtensions.format(new Date(), "yyyy-MM-dd")
//            def title = "Trends_EC-DJ-${date}.csv"
//            def data = vdlFileService.exportBrutData();
//
//            render getJsonSuccess(data, "Fichier $title a été générer avec succès") as JSON
//            return
//
//        } catch (Exception e) {
//            e.printStackTrace()
//            render getJsonError(5000, "Erreur lors de la génération du fichier Excel") as JSON
//            return
//        }
//    }

    def downloadBruteFile() {
        try {

            def date = (params.date) ? DateUtils.parseDate(params.date, DateUtilExtensions.listPattern()) : new Date()
            def formatedDate = DateUtilExtensions.format(date, "yyyy-MM-dd")
            def title = "Trends_EC-DJ-${formatedDate}.csv"
            String tempDir = csvToolService.getFolderDir() // pour les problèmes de séparateur
            def file = new File(tempDir + title)
            if (!file.exists()) {
                throw new SaqException("4000", "Aucun fichier generé a la date: ${formatedDate} ")
            }

            render(file: file, fileName: title, contentType: "text/csv")
            return

        } catch (SaqException e) {
            render getJsonError(4000, e.message) as JSON
            return
        } catch (Exception e) {
            e.printStackTrace()
            render getJsonError(5000, "Une Erreur lors du telechargement du fichier Excel") as JSON
            return
        }

    }
    // Download de fichier généré dans le répertoire temporaire du systéme

}