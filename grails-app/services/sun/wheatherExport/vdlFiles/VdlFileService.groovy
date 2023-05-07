package sun.wheatherExport.vdlFiles

import com.google.common.io.Files
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.StaticMapsApi
import com.google.maps.StaticMapsRequest
import com.google.maps.model.EncodedPolyline
import com.google.maps.model.Geometry
import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import io.quickchart.QuickChart
import org.apache.commons.lang.time.DateUtils
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.util.IOUtils
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.ini4j.Ini
import org.ini4j.IniPreferences
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import sun.constante.Constantes
import sun.constante.TrendsDictionary
import sun.constante.TypeVDL
import sun.exception.SaqException
import sun.helpers.DateUtilExtensions
import sun.weatherExport.ServiceResponse
import sun.wheatherExport.Forecast
import sun.wheatherExport.Helper
import sun.wheatherExport.PathFile
import com.google.maps.StaticMapsRequest.ImageFormat;
import com.google.maps.StaticMapsRequest.Markers;
import com.google.maps.StaticMapsRequest.Markers.CustomIconAnchor;
import com.google.maps.StaticMapsRequest.Markers.MarkersSize;
import com.google.maps.StaticMapsRequest.Path;
import com.google.maps.StaticMapsRequest.StaticMapType;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import com.google.maps.model.Size;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.text.ParseException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

@Transactional
class VdlFileService {
    def vdlFileService
    def springSecurityService
    def csvToolService
    def dataSource
    def mapService
    @Autowired
    ApplicationContext applicationContext
    ExecutorService executor = Executors.newSingleThreadExecutor()// declarer tout en haut comme un singleton


    private static final String[] ORIENTATIONS = "N/S/E/W".split("/");

    def createVdlData(def rows, VDLFile file) {

        def serviceResponse = new ServiceResponse()
        try {
            def helper = Helper.first()
            def lbrute

            switch (file.typeVDL) {
                case TypeVDL.BASE:
                    for (int i = 0; i < file.sizeSet; i++) {
                        def vdlBase = new VDLBaseData()
                        vdlBase.file = file
                        rows.each { r ->
                            if (r[0] == "Base Name") {
                                vdlBase.baseNameTag = r[1]
                                vdlBase.baseName = r[i + 2]
                            }
                            if (r[0] == "Latitude") {
                                vdlBase.latitudeTag = r[1]
                                vdlBase.latitude = r[i + 2]

                            }
                            if (r[0] == "Longitude") {
                                vdlBase.longitudeTag = r[1]
                                vdlBase.longitude = r[i + 2]

                            }
                            if (r[0] == "Circle") {
                                vdlBase.circleTag = r[1]
                                vdlBase.circle = r[i + 2]
                            }
                            vdlBase.colIndex = i

                        }
                        vdlBase.save(flush: true)
                        print("vld_base :": rows)
                    }
                    break

                case TypeVDL.CUSTOMER:
                    for (int i = 0; i < file.sizeSet; i++) {
                        def vdlCustomer = new VDLCustomerData()
                        vdlCustomer.file = file
                        rows.each { r ->
                            if (r[0] == "Customer Name") {
                                vdlCustomer.tag = r[1]
                                vdlCustomer.customerName = r[i + 2]
                            }
                            vdlCustomer.colIndex = i
                        }
                        vdlCustomer.save()
                    }
                    break

                case TypeVDL.MASTER:
                    for (int i = 0; i < file.sizeSet; i++) {
                        def vdlMaster = new VDLMasterData()
                        vdlMaster.file = file
                        rows.each { r ->
                            if (r[0] == "Master Name") {
                                vdlMaster.tag = r[1]
                                vdlMaster.masterName = r[i + 2]
                            }
                            vdlMaster.colIndex = i
                        }
                        vdlMaster.save()
                    }
                    break

                case TypeVDL.SITE:

                    rows.each { r ->
                        if (r[0] != "ElementName") {
                            def vdlSite = new VDLSiteData()
                            vdlSite.file = file
                            vdlSite.elementName = r[0]
                            vdlSite.tag = r[1]
                            file.sizeSet.times {
                                vdlSite.("site" + it) = r[it + 2]
                            }
                            vdlSite.save()
                        }
                    }
                    break

                case TypeVDL.DOCKING:
                    rows.each { r ->
                        if (r[0] != "ElementName") {
                            def vdlDocking = new VDLDockingData()
                            vdlDocking.file = file
                            vdlDocking.elementName = r[0]
                            vdlDocking.tag = r[1]
                            file.sizeSet.times {
                                vdlDocking.("set" + it) = r[it + 2]
                            }
                            vdlDocking.save()
                        }
                    }
                    break

//                case TypeVDL.VOYAGE:
//                    rows.each { r ->
//                        if (r[0] != "ElementName") {
//                            def vdlVoyage = new VDLVoyageData()
//                            vdlVoyage.file = file
//                            vdlVoyage.elementName = r[0]
//                            vdlVoyage.tag = r[1]
//                            println(rowV: r)
//                            file.sizeSet.times {
//                                vdlVoyage.("set" + it) = r[it + 2]
//                            }
//                            vdlVoyage.save()
//                        }
//                    }
//                    break
                case TypeVDL.TREND:
                    def ai = []
                    rows.each { r ->

                        if (r[0].contains("Timestamp")) {
                            ai = r.findAll { it?.startsWith("AI") }.collect { it.replaceAll('\\(float\\)', "").replaceAll('\\(unsignedInt\\)', "").replaceAll('\\(unsignedShort\\)', "") }
                        }

                        if (!r[0].contains("Timestamp")) {
                            def bruteData = new BruteData()
                            bruteData.file = file
                            try {
                                if (r[0] != "") {
                                    def date = DateUtils.parseDate(r[0] as String, DateUtilExtensions.listPattern())
                                    bruteData.dateTimeTrend = date

                                    if (!lbrute && date > DateUtilExtensions.getDateAtLastHour(helper.dateRapport))
                                        lbrute = BruteData.findAllByDateTimeTrendBetweenAndVoyageIdNotEqual(helper.dateRapport, DateUtilExtensions.plus(helper.dateRapport, 1), 0
                                                , [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]
                                }


                            } catch (ParseException ex) {
                                println("error: ParseException | message: $ex.message ")
                            }

                            def mapArr = []
                            for (int i = 0; i < ai.size(); i++) {
                                def map = [:]
                                r[i + 1] = (r[i + 1] == '') ? 0 : r[i + 1]
                                map[ai[i]] = new Double(r[i + 1])
                                mapArr << map

                                if (ai[i] == TrendsDictionary.FENDERS_PUSH_FORCE_SENSORS) {
                                    bruteData.fenderPushForce = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ROOL) {
                                    bruteData.rool = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.CPP_PS_PITCH) {
                                    bruteData.cppPsPitch = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.CPP_SB_PITCH) {
                                    bruteData.cppSbPitch = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.CPP_SB_MODE) {
                                    bruteData.cppMode = r[i + 1]
                                }
                                if (ai[i] == TrendsDictionary.ME_PS_USE_HOURS) {
                                    bruteData.mePsUseHr = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ME_STB_USE_HOURS) {
                                    bruteData.meStbUseHr = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.GE_PS_RUNNING_HOURS) {
                                    bruteData.geRunningHr = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.GE_STB_RUNNING_HOURS) {
                                    bruteData.geStbRunningHr = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.SPEED_OVER_GROUND) {
                                    bruteData.speedOverGround = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ME_PS_SPEED_RPM) {
                                    bruteData.mePsSpeedRpm = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ME_STB_SPEED_RPM) {
                                    bruteData.meStbSpeedRpm = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ELEC_MOTOR_PS_POWER_OUTPUT) {
                                    bruteData.elecMotorPs = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.ELEC_MOTOR_STB_POWER_OUTPUT) {
                                    bruteData.elecMotorStb = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_PS_STATE_OF_CHARGE) {
                                    bruteData.batteriePsState = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_STB_STATE_OF_CHARGE) {
                                    bruteData.batterieSbState = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_STB_STATE_OF_HEALTH) {
                                    bruteData.batterieSbStateHeath = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_PS_STATE_OF_HEALTH) {
                                    bruteData.batteriePsStateHeath = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_SB_POWER_OUTPUT) {
                                    bruteData.batterieSbPowerOutput = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.BATTERY_PS_POWER_OUTPUT) {
                                    bruteData.batteriePsPowerOutput = new Double(r[i + 1])
                                }
                                if (ai[i] == TrendsDictionary.GPS_LATITUDE) {
                                    def gpsLat = r[i + 1]
                                    //.replaceAll("E\\+001", "").replaceAll("E\\+002", "").replaceAll("E\\+003", "").replaceAll("E\\+004", "").replaceAll("E\\+005", "").replaceAll("E\\+", "")
                                    bruteData.gpsLat = new Float(gpsLat)
                                }
                                if (ai[i] == TrendsDictionary.GPS_LONGITUDE) {
                                    def gpsLon = r[i + 1]
                                    //.replaceAll("E\\+001", "").replaceAll("E\\+002", "").replaceAll("E\\+003", "").replaceAll("E\\+004", "").replaceAll("E\\+005", "").replaceAll("E\\+", "")
                                    bruteData.gpsLon = new Float(gpsLon)
                                }
                                if (ai[i] == TrendsDictionary.YAW_DEVIATION) {
                                    bruteData.dockingHeadingDeviation = new Float(r[i + 1])
                                }


                            }

                            bruteData.aiValues = JsonOutput.toJson(mapArr)
                            if (r[-7] && r[-7] != '') {
                                bruteData.voyageId = Integer.parseInt(r[-7])

                                if (lbrute && DateUtilExtensions.plus(helper.dateRapport, 1) < bruteData.dateTimeTrend) {
                                    if (lbrute.voyageId != bruteData.voyageId) {
                                        helper.fileBruteLoaded = true
                                        helper.save()
                                        serviceResponse.serviceOk = false
                                        return serviceResponse
                                    }
                                }
                            }
                            if (r[-6] && r[-6] != '')
                                bruteData.dockingId = Integer.parseInt(r[-6])
//                            if (r[-5] && r[-5] != '')
//                                bruteData.dockingHeadingDeviation = new BigDecimal(r[-5])
                            if (r[-5] && r[-5] != '')
                                bruteData.ossId = Integer.parseInt(r[-5])
                            if (r[-4] && r[-4] != '')
                                bruteData.assetId = Integer.parseInt(r[-4])
                            if (r[-3] && r[-3] != '')
                                bruteData.onSiteId = Integer.parseInt(r[-3])
                            if (r[-2] && r[-2] != '')
                                bruteData.dockingStable = Integer.parseInt(r[-2])
                            bruteData.save()
                        }
                    }
                    break
            }

            serviceResponse.serviceOk = true
            serviceResponse.objetInstance = file
        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "createVdlData", message: ex.message]))
            throw new Exception(ex.message)
        }

        return serviceResponse
    }

    def loadBruteDataFileDb(InputStream inputStream) {
        try {
            log.info("Chargement fichier dico trends base frangmente ...")
            XSSFWorkbook book = new XSSFWorkbook(inputStream);
            XSSFSheet[] sheets = book.sheets;
            def db1 = []
            def db2 = []
            def db3 = []
            for (XSSFSheet sheet : sheets) {
                int nombreTotalRows = sheet.getLastRowNum()
                log.info("nombreTotalRows: ${nombreTotalRows}")
                for (int i = 1; i <= nombreTotalRows; i++) {


                    Row row = sheet.getRow(i)

                    //// log.info("recuperation colone bd trends: traitement de la ligne N° ${i}")

                    if (!isRowEmpty(row) && i >= 5) {
                        Iterator cellIterator = row.cellIterator()


                        while (cellIterator.hasNext()) {
                            XSSFCell cell = cellIterator.next()
                            def title = ""
                            def description = ""
                            switch (cell.columnIndex) {
                                case 0:
                                    if (title != "")
                                        title = row.getCell(0).getStringCellValue().replaceAll('\\(float\\)', "").replaceAll('\\(unsignedInt\\)', "").replaceAll('\\(unsignedShort\\)', "")
                                    break
                                case 2:
                                    description = cell.getStringCellValue()
                                    break
                                case 6:
                                    def val = cell.getStringCellValue()
                                    if (val == "x") {
                                        db1 << row.getCell(0).getStringCellValue().replaceAll('\\(float\\)', "").replaceAll('\\(unsignedInt\\)', "").replaceAll('\\(unsignedShort\\)', "")
                                    }
                                    break
                                case 7:
                                    def val = cell.getStringCellValue()
                                    if (val == "x") {
                                        db2 << row.getCell(0).getStringCellValue().replaceAll('\\(float\\)', "").replaceAll('\\(unsignedInt\\)', "").replaceAll('\\(unsignedShort\\)', "")
                                    }
                                    break

                                case 8:
                                    def val = cell.getStringCellValue()
                                    if (val == "x") {
                                        db3 << row.getCell(0).getStringCellValue().replaceAll('\\(float\\)', "").replaceAll('\\(unsignedInt\\)', "").replaceAll('\\(unsignedShort\\)', "")
                                    }
                                    break


                            }

                        }
                    }

                }
            }
            log.info("Chargement fichier dico trends base frangmente termine ! ")

            return [db1: db1, db2: db2, db3: db3]

        }
        catch (Exception ex) {
            ex.printStackTrace()

            log.error(ex.getMessage())
            return null
        }


    }

    def generateAllBruteData() {
        def FILE_DICO_TRENDS = PathFile.findByName("FILE_DICO_TRENDS")
        if (!FILE_DICO_TRENDS)
            throw new SaqException("4004", "FILE_DICO_TRENDS not found !")

        def dbDicoFile = new File(FILE_DICO_TRENDS.path)
        if (!dbDicoFile.exists()) {
            log.info("le fichier dico : $FILE_DICO_TRENDS.path est introuvable")
            return
        }
        def dbs = loadBruteDataFileDb(dbDicoFile.newDataInputStream())


        generateBruteData(dbs.db1, "EC92159_001-DJ", 1)
        generateBruteData(dbs.db2, "EC92160_001-DJ", 2)
        generateBruteData(dbs.db3, "EC92161_001-DJ", 3)

        Runtime.getRuntime().exit(0)
        System.exit(0)


    }

    def generateBruteData(def dbCol, def fileName, def index) {
        log.info(JsonOutput.toJson([methode: "generateBruteData", fichier: fileName]))
        def FILE_DATE = PathFile.findByName("FILE_MATRIX_PATH")
        if (!FILE_DATE)
            throw new SaqException("4004", "FILE_MATRIX_PATH not found !")

//        Ini ini = new Ini(new File(FILE_DATE.path + "/rapport.ini"));
//        Preferences prefs = new IniPreferences(ini);
        def dateTrends = Helper?.first()?.dateRapport
        log.info("Date Rapport: " + dateTrends);

        def db = new Sql(dataSource)
        def dateDebut = DateUtilExtensions.getDayStart()
        def dateFin = DateUtilExtensions.getDayEnd()
        def jsonSlurper = new JsonSlurper()
        def reqDate = new Date()
        def endDate = DateUtilExtensions.plus(new Date(), 1)
        def date
        if (!dateTrends) {
            throw new SaqException("4004", "Date rapport non defini !")
        } else {
            date = DateUtilExtensions.format(dateTrends, "yyyy-MM-dd")
            endDate = DateUtilExtensions.format(DateUtilExtensions.plus(dateTrends, 1), "yyyy-MM-dd")

        }

        def totalCount = BruteData.count()
        println(totalCount)
        def FILE_BRUTE_PATH = ""
        if (index == 1) {
            def FILE_BRUTE_PATH1 = PathFile.findByName("FILE_BRUTE_PATH1")
            if (!FILE_BRUTE_PATH1)
                throw new SaqException("4004", "FILE_BRUTE_PATH1 not found !")
            FILE_BRUTE_PATH = FILE_BRUTE_PATH1.path

        }
        if (index == 2) {
            def FILE_BRUTE_PATH2 = PathFile.findByName("FILE_BRUTE_PATH2")
            if (!FILE_BRUTE_PATH2)
                throw new SaqException("4004", "FILE_BRUTE_PATH2 not found !")
            FILE_BRUTE_PATH = FILE_BRUTE_PATH2.path

        }
        if (index == 3) {
            def FILE_BRUTE_PATH3 = PathFile.findByName("FILE_BRUTE_PATH3")
            if (!FILE_BRUTE_PATH3)
                throw new SaqException("4004", "FILE_BRUTE_PATH3 not found !")
            FILE_BRUTE_PATH = FILE_BRUTE_PATH3.path

        }
        def directory = new File(FILE_BRUTE_PATH)
        if (!directory.exists()) {
            directory.mkdir();
        }
        println(FILE_BRUTE_PATH)
        def title = "$fileName-${date}.csv"
        def justForTitle = true
        def libelleColonne = ["Timestamp"]
        def donnees = []
        def ligne = []
        def pages = Math.round(totalCount / 10000)
        pages.times { p ->
            def offset = p * 10000
            println('etape generation lot N. ': offset)
            def sql = "SELECT date_time_trend           AS dateTimeTrend,\n" +
                    "       ai_values                 AS aiValues,\n" +
                    "       voyage_id                 AS voyageId,\n" +
                    "       docking_id                AS dockingId,\n" +
                    "       docking_heading_deviation AS dockingHeadingDeviation,\n" +
                    "       oss_id                    AS ossId,\n" +
                    "       asset_id                  AS assetId,\n" +
                    "       on_site_id                AS onSiteId,\n" +
                    "       docking_stable            AS dockingStable\n" +
                    "FROM brute_data  where date_time_trend between '$date' and '$endDate'  order by date_time_trend asc limit 10000 offset " + offset

            def donneeBrutes = db.rows(sql)
            //def donneeBrutes = BruteData.findAll([max: 500])

            donneeBrutes.each {
                ligne = []
                ligne.add(it.dateTimeTrend)
                if (it.aiValues) {
                    def inputAi = (InputStream) it.aiValues.getAsciiStream()
                    def object = jsonSlurper.parse(inputAi)
                    if (object) {
                        //return Lazy Map

                        object.each { o ->
                            o = (Map) o
                            o.keySet().each { i ->
                                if (dbCol.find { it.contains(i) }) {
                                    if (justForTitle) {
                                        libelleColonne << i
                                    }
                                    ligne.add(o.get(i))
                                }
                            }
                        }
                        if (justForTitle) {
                            libelleColonne.add("ASSET ID")
                            libelleColonne.add("DOCKING ID")
                            libelleColonne.add("DOCKING HEADING DEVIATION")
                            libelleColonne.add("DOCKING STABLE")
                            libelleColonne.add("ON SITE")
                            libelleColonne.add("OSS ID")
                            libelleColonne.add("VOYAGE ID")
                            csvToolService.writeHeader(libelleColonne, title, FILE_BRUTE_PATH)
                        }
                        justForTitle = false

                    }
                }
                ligne.add(it.assetId)
                ligne.add(it.dockingId)
                ligne.add(it.dockingHeadingDeviation)
                ligne.add(it.dockingStable)
                ligne.add(it.onSiteId)
                ligne.add(it.ossId)
                ligne.add(it.voyageId)
                csvToolService.writeLine(ligne, title, FILE_BRUTE_PATH)
            }
        }


        return [fileName: title, totalLines: totalCount]


    }

    @Transactional
    def getFileData() {
        try {
            log.info("===============| CHARGEMENT FICHIER ET REPERTOIRE |==================")
            def helper = Helper.first()
            def formatedDateRapport = DateUtilExtensions.format(helper.dateRapport, "dd-MM-yyyy")

            def FOLDER_TRENDS = PathFile.findByName("FOLDER_TRENDS")
            if (!FOLDER_TRENDS)
                throw new SaqException("4004", "FOLDER_TRENDS not found !")


            def FOLDER_DOCKING = PathFile.findByName("FOLDER_DOCKING")
            if (!FOLDER_DOCKING)
                throw new SaqException("4004", "FOLDER_DOCKING not found !")

            def FILE_VDL_BASE = PathFile.findByName("FILE_VDL_BASE")
            if (!FILE_VDL_BASE)
                throw new SaqException("4004", "FILE_VDL_BASE not found !")
            def FILE_VDL_CUSTOMER = PathFile.findByName("FILE_VDL_CUSTOMER")
            if (!FILE_VDL_CUSTOMER)
                throw new SaqException("4004", "FILE_VDL_CUSTOMER not found !")
            def FILE_VDL_MASTER = PathFile.findByName("FILE_VDL_MASTER")
            if (!FILE_VDL_MASTER)
                throw new SaqException("4004", "FILE_VDL_MASTER not found !")
            def FILE_VDL_SITE = PathFile.findByName("FILE_VDL_SITE")
            if (!FILE_VDL_SITE)
                throw new SaqException("4004", "FILE_VDL_SITE not found !")

            log.info("===============| DEMARAGE IMPORTATION DE DONNEES |==================")
            def forderTrends = new File(FOLDER_TRENDS.path)
            def forderDocking = new File(FOLDER_DOCKING.path)
            if (!forderTrends.exists())
                throw new SaqException("5000", "Repertoire de fichier trends introuvable")
            if (!forderDocking.exists())
                throw new SaqException("5000", "Repertoire de fichier docking introuvable")

            def type
            def vdlBase = new File(FILE_VDL_BASE.path)
            def vdlCustomer = new File(FILE_VDL_CUSTOMER.path)
            def vdlMaster = new File(FILE_VDL_MASTER.path)
            def vdlSite = new File(FILE_VDL_SITE.path)
            def files = [
                    [file: (InputStream) vdlBase.newInputStream(), name: vdlBase.name, type: TypeVDL.BASE],
                    [file: (InputStream) vdlCustomer.newInputStream(), name: vdlCustomer.name, type: TypeVDL.CUSTOMER],
                    [file: (InputStream) vdlMaster.newInputStream(), name: vdlMaster.name, type: TypeVDL.MASTER],
                    [file: (InputStream) vdlSite.newInputStream(), name: vdlSite.name, type: TypeVDL.SITE]
            ]


            forderDocking.eachFile {
                if (it.name.contains("DK") && it.name.contains(formatedDateRapport)) {
                    files << [file: (InputStream) it.newInputStream(), name: it.name, type: TypeVDL.DOCKING]
                }
            }

            forderTrends.eachFile {
                if (it.name.contains("Trend"))
                    files << [file: (InputStream) it.newInputStream(), name: it.name, type: TypeVDL.TREND]
            }
            forderTrends = null

            def indx = 0
            files.each { fl ->
                if (Helper.first().fileBruteLoaded)
                    return true

                log.info("fichier => ${fl.name} | type => ${fl.type} | status=> chargement: encour...")

                def file = new VDLFile()
                def line = 0
                def rows = []
                def fichier = fl.file
                file.fileName = fl.name

                if (fl.type == TypeVDL.BASE) {
                    type = TypeVDL.BASE
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.CUSTOMER) {
                    type = TypeVDL.CUSTOMER
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.MASTER) {
                    type = TypeVDL.MASTER
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.SITE) {
                    type = TypeVDL.SITE
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.DOCKING) {
                    type = TypeVDL.DOCKING
                    file.typeVDL = type
                }
                if (fl.type == TypeVDL.TREND) {
                    type = TypeVDL.TREND
                    file.typeVDL = type
                }

                if (!VDLFile.findByFileName(file.fileName)) {

                    fichier.eachLine { l ->
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
                    file.save()

                    def serviceResponse = vdlFileService.createVdlData(rows, file)
                    if (!serviceResponse.serviceOk)
                        return true

                }
                log.info("fichier => ${fl.name} | type => ${fl.type} | status=> chargement: terminé !")
                fl = null
            }
        } catch (SaqException ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            return
        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "importFileClients", message: ex.message]))
            return
        }

    }

    def generateMatrix() {
        try {
            log.info(JsonOutput.toJson([methode: "generateMatrix", step: "DEMARAGE GENERATION MATRIX"]))
            def FILE_MATRIX_PATH = PathFile.findByName("FILE_MATRIX_PATH")
            if (!FILE_MATRIX_PATH)
                throw new SaqException("4004", "FILE_MATRIX_PATH not found !")
            def FILE_MATRIX_TEMPLATE = PathFile.findByName("FILE_MATRIX_TEMPLATE")
            if (!FILE_MATRIX_TEMPLATE)
                throw new SaqException("4004", "FILE_MATRIX_TEMPLATE not found !")


            Ini ini = new Ini(new File(FILE_MATRIX_PATH.path + "/rapport.ini"));
            Preferences prefs = new IniPreferences(ini);
//            def dateRapportStr = prefs.node("rapport")?.get("date", null)
//            if (!dateRapportStr)
//                throw new SaqException("4004", "Date rapport not defined !")

            def dateRapport = Helper?.first()?.dateRapport


            def voyages = VDLDockingData.findByElementNameIlike("%iCTV85_VoyageID%")?.set0
            voyages = Integer.parseInt(voyages)

            def fileDocks = VDLDockingData.findAllByElementNameAndSet0("iCTV85_VoyageID", "$voyages")
            if (fileDocks.isEmpty())
                throw new SaqException("4000", "Aucun Docking trouvé pour le voyage $voyages")

            def dockingDatas = VDLDockingData.findAllByFileInList(fileDocks.file)
            def vesselName = dockingDatas.find { it.elementName == "sCTV85_VesselName" }?.set0
            def matrixName = "VDR_" + vesselName + "_" + DateUtilExtensions.format(dateRapport, "ddMMyyyy") + ".xlsx"
            def fileName = FILE_MATRIX_PATH.path + matrixName
            def file = new File(fileName)
            if (file.exists()) {
                log.info("Matrix déjà genere !")
            }
            def matrixTemplate = new File(FILE_MATRIX_TEMPLATE.path + "matrix.xlsx")

            def responseGen = generateMatrix(matrixTemplate, FILE_MATRIX_TEMPLATE.path, fileName, voyages, dateRapport)
            if (!responseGen.serviceOk) {
                log.error(JsonOutput.toJson([methode: "generateMatrix", message: responseGen.message]))
            }
            //String tempDir = csvToolService.getFolderDir() // pour les problèmes de séparateur

            def file1 = new File(fileName)
            if (!file1.exists()) {
                throw new Exception("Une erreur est survenu lors du telechargement !")
            }

            log.info("=============> Fichier matrix genere avec succes")

            // ...determine it's time to shut down
            return

        } catch (Exception e) {
            e.printStackTrace()

        }
    }

    def generateMatrix(File matrixTemplate, def pathTempl, def fileName, int voyageId, def dateRapport) {
        def serviceResponse = new ServiceResponse()
        try {

            def dateFormat = DateUtilExtensions.format(dateRapport, "dd-MM-yyyy")
            def fileVoyage = VDLFile.findByFileNameIlike("%0${voyageId}-${dateFormat}%")

            def vldMasters = VDLMasterData.findAll()
            def vldBases = VDLBaseData.findAll()
            def vldSites = VDLSiteData.findAll()
            def vldCustomers = VDLCustomerData.findAll()


            if (vldBases.isEmpty())
                throw new SaqException("4000", "Aucune Donnée de type VDL_BASE trouvée !")

            if (vldSites.isEmpty())
                throw new SaqException("4000", "Aucune Donnée de type VDL_SITE trouvée !")
            if (vldMasters.isEmpty())
                throw new SaqException("4000", "Aucune Donnée de type VDL_MASTER trouvée !")
            if (vldCustomers.isEmpty())
                throw new SaqException("4000", "Aucune Donnée de type VDL_CUSTOMER trouvée !")

            def matrix = new File(fileName)
            def dockingSheet = new File(pathTempl + "dokings.xlsx")
            def voyageSheet = new File(pathTempl + "voyage_graph.xlsx")
            def profilesSheet = new File(pathTempl + "operational_pofiles.xlsx")
            Files.copy(matrixTemplate, matrix)
            FileInputStream inputStream = new FileInputStream(matrix)
            ZipSecureFile.setMinInflateRatio(0);
            //Creating workbook from input stream
            Workbook workbook = WorkbookFactory.create(inputStream);
            Workbook workbookDokings = WorkbookFactory.create(new FileInputStream(dockingSheet));
            Workbook workbookVoyage = WorkbookFactory.create(new FileInputStream(voyageSheet));
            Workbook workbookEntete = WorkbookFactory.create(new FileInputStream(matrixTemplate));
            Workbook workbookOpeProfiles = WorkbookFactory.create(new FileInputStream(profilesSheet));

            //Reading first sheet of excel file
            Sheet sheet = workbook.getSheetAt(0);
            Sheet sheetDocking = workbookDokings.getSheetAt(0);
            Sheet sheetVoyages = workbookVoyage.getSheetAt(0);
            Sheet sheetOpeProfiles = workbookOpeProfiles.getSheetAt(0);

            //Getting the count of existing records
            // DIviser pour mieux regner
            def voyages = VDLDockingData.findAllByElementNameIlike("%iCTV85_VoyageID%")?.set0?.unique { a, b -> a <=> b }
            def pageIndex = 0
            def vIndex = 1
            println(voyages: voyages.size())
            def context = new GeoApiContext.Builder()
                    .apiKey(Helper.first().googleApiKey)
                    .build()
            voyages.each {
                voyageId = Integer.parseInt(it)
                def fileDocks = VDLDockingData.findAllByElementNameAndSet0("iCTV85_VoyageID", voyageId)
                if (fileDocks.isEmpty())
                    throw new SaqException("4000", "Aucun Docking trouvé pour le voyage $voyageId")

                def dockingDatas = VDLDockingData.findAllByFileInList(fileDocks.file)

                if (dockingDatas.isEmpty())
                    throw new SaqException("4000", "Aucune Donnée Brute trouvée !")

                sheet = fillBord1(workbookEntete, sheet, sheetDocking, sheetVoyages, dateRapport, voyageId, dockingDatas, vldMasters, vldBases, vldSites, vldCustomers, context, pageIndex, vIndex)
                pageIndex = sheet.lastRowNum + 15
                vIndex++
            }
            def firstRowOp = sheet.lastRowNum + 5
            Iterator<Row> iterator = sheetOpeProfiles.iterator();
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                sheet = copyRow(sheet.workbook, sheet, sheetOpeProfiles, currentRow.rowNum, firstRowOp + currentRow.rowNum)

            }
            println(firstRowOp: firstRowOp)
            operationProfile(sheet, firstRowOp + 6, voyages)
            inputStream.close()


            //Crating output stream and writing the updated workbook
            FileOutputStream os = new FileOutputStream(matrix);
            workbook.write(os);

            //Close the workbook and output stream
            workbook.close();
            os.close();

            serviceResponse.serviceOk = true
            serviceResponse.objetInstance = sheet


        }
        catch (Exception e) {
            e.printStackTrace()
            log.error(JsonOutput.toJson([methode: "generateMatrix", message: e.message]))

            serviceResponse.serviceOk = false
            serviceResponse.message = e.message
        }

        return serviceResponse

    }

    def operationProfile(Sheet sheet, def rowNum, def listVoyage) {
        def tofromTransit = 0
        def insideTransit = 0
        def firstTimeDepartureBase = 0
        def firstTimeArrivaleBase = 0
        def firstTimeDepartureSite = 0
        def firstTimeArrivaleSite = 0
        def lastTimeDepartureBase = 0
        def lastTimeArrivaleBase = 0
        def lastTimeDepartureSite = 0
        def lastTimeArrivaleSite = 0
        def startDate = Helper.first().dateRapport
        def endDate = DateUtilExtensions.plus(startDate, 1)
        def i = 0
        def dockingDurantions = 0L

        listVoyage.each { voyageId ->

            BruteData db1 = BruteData.findAllByVoyageIdAndDateTimeTrendBetween(voyageId, startDate, endDate, [sort: 'dateTimeTrend', order: 'asc', limit: 1])[0]

            BruteData db2 = BruteData.findAllByVoyageIdAndDateTimeTrendBetween(voyageId, startDate, endDate, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]

            BruteData ds1 = BruteData.findAllByVoyageIdAndOnSiteIdAndDateTimeTrendBetween(voyageId, 1, startDate, endDate, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]

            BruteData ds2 = BruteData.findAllByVoyageIdAndOnSiteIdAndDateTimeTrendBetween(voyageId, 1, startDate, endDate, [sort: 'dateTimeTrend', order: 'asc', limit: 1])[0]

            def timeDepartureBase = db1?.dateTimeTrend?.time
            def timeArrivaleBase = db2?.dateTimeTrend?.time
            def timeDepartureSite = ds1?.dateTimeTrend?.time
            def timeArrivaleSite = ds2?.dateTimeTrend?.time
            if (!timeDepartureBase || !timeArrivaleBase || !timeDepartureSite || !timeArrivaleSite)
                return sheet

            if (i == 0) {
                firstTimeDepartureBase = timeDepartureBase
                firstTimeArrivaleBase = timeArrivaleBase
                firstTimeDepartureSite = timeDepartureSite
                firstTimeArrivaleSite = timeArrivaleSite
            }
            if (i == listVoyage.size() - 1) {
                lastTimeDepartureBase = timeDepartureBase
                lastTimeArrivaleBase = timeArrivaleBase
                lastTimeDepartureSite = timeDepartureSite
                lastTimeArrivaleSite = timeArrivaleSite
            }
            println(timeArrivaleSite: timeArrivaleSite)
            println(timeDepartureBase: timeDepartureBase)
            println(timeArrivaleBase: timeArrivaleBase)
            println(timeDepartureSite: timeDepartureSite)


            tofromTransit += (timeArrivaleSite - timeDepartureBase + timeArrivaleBase - timeDepartureSite)
            insideTransit += Math.abs(timeArrivaleSite - timeDepartureSite)


            def groupBdata = BruteData.findAllByVoyageIdAndDateTimeTrendBetweenAndDockingIdNotEqual(voyageId, startDate, endDate, 0, [sort: 'dateTimeTrend', order: 'asc'])
                    .groupBy({ b -> b.dockingId }).collect { k, v -> [(k): v] }
            groupBdata.each {
                it.each { k, v ->
                    dockingDurantions += v.last().dateTimeTrend.time - v.first().dateTimeTrend.time
                    println(dockingDurantions: dockingDurantions / 1000)
                }
            }

            i++
        }
        //FROM TO
        def dailyShareFromTo = (tofromTransit / 1000) / 86400
        def difftimeFromToBase = lastTimeArrivaleBase - firstTimeDepartureBase
        def operatingFromToShare = (tofromTransit / difftimeFromToBase)

        sheet.getRow(rowNum + 7).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(tofromTransit), TimeZone.getTimeZone("UTC")))
        sheet.getRow(rowNum + 7).getCell(10).setCellValue(dailyShareFromTo)
        sheet.getRow(rowNum + 7).getCell(13).setCellValue(operatingFromToShare)

        //INSIDE
        def dailyShareInside = (insideTransit / 1000) / 86400
        def operatingInsideShare = (insideTransit) / difftimeFromToBase
        def dateInsideTransit = DateUtilExtensions.setTimeZone(new Date(insideTransit), TimeZone.getTimeZone("UTC"))
        sheet.getRow(rowNum + 8).getCell(7).setCellValue(dateInsideTransit)
        sheet.getRow(rowNum + 8).getCell(10).setCellValue(dailyShareInside)
        sheet.getRow(rowNum + 8).getCell(13).setCellValue(operatingInsideShare)
        // STANDBY
        def sortbDatas = BruteData.findAllByDateTimeTrendBetween(startDate, endDate, [sort: 'dateTimeTrend', order: 'asc'])
        def firstBruteData = BruteData.findAllByDateTimeTrendBetween(startDate, endDate, [sort: 'dateTimeTrend', order: 'asc', limit: 1])[0]
        def lastBruteData = BruteData.findAllByDateTimeTrendBetween(startDate, endDate, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]
        def startTimeDk = firstBruteData.dateTimeTrend
        def endTimeDk = lastBruteData.dateTimeTrend

        def totalDocking = dockingDurantions
        println(totalDocking: totalDocking)

        Long dailyDurationStandBy = (Long) Math.abs(insideTransit - totalDocking)
        def dailyShareStandBy = (dailyDurationStandBy / 1000) / 86400
        def operatingStandBy = (dailyDurationStandBy / difftimeFromToBase)
        sheet.getRow(rowNum + 9).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(dailyDurationStandBy), TimeZone.getTimeZone("UTC")))
        sheet.getRow(rowNum + 9).getCell(10).setCellValue(dailyShareStandBy)
        sheet.getRow(rowNum + 9).getCell(13).setCellValue(operatingStandBy)

        // PUSHING ASSETS
        def dailyShareAsset = (totalDocking / 1000) / 86400
        println(difftimeFromToBase: difftimeFromToBase)
        println(totalDocking: totalDocking)
        def operatingAsset = (totalDocking / difftimeFromToBase)
        sheet.getRow(rowNum + 10).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(totalDocking), TimeZone.getTimeZone("UTC")))
        sheet.getRow(rowNum + 10).getCell(10).setCellValue(dailyShareAsset)
        sheet.getRow(rowNum + 10).getCell(13).setCellValue(operatingAsset)

        def moored = BruteData.findAllByDateTimeTrendBetweenAndVoyageId(startDate, endDate, 0, [sort: 'dateTimeTrend', order: 'asc'])
        def mstartTimeDk = moored.first().dateTimeTrend
        def mendTimeDk = moored.last().dateTimeTrend
        def totalMoored = moored.size() * 1000L
        println(mstartTimeDk: mstartTimeDk)
        println(totalMooredEchantillons: moored.size())
        def dailyShareMoored = (totalMoored / 1000) / 86400
        sheet.getRow(rowNum + 11).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(totalMoored), TimeZone.getTimeZone("UTC")))
        sheet.getRow(rowNum + 11).getCell(10).setCellValue(dailyShareMoored)
        // println(llll: rowNum + 10)


        def firstMePsUseHr = firstBruteData.mePsUseHr
        def lastMePsUseHr = lastBruteData.mePsUseHr
        sheet.getRow(rowNum + 25).getCell(7).setCellValue(lastMePsUseHr - firstMePsUseHr)
        sheet.getRow(rowNum + 25).getCell(11).setCellValue(lastMePsUseHr)

        def firstMeStbUseHr = firstBruteData.meStbUseHr
        def lastMeStbUseHr = lastBruteData.meStbUseHr
        sheet?.getRow(rowNum + 26)?.getCell(7)?.setCellValue(lastMeStbUseHr - firstMeStbUseHr)
        sheet?.getRow(rowNum + 26)?.getCell(11)?.setCellValue(lastMeStbUseHr)

        def sumElecMotorPs = sortbDatas.findAll { it.elecMotorPs > 0 }.size() * 1000

        sheet.getRow(rowNum + 27).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(sumElecMotorPs), TimeZone.getTimeZone("UTC")))

        def sumElecMotorStb = sortbDatas.findAll { it.elecMotorStb > 0 }.size() * 1000
        println(ElecMotorPs: sumElecMotorPs)
        println(sumElecMotorStb: sumElecMotorStb)
        sheet.getRow(rowNum + 28).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(sumElecMotorStb), TimeZone.getTimeZone("UTC")))

//        def firstGePsUseHr = firstBruteData.geRunningHr
//        def lastGePsUseHr = lastBruteData.geRunningHr
//        sheet.getRow(rowNum + 29).getCell(7).setCellValue(lastGePsUseHr - firstGePsUseHr)
//        sheet.getRow(rowNum + 29).getCell(11).setCellValue(lastGePsUseHr)
//
//        def firstGeStbUseHr = firstBruteData.geStbRunningHr
//        def lastGeStbUseHr = lastBruteData.geStbRunningHr
//        sheet.getRow(rowNum + 30).getCell(7).setCellValue(lastGeStbUseHr - firstGeStbUseHr)
//        sheet.getRow(rowNum + 30).getCell(11).setCellValue(lastGeStbUseHr)

        def firstBatteriePsStateHeath = firstBruteData.batteriePsStateHeath
        def lastBatteriePsStateHeath = lastBruteData.batteriePsStateHeath
        sheet?.getRow(rowNum + 29)?.getCell(7)?.setCellValue(lastBatteriePsStateHeath - firstBatteriePsStateHeath)
        sheet?.getRow(rowNum + 29)?.getCell(11)?.setCellValue(lastBatteriePsStateHeath)

        def firstBatterieSbStateHeath = firstBruteData.batterieSbStateHeath
        def lastBatterieSbStateHeath = lastBruteData.batterieSbStateHeath
        sheet?.getRow(rowNum + 30)?.getCell(7)?.setCellValue(lastBatterieSbStateHeath - firstBatterieSbStateHeath)
        sheet?.getRow(rowNum + 30)?.getCell(11)?.setCellValue(lastBatterieSbStateHeath)
        println(firstBruteData.batterieSbStateHeath)
        println(lastBatterieSbStateHeath)

        return sheet
    }

    def fillBord1(Workbook workbookEntete, Sheet sheet, Sheet sheetDocking, Sheet sheetVoyages, def dateRapport, int voyageId, List<VDLDockingData> dockingDatas, List<VDLMasterData> vldMasters, List<VDLBaseData> vldBases, List<VDLSiteData> vldSites, List<VDLCustomerData> vldCustomers, def context, int pageIndex, int vIndex) {

        if (pageIndex != 0) {
            def sheetTmpl = workbookEntete.getSheetAt(0)
            Iterator<Row> iterator = sheetTmpl.iterator();
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                sheet = copyRow(sheet.workbook, sheet, sheetTmpl, currentRow.rowNum, pageIndex + currentRow.rowNum)

            }
        }
        drawMap(voyageId, sheet, pageIndex + 16)
        // PREMIERE SESCTION TABLEAU 1
        Row row1 = sheet.getRow(pageIndex + 4)
        //Vessel name - Docking
        row1.getCell(1).setCellValue(dockingDatas.find { it.elementName == "sCTV85_VesselName" }?.set0)
        // Date

        // VDL_DB_Master.csv.Master Name [VDL_EC00000_00x-DKxxxxx-dd-mm-yyy.csv.iCTV85_IndexMasterName[2]]
        def allIndex = dockingDatas.findAll { it.elementName == "iCTV85_IndexMasterName" }.set0
        println(allIndex: allIndex)
        def index = allIndex[0]
        def masterDocking = vldMasters.find { it.colIndex == Integer.parseInt(index) - 1 }

        row1.getCell(12).setCellValue(masterDocking.masterName)

        Row row2 = sheet.getRow(pageIndex + 8)
        row2.getCell(2).setCellValue(vIndex)
        //Index iCTV85IndexDepartureBase -> VDL_DB_Base.csv
        def iCTV85IndexDepartureBase = dockingDatas.find { it.elementName == "iCTV85IndexDepartureBase" }.set0
        def iCTV85IndexArrivalBase = dockingDatas.find { it.elementName == "iCTV85IndexArrivalBase" }.set0
        def iCTV85IndexSite = dockingDatas.find { it.elementName == "iCTV85IndexSite" }.set0
        def iCTV85_IndexCustomerName = dockingDatas.find { it.elementName == "iCTV85_IndexCustomer" }.set0

        row2.getCell(7).setCellValue(vldBases.find { it.colIndex == Integer.parseInt(iCTV85IndexDepartureBase) - 1 }.baseName)
        row2.getCell(12).setCellValue(vldBases.find { it.colIndex == Integer.parseInt(iCTV85IndexArrivalBase) - 1 }.baseName)


        Row row3 = sheet.getRow(pageIndex + 12)
        // Index iCTV85IndexSite -> VDL_DB_Site.csv
        def indexSiteName = Integer.parseInt(iCTV85IndexSite) - 1
        def indexMasterName = Integer.parseInt(iCTV85_IndexCustomerName) - 1
        println(indexSiteName: indexSiteName)
        println(indexMasterName: indexMasterName)
        row3.getCell(1).setCellValue(vldSites.find { it.tag == 'sCTV85_SiteName' }.("site$indexSiteName"))
        row3.getCell(12).setCellValue(vldCustomers.find { it.colIndex == indexMasterName - 1 }.customerName)


        //Row row4 = sheet.getRow(31)
        //Time departure from base
        BruteData db1 = BruteData.findAllByVoyageId(voyageId, [sort: 'dateTimeTrend', order: 'asc', limit: 1])[0]

        BruteData db2 = BruteData.findAllByVoyageId(voyageId, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]

        BruteData ds1 = BruteData.findAllByVoyageIdAndOnSiteId(voyageId, 1, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]

        BruteData ds2 = BruteData.findAllByVoyageIdAndOnSiteId(voyageId, 1, [sort: 'dateTimeTrend', order: 'asc', limit: 1])[0]

        row1.getCell(7).setCellValue(db1.dateTimeTrend)

        def timeDepartureBase = db1?.dateTimeTrend
        def timeArrivaleBase = db2?.dateTimeTrend
        def timeDepartureSite = ds1?.dateTimeTrend
        def timeArrivaleSite = ds2?.dateTimeTrend
        log.info(JsonOutput.toJson([timeDepartureBase: timeDepartureBase, timeArrivaleBase: timeArrivaleBase,
                                    timeDepartureSite: timeDepartureSite, timeArrivaleSite: timeArrivaleSite]))

        if (!timeDepartureBase || !timeArrivaleBase || !timeDepartureSite || !timeArrivaleSite)
            return sheet

        sheet.getRow(pageIndex + 31).getCell(6).setCellValue(timeDepartureBase)
        sheet.getRow(pageIndex + 31).getCell(14).setCellValue(timeDepartureSite)

        //Row row5 = sheet.getRow(32)
        //Time departure from base
        def pobDeparture = " - "  //.find { it.tag == "iCTV85_POB_Departure" }.set0
        def pobArrival = " - " // .find { it.tag == "iCTV85_POB_Arrival" }.set0
        sheet.getRow(pageIndex + 32).getCell(6).setCellValue(pobDeparture)
        sheet.getRow(pageIndex + 32).getCell(14).setCellValue(timeArrivaleBase)


        //Row row6 = sheet.getRow(33)
        sheet.getRow(pageIndex + 33).getCell(6).setCellValue(timeArrivaleSite)
        sheet.getRow(pageIndex + 33).getCell(14).setCellValue(pobArrival)
        //Row row6 = sheet.getRow(35)

        def sogBase = speedOverGround(timeDepartureBase, timeArrivaleBase, voyageId)

        def inSite1 = getDistance1(timeArrivaleSite, timeDepartureSite, voyageId, "inside the site ")
        def inbound1 = getDistance1(timeDepartureSite, timeArrivaleBase, voyageId, "inbound")
        def outbound1 = getDistance1(timeDepartureBase, timeArrivaleSite, voyageId, "outbound")
        println(outbound1: outbound1)
        println(inbound1: inbound1)
        println(inSite1: inSite1)
        sheet.getRow(pageIndex + 35).getCell(6).setCellValue(outbound1.distance)
        sheet.getRow(pageIndex + 35).getCell(14).setCellValue(inbound1.distance)

        sheet.getRow(pageIndex + 36).getCell(6).setCellValue(outbound1.avgSpeed)
        sheet.getRow(pageIndex + 36).getCell(14).setCellValue(inbound1.avgSpeed)
        //Row row6 = sheet.getRow(36)
        sheet.getRow(pageIndex + 37).getCell(6).setCellValue(outbound1.max)
        sheet.getRow(pageIndex + 37).getCell(14).setCellValue(inbound1.max)

        sheet.getRow(pageIndex + 39).getCell(6).setCellValue(inSite1.distance)
        sheet.getRow(pageIndex + 40).getCell(6).setCellValue(inSite1.avgSpeed)
        sheet.getRow(pageIndex + 41).getCell(6).setCellValue(inSite1.max)

        def tb = dateDiffInSecond(timeDepartureBase, timeArrivaleBase)

        sheet.getRow(pageIndex + 39).getCell(14).setCellValue(sogBase.distance)


        def diffTime = sogBase.timeDiff // dateDiffInSecond(timeDepartureBase, timeArrivaleBase)
        //Row row6 = sheet.getRow(40)
        def totalFluelComsumption = (sogBase.avgPsFluel * diffTime) + (sogBase.avgStbFluel * diffTime)
        def totalBatteryComsumption = (sogBase.avgPsPowerOutput * diffTime) + (sogBase.avgStbPowerOutput * diffTime)
        log.info(JsonOutput.toJson([timeDepartureBase: timeDepartureBase]))
        log.info(JsonOutput.toJson([timeArrivaleBase: timeArrivaleBase]))
        log.info(JsonOutput.toJson([timeDepartureSite: timeDepartureSite]))
        log.info(JsonOutput.toJson([timeArrivaleSite: timeArrivaleSite]))
        log.info(JsonOutput.toJson([totalFluelComsumption: totalFluelComsumption]))
        log.info(JsonOutput.toJson([totalBatteryComsumption: totalBatteryComsumption]))
        log.info(JsonOutput.toJson([diffTime: diffTime]))
        log.info(JsonOutput.toJson([avg: sogBase]))


        sheet.getRow(pageIndex + 40).getCell(14).setCellValue(totalFluelComsumption)
        //sheet.getRow(37).getCell(14).setCellValue(sogSite.avgSpeed)
        sheet.getRow(pageIndex + 42).getCell(14).setCellValue(totalBatteryComsumption)
        //sheet.getRow(37).getCell(14).setCellValue(sogSite.avgSpeed)
        def c = Calendar.getInstance()
        c.setTime(db1.dateTimeTrend)
        c.set(Calendar.HOUR, 00)
        c.set(Calendar.MINUTE, 00)
        c.set(Calendar.SECOND, 00)
        def date = c.time
        def bdt = BruteData.findByDateTimeTrend(date)
        if (!bdt)
            bdt = db1

        def fluelDailyStart = getFuelOnBoard(bdt)

        c.set(Calendar.HOUR, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 00)
        def date0 = c.time
        c.set(Calendar.SECOND, 59)
        def date1 = c.time
        def buteDataa = BruteData.findAllByDateTimeTrendBetween(date0, date1, [sort: 'dateTimeTrend', order: 'desc', limit: 1])[0]

        if (!buteDataa)
            buteDataa = db2

        def fluelDailyEnd = getFuelOnBoard(buteDataa)
        log.info(JsonOutput.toJson([fluelDailyStart: fluelDailyStart]))
        log.info(JsonOutput.toJson([fluelDailyEnd: fluelDailyEnd]))

        def totalFluelStart = fluelDailyStart.levelSsFluel + fluelDailyStart.levelStbFluel
        def totalFluelEnd = fluelDailyEnd.levelSsFluel + fluelDailyEnd.levelStbFluel

        sheet.getRow(pageIndex + 44).getCell(6).setCellValue(totalFluelStart)
        sheet.getRow(pageIndex + 44).getCell(14).setCellValue(totalFluelEnd)

        def fCTV85_FuelIn = dockingDatas.findAll { it.elementName == 'fCTV85_FuelIn' }.set0.sum { new Double(it) }
        def fCTV85_FuelOut = dockingDatas.findAll { it.elementName == 'fCTV85_FuelOut' }.set0.sum { new Double(it) }
        sheet.getRow(pageIndex + 46).getCell(6).setCellValue(fCTV85_FuelIn)
        sheet.getRow(pageIndex + 48).getCell(6).setCellValue(fCTV85_FuelOut)
        println(fCTV85_FuelIn: fCTV85_FuelIn)
        println(fCTV85_FuelOut: fCTV85_FuelOut)
        def totalFluelCons = totalFluelStart - totalFluelEnd
        //sheet.getRow(pageIndex + 48).getCell(14).setCellValue(totalFluelCons)

        //Si (OSS_ID(unsignedInt) <> 0)
        //          VDL_DB_Site.csv.Site Plateform OSS Ref 00(OSS_ID(unsignedInt))[Index iCTV85IndexSite -> VDL_DB_Site.csv]
        def bdatas = BruteData.findAllByVoyageIdAndDockingIdNotEqual(voyageId, 0)
        def groupBdata = bdatas.groupBy({ b -> b.dockingId }).collect { k, v -> [(k): v] }
        def dokingTotalRows = groupBdata.size()
        def indexC = 0
        def firstRowDockinIndex = pageIndex + 104
        def firstDockTime
        def lastDockTime
        def listDuration = []
        def avgTrustList = []
        def maxImpactList = []
        def totalPaxTrans = 0
        def totalLifts = 0
        def totalFluelIn = 0
        def totalFluelOut = 0

        def max = []
        //println(keySet: groupBdata.collect { it.keySet() })
        groupBdata.each {
            it.each { k, v ->

                def dateFormat = DateUtilExtensions.format(v?.first()?.dateTimeTrend, "dd-MM-yyyy")
                println("%0${k}-${dateFormat}%")
                v.first().dateTimeTrend
                def vdlFile = VDLFile.findByFileNameIlike("%0${k}-${dateFormat}%")
                def vdlDK = VDLDockingData.findAllByFile(vdlFile)
                if (!v.isEmpty() && vdlFile && !vdlDK.isEmpty()) {
                    indexC++
                    def ossId = v?.findAll { it.ossId != 0 }?.collect { it?.ossId }?.unique { a, b -> a <=> b }
                    def assetId
                    def siteName
                    if (ossId.isEmpty()) {
                        assetId = v?.findAll { it.assetId != 0 }?.collect { it?.assetId }?.unique { a, b -> a <=> b }
                        siteName = vldSites.findAll { it.elementName == "Site Plateform Ref 00${assetId[0]}" }
                    } else {
                        siteName = vldSites.findAll { it.elementName == "Site Plateform OSS Ref 00${ossId[0]}" }
                    }

                    log.info("startDate => " + v.sort { a, b -> b.dateTimeTrend <=> a.dateTimeTrend }.last().dateTimeTrend)
                    log.info("endDate => " + v.sort { a, b -> b.dateTimeTrend <=> a.dateTimeTrend }.first().dateTimeTrend)
                    log.info("dockingId => " + k)

                    Date startTimeDk = v.sort { a, b -> b.dateTimeTrend <=> a.dateTimeTrend }.last().dateTimeTrend
                    Date endTimeDk = v.sort { a, b -> b.dateTimeTrend <=> a.dateTimeTrend }.first().dateTimeTrend
                    //def vdlDK = VDLDockingData.findAll("from VDLDockingData f where f.element_name='iCTV85_DockingID' and  set0=:id ", [id: k.toLong()])
                    if (!firstDockTime)
                        firstDockTime = startTimeDk

                    lastDockTime = endTimeDk


                    sheet.getRow(firstRowDockinIndex + indexC).getCell(0).setCellValue("#" + indexC)
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(2).setCellValue(startTimeDk)
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(3).setCellValue(endTimeDk)
                    def durationDiff = endTimeDk.time - startTimeDk.time
                    listDuration << durationDiff


                    sheet.getRow(firstRowDockinIndex + indexC).getCell(4).setCellValue(DateUtilExtensions.setTimeZone(new Date(durationDiff), TimeZone.getTimeZone("UTC")))
                    //def sensorData = sensorData(v)
                    def maxFenderPush = v.fenderPushForce.max()
                    def sumFenderPush = v.sum { it.fenderPushForce }
                    def avgFenderPush = sumFenderPush / v.fenderPushForce.size()

                    sheet.getRow(firstRowDockinIndex + indexC).getCell(6).setCellValue(maxFenderPush)
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(7).setCellValue(avgFenderPush)

                    if (maxFenderPush)
                        maxImpactList << maxFenderPush

                    if (avgFenderPush)
                        avgTrustList << avgFenderPush

                    // Pax OUT #1

                    def fCTV85_PaxIn = vdlDK.find { it.elementName == "fCTV85_PaxIn" }?.set0
                    def fCTV85_PaxOut = vdlDK.find { it.elementName == "fCTV85_PaxOut" }?.set0
                    def fCTV85_LiftsOut = vdlDK.find { it.elementName == "fCTV85_LiftsOut" }?.set0
                    def fCTV85_LiftsIn = vdlDK.find { it.elementName == "fCTV85_LiftsIn" }?.set0
                    def fCTV85_FuelOut1 = vdlDK.find { it.elementName == "fCTV85_FuelOut" }?.set0
                    def fCTV85_FuelIn1 = vdlDK.find { it.elementName == "fCTV85_FuelIn" }?.set0
                    def comments = vdlDK.find { it.elementName == 'sCTV85_Comments' }.set0
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(8).setCellValue(new Double(fCTV85_PaxOut)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(9).setCellValue(new Double(fCTV85_PaxIn)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(10).setCellValue(new Double(fCTV85_LiftsOut)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(11).setCellValue(new Double(fCTV85_LiftsIn)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(12).setCellValue(new Double(fCTV85_FuelOut1)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(13).setCellValue(new Double(fCTV85_FuelIn1)?.intValue())
                    sheet.getRow(firstRowDockinIndex + indexC).getCell(14).setCellValue(comments)
                    if (fCTV85_PaxIn && fCTV85_PaxOut) {
                        totalPaxTrans += new BigDecimal(fCTV85_PaxIn) + new BigDecimal(fCTV85_PaxOut)
                    }
                    if (fCTV85_LiftsIn && fCTV85_LiftsOut) {
                        totalLifts += new BigDecimal(fCTV85_LiftsOut) + new BigDecimal(fCTV85_LiftsIn)
                    }
                    if (fCTV85_FuelOut1) {
                        totalFluelOut += new BigDecimal(fCTV85_FuelOut1)
                    }
                    if (fCTV85_FuelIn1) {
                        totalFluelIn += new BigDecimal(fCTV85_FuelIn1)
                    }
                }
            }
        }
        // Distance sailed outbound
        //Valeur calculée : addition de distances par échantillon entre "Time departure from base" et "Time arrival on site"

        //        sheet.getRow(48).getCell(14).setCellValue(totalFluelCons)
        sheet.getRow(pageIndex + 126).getCell(2).setCellValue(groupBdata.size())
        // dockings duration
        Long durationDkT = listDuration.sum() as Long
        sheet.getRow(pageIndex + 126).getCell(7).setCellValue(DateUtilExtensions.setTimeZone(new Date(durationDkT), TimeZone.getTimeZone("UTC")))

        def avgDuration = 0L
        if (!listDuration.isEmpty())
            avgDuration = (durationDkT / listDuration.size()).toLong()

        sheet.getRow(pageIndex + 126).getCell(12).setCellValue(DateUtilExtensions.setTimeZone(new Date(avgDuration), TimeZone.getTimeZone("UTC")))
        sheet.getRow(pageIndex + 129).getCell(2).setCellValue(totalPaxTrans.intValue())
        sheet.getRow(pageIndex + 129).getCell(6).setCellValue(totalLifts.intValue())
        sheet.getRow(pageIndex + 129).getCell(9).setCellValue(totalFluelOut.intValue())
        sheet.getRow(pageIndex + 129).getCell(13).setCellValue(totalFluelIn.intValue())


        def avgTrust = 0
        if (avgTrustList) {
            avgTrust = avgTrustList.sum() / avgTrustList.size()
        }
        def maxImpact = 0
        if (avgTrustList) {
            maxImpact = maxImpactList.sum() / maxImpactList.size()
        }

        sheet.getRow(pageIndex + 132).getCell(5).setCellValue(maxImpact)
        sheet.getRow(pageIndex + 132).getCell(9).setCellValue(avgTrust)
        def indexDocking = 0
        def lastRow = setDokingDataAndCharts(groupBdata, sheet, sheetDocking, dateRapport, vldSites, voyageId, vIndex, pageIndex + 136)
        def listBrutedata = BruteData.findAllByVoyageId(voyageId)
        setVoyageDataAndCharts(listBrutedata, timeDepartureBase, timeArrivaleSite, timeDepartureSite, timeArrivaleBase, sheet, sheetVoyages, dateRapport, vldSites, voyageId, vIndex, sheet.lastRowNum + 12)
        return sheet


    }

    def setVoyageDataAndCharts(List<BruteData> listBruteData, def timeDepartureFromBase, def timeArrivalOnSite, def timeDepartureFromSite, def timeArrivalAtBase, Sheet sheet, Sheet sheetTmpl, def dateRapport, def vldSites, def voyageId, vIndex, int lastRow) {
        def itr = 0
        Iterator<Row> iterator = sheetTmpl.iterator();
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            sheet = copyRow(sheet.workbook, sheet, sheetTmpl, currentRow.rowNum, lastRow + currentRow.rowNum + (itr > 0 ? 86 : 0))

        }
        println(timeDepartureFromBase: timeDepartureFromBase)
        println(timeArrivalOnSite: timeArrivalOnSite)
        def listBruteData1 = listBruteData.findAll { it.dateTimeTrend.seconds in [0, 30, 45] && it.dateTimeTrend.before(timeArrivalOnSite) && it.dateTimeTrend.after(timeDepartureFromBase) }
        def listSpeedOverGround = listBruteData1.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.speedOverGround?.doubleValue()] }
        sheet.getRow(lastRow).getCell(8).setCellValue(vIndex)
        sheet.getRow(lastRow + 36).getCell(8).setCellValue(vIndex)
        sheet.getRow(lastRow + 72).getCell(8).setCellValue(vIndex)

        // OUTBOUND GRAPH
        println(listFenderPush: listSpeedOverGround.size())        //collect {}
        if (listSpeedOverGround.empty) {
            listSpeedOverGround = [[x: DateUtilExtensions.format(new Date(), "dd/MM/yyyy HH:mm:ss"), y: 0]]
        }
        Double maxSpeed = listSpeedOverGround.y.max() * 1.2
        def strData = JsonOutput.toJson(listSpeedOverGround)
        println(strData: strData)
        def dataChart = "{\n" +
                "  type: 'line',\n" +
                "  data: {\n" +
                "    datasets: [\n" +
                "      {\n" +
                "   fill: false,\n" +
                "   lineTension: 0,\n" +
                "   borderWidth: 1,\n" +
                "   borderColor: getGradientFillHelper('vertical', ['#f00509' , '#ebe836', '#36eb3f']), \n" +
                "    gradient: {\n" +
                "        borderColor: {\n" +
                "          axis: 'y',\n" +
                "          colors: {\n" +
                "            20: '#f00509',\n" +
                "            14: '#ebe836',\n" +
                "            0: '#36eb3f'\n" +
                "          }\n" +
                "       }\n" +
                "    },\n" +
                "    data: " + strData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  options: {\n" +
                "    chartArea: {\n" +
                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                "    },\n" +
                "      elements: {\n" +
                "          point:{\n" +
                "              radius: 0\n" +
                "          },\n" +
                "       line: {\n" +
                "            tension: 0.4\n" +
                "        }\n" +
                "      },\n" +
                "    legend: {\n" +
                "            display: false\n" +
                "    },\n" +
                "    scales: {\n" +
                "      xAxes: [{\n" +
                "        type: 'time',\n" +
                "        time: {\n" +
                "            parser: 'DD/MM/YYYY HH:mm:ss',\n" +
                "            displayFormats: {\n" +
                "            minute: 'HH:mm' " +
                "            },\n" +
                "         },\n" +
                "        ticks: {\n" +
                "            fontSize: 8,\n" +
                "            fontColor : 'rgb(0,0,0)', \n" +
                "            autoSkip: true ,\n" +
                "              maxTicksLimit: 20 \n" +
                "         },\n" +
                "       unitStepSize: 100\n" +
                "      }],\n" +
                "      yAxes: [{\n" +
                "        ticks: {\n" +
                "          fontSize: 8,\n" +
                "           min: 0,\n" +
                "           max: " + maxSpeed + ",\n" +
                "          fontColor : 'rgb(0,0,0)', " +
                "          beginAtZero: false" +
                "        }," +
                "        gridLines: {\n" +
                "                display: true\n" +
                "            }\n" +
                "      }]\n" +
                "    }\n" +
                "  }\n" +
                "}"

        println(dataChart)

        QuickChart chart = new QuickChart();
        chart.setWidth(800);
        chart.setHeight(150);
        chart.setConfig(dataChart)
        def tmpdir = csvToolService.getTempDir()
        def chartFileName = tmpdir + "radiant_sog.png"
        chart.toFile(chartFileName);
        InputStream inputStream1 = new FileInputStream(chartFileName)
        def bytes = IOUtils.toByteArray(inputStream1)
        int pictureIdx = sheet.workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
        //close the input stream
        inputStream1.close();

        //Returns an object that handles instantiating concrete classes
        CreationHelper helper = sheet.workbook.getCreationHelper();

        //Creates the top-level drawing patriarch.
        Drawing drawing = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchor = helper.createClientAnchor();
        //set top-left corner for the image
        anchor.setCol1(0);
        anchor.setRow1(lastRow + 5);

        //Creates a picture
        Picture pict = drawing.createPicture(anchor, pictureIdx);
        pict.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pict.resize();


        buildChartMultiAxis(listBruteData1, sheet, lastRow + 12)
        buildChartMultiAxis1(listBruteData1, sheet, lastRow + 21)


        // INSITE GRAPH
        def mod1 = [0, 15, 30, 45]
        def mod2 = [0, 30, 45]
        def listBruteData2 = listBruteData.findAll { it.dateTimeTrend.seconds in mod1 && it.dateTimeTrend.after(timeArrivalOnSite) && it.dateTimeTrend.before(timeDepartureFromSite) }
        if (listBruteData2.size() > 1800) {
            listBruteData2 = listBruteData2.findAll { it.dateTimeTrend.seconds in mod2 }
        }
        def listSpeedOverGround1 = listBruteData2.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.speedOverGround?.doubleValue()] }
        println(GRAPH: "INSITE GRAPH")       //collect {}
        println(listFenderPush: listSpeedOverGround1.size())        //collect {}
        println(timeArrivalOnSite: timeArrivalOnSite)        //collect {}
        println(timeDepartureFromSite: timeDepartureFromSite)        //collect {}
        if (listSpeedOverGround1.empty) {
            listSpeedOverGround1 = [[x: DateUtilExtensions.format(new Date(), "dd/MM/yyyy HH:mm:ss"), y: 0]]
        }

        Double maxSpeed1 = listSpeedOverGround1.y.max() * 1.2
        def strData1 = JsonOutput.toJson(listSpeedOverGround1)
        def dataChart1 = "{\n" +
                "  type: 'line',\n" +
                "  data: {\n" +
                "    datasets: [\n" +
                "      {\n" +
                "   fill: false,\n" +
                "   lineTension: 0,\n" +
                "   borderWidth: 1,\n" +
                "   borderColor: getGradientFillHelper('vertical', ['#f00509' , '#ebe836', '#36eb3f']), \n" +
                "    gradient: {\n" +
                "        borderColor: {\n" +
                "          axis: 'y',\n" +
                "          colors: {\n" +
                "            20: '#f00509',\n" +
                "            14: '#ebe836',\n" +
                "            0: '#36eb3f'\n" +
                "          }\n" +
                "       }\n" +
                "    },\n" +
                "    data: " + strData1.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  options: {\n" +
                "    chartArea: {\n" +
                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                "    },\n" +
                "      elements: {\n" +
                "          point:{\n" +
                "              radius: 0\n" +
                "          },\n" +
                "       line: {\n" +
                "            tension: 0.4\n" +
                "        }\n" +
                "      },\n" +
                "    legend: {\n" +
                "            display: false\n" +
                "    },\n" +
                "    scales: {\n" +
                "      xAxes: [{\n" +
                "        type: 'time',\n" +
                "        time: {\n" +
                "            parser: 'DD/MM/YYYY HH:mm:ss',\n" +
                "            displayFormats: {\n" +
                "            minute: 'HH:mm'" +
                "            },\n" +
                "         },\n" +
                "        ticks: {\n" +
                "            fontSize: 8,\n" +
                "            fontColor : 'rgb(0,0,0)', \n" +
                "            autoSkip: true ,\n" +
                "              maxTicksLimit: 20 \n" +
                "         },\n" +
                "       unitStepSize: 100\n" +
                "      }],\n" +
                "      yAxes: [{\n" +
                "        ticks: {\n" +
                "          fontSize: 8,\n" +
                "           min: 0,\n" +
                "           max: " + maxSpeed1 + ",\n" +
                "          fontColor : 'rgb(0,0,0)', " +
                "          beginAtZero: false" +
                "        }," +
                "        gridLines: {\n" +
                "                display: true\n" +
                "            }\n" +
                "      }]\n" +
                "    }\n" +
                "  }\n" +
                "}"

        println(dataChart1)

        QuickChart chart1 = new QuickChart();
        chart1.setWidth(800);
        chart1.setHeight(150);
        chart1.setConfig(dataChart1)
        def chartFileName1 = tmpdir + "radiant1_sog.png"
        chart1.toFile(chartFileName1);
        InputStream inputStream2 = new FileInputStream(chartFileName1)
        def bytes1 = IOUtils.toByteArray(inputStream2)
        int picture1Idx = sheet.workbook.addPicture(bytes1, Workbook.PICTURE_TYPE_PNG);
        //close the input stream
        inputStream2.close();

        //Returns an object that handles instantiating concrete classes
        CreationHelper helper1 = sheet.workbook.getCreationHelper();

        //Creates the top-level drawing patriarch.
        Drawing drawing1 = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchor1 = helper1.createClientAnchor();
        //set top-left corner for the image
        anchor1.setCol1(0);
        anchor1.setRow1(lastRow + 39);

        //Creates a picture
        Picture pict1 = drawing1.createPicture(anchor1, picture1Idx);
        pict1.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pict1.resize();

        buildChartMultiAxis(listBruteData2, sheet, lastRow + 50)
        buildChartMultiAxis1(listBruteData2, sheet, lastRow + 60)


        // INBOUND GRAPH
        def listBruteData3 = listBruteData.findAll { it.dateTimeTrend.seconds in [0, 15, 30, 45] && it.dateTimeTrend.after(timeArrivalAtBase) && it.dateTimeTrend.before(timeDepartureFromSite) }
        def listSpeedOverGround2 = listBruteData3.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.speedOverGround?.doubleValue()] }
        println(timeArrivalAtBase3: timeArrivalAtBase)
        println(timeArrivalAtBase3: timeDepartureFromSite)
        if (listSpeedOverGround2.empty) {
            listSpeedOverGround2 = [[x: DateUtilExtensions.format(timeArrivalAtBase, "dd/MM/yyyy HH:mm:ss"), y: 0]]
            println("listSpeedOverGround2 vide !")
        }

        Double maxSpeed2 = listSpeedOverGround2.y.max() * 1.2
        def strData2 = JsonOutput.toJson(listSpeedOverGround2)
        def dataChart2 = "{\n" +
                "  type: 'line',\n" +
                "  data: {\n" +
                "    datasets: [\n" +
                "      {\n" +
                "   fill: false,\n" +
                "   lineTension: 0,\n" +
                "   borderWidth: 1,\n" +
                "   borderColor: getGradientFillHelper('vertical', ['#f00509' , '#ebe836', '#36eb3f']), \n" +
                "    gradient: {\n" +
                "        borderColor: {\n" +
                "          axis: 'y',\n" +
                "          colors: {\n" +
                "            20: '#f00509',\n" +
                "            14: '#ebe836',\n" +
                "            0: '#36eb3f'\n" +
                "          }\n" +
                "       }\n" +
                "    },\n" +
                "    data: " + strData2.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  options: {\n" +
                "    chartArea: {\n" +
                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                "    },\n" +
                "      elements: {\n" +
                "          point:{\n" +
                "              radius: 0\n" +
                "          },\n" +
                "       line: {\n" +
                "            tension: 0.4\n" +
                "        }\n" +
                "      },\n" +
                "    legend: {\n" +
                "            display: false\n" +
                "    },\n" +
                "    scales: {\n" +
                "      xAxes: [{\n" +
                "        type: 'time',\n" +
                "        time: {\n" +
                "            parser: 'DD/MM/YYYY HH:mm:ss',\n" +
                "            displayFormats: {\n" +
                "            minute: 'HH:mm'" +
                "            },\n" +
                "         },\n" +
                "        ticks: {\n" +
                "            fontSize: 8,\n" +
                "            fontColor : 'rgb(0,0,0)', \n" +
                "            autoSkip: true ,\n" +
                "              maxTicksLimit: 10 \n" +
                "         },\n" +
                "       unitStepSize: 900\n" +
                "      }],\n" +
                "      yAxes: [{\n" +
                "        ticks: {\n" +
                "          fontSize: 8,\n" +
                "           min: 0,\n" +
                "           max: " + maxSpeed2 + ",\n" +
                "          fontColor : 'rgb(0,0,0)', " +
                "          beginAtZero: false" +
                "        }," +
                "        gridLines: {\n" +
                "                display: true\n" +
                "            }\n" +
                "      }]\n" +
                "    }\n" +
                "  }\n" +
                "}"

        println(dataChart2)

        QuickChart chart2 = new QuickChart();
        chart2.setWidth(800);
        chart2.setHeight(150);
        chart2.setConfig(dataChart2)
        def chartFileName2 = tmpdir + "radiant2_sog.png"
        chart2.toFile(chartFileName2);
        InputStream inputStream3 = new FileInputStream(chartFileName2)
        def bytes2 = IOUtils.toByteArray(inputStream3)
        int picture2Idx = sheet.workbook.addPicture(bytes2, Workbook.PICTURE_TYPE_PNG);
        //close the input stream
        inputStream3.close();

        //Returns an object that handles instantiating concrete classes
        CreationHelper helper2 = sheet.workbook.getCreationHelper();

        //Creates the top-level drawing patriarch.
        Drawing drawing2 = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchor2 = helper2.createClientAnchor();
        //set top-left corner for the image
        anchor2.setCol1(0);
        anchor2.setRow1(lastRow + 78);


        //Creates a picture
        Picture pict2 = drawing2.createPicture(anchor2, picture2Idx);
        pict2.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pict2.resize();

        buildChartMultiAxis(listBruteData3, sheet, lastRow + 90)
        buildChartMultiAxis1(listBruteData3, sheet, lastRow + 102)
        return lastRow + 115

    }

    def setDokingDataAndCharts(def groupBdata, Sheet sheet, Sheet sheetTmpl, def dateRapport, def vldSites, def voyageId, def vIndex, int lastRow) {
        def itr = 0
        def page = 86


        groupBdata.each {

            Iterator<Row> iterator = sheetTmpl.iterator();
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                sheet = copyRow(sheet.workbook, sheet, sheetTmpl, currentRow.rowNum, lastRow + currentRow.rowNum + (itr > 0 ? 86 : 0))
            }

            lastRow = lastRow + 8 + (itr > 0 ? 86 : 0)
            sheet?.getRow(lastRow)?.getCell(8)?.setCellValue(vIndex)

            it.each { k, v ->
                sheet?.getRow(lastRow - 8)?.getCell(13)?.setCellValue(itr + 1)
                if (!v.isEmpty()) {
                    def dokings = v.sort { a, b -> a.dateTimeTrend <=> b.dateTimeTrend }
                    Date startTimeDk = dokings.first().dateTimeTrend
                    Date endTimeDk = dokings.last().dateTimeTrend
                    // row
                    def latitude = VDLSiteData.findByElementName("Site Plateform Latitude00${itr + 1}")?.site0
                    def longitude = VDLSiteData.findByElementName("Site Plateform Longitude00${itr + 1}")?.site0
                    if (latitude && longitude) {
                        latitude = new Float(latitude)
                        longitude = new Float(longitude)
//                        def degreesLg = Math.floor(longitude).toInteger()
//                        def degreesLt = Math.floor(latitude).toInteger()
//                        def minutesLg = Math.floor(60.0 * (longitude - degreesLg)).toInteger()
//                        def minutesLt = Math.floor(60.0 * (latitude - degreesLt)).toInteger()
//                        def secondsLt = 3600.0 * (latitude - degreesLt) - (60.0 * minutesLt)
//                        def secondsLg = 3600.0 * (longitude - degreesLg) - (60.0 * minutesLg)
//                        def strLatitude = "$degreesLt°$minutesLt'$secondsLt\""
//                        def strLongitude = "$degreesLg°$minutesLg'$secondsLg\""
                        String converted0 = decimalToDMS(latitude)
                        final String dmsLat = latitude > 0 ? ORIENTATIONS[0] : ORIENTATIONS[1];
                        def strLatitude = converted0.concat(" ").concat(dmsLat);
                        String converted1 = decimalToDMS(longitude)
                        final String dmsLng = longitude > 0 ? ORIENTATIONS[2] : ORIENTATIONS[3];
                        def strLongitude = converted1.concat(" ").concat(dmsLng);
                        sheet?.getRow(lastRow)?.getCell(0)?.setCellValue(strLatitude)
                        sheet?.getRow(lastRow + 1)?.getCell(0)?.setCellValue(strLongitude)
                    }
                    sheet?.getRow(lastRow)?.getCell(3)?.setCellValue(startTimeDk)
                    sheet?.getRow(lastRow)?.getCell(4)?.setCellValue(endTimeDk)
                    sheet?.getRow(lastRow)?.getCell(5)?.setCellValue(DateUtilExtensions.setTimeZone(new Date(endTimeDk.time - startTimeDk.time), TimeZone.getTimeZone("UTC")))


                    def dateFormat = DateUtilExtensions.format(v?.first()?.dateTimeTrend, "dd-MM-yyyy")
                    println("%0${k}-${dateFormat}%")
                    def vdlFile = VDLFile.findByFileNameIlike("%0${k}-${dateFormat}%")
                    def vdlDK = VDLDockingData.findAllByFile(vdlFile)
                    if (vdlFile && !vdlDK.isEmpty()) {

                        // def sensorData = sensorData(v)
                        def maxPushFenderDocking = v.fenderPushForce.max()
                        def sumFp = v.fenderPushForce.sum()

                        def avgPushFenderDocking = sumFp / v.size()
                        println(avgPushFenderDocking: avgPushFenderDocking)

                        sheet?.getRow(lastRow)?.getCell(7)?.setCellValue(maxPushFenderDocking)
                        sheet?.getRow(lastRow)?.getCell(8)?.setCellValue(avgPushFenderDocking)
                        // Pax OUT #1
                        def fCTV85_PaxIn = vdlDK.find { it.elementName == "fCTV85_PaxIn" }?.set0
                        def fCTV85_PaxOut = vdlDK.find { it.elementName == "fCTV85_PaxOut" }?.set0
                        def fCTV85_LiftsOut = vdlDK.find { it.elementName == "fCTV85_LiftsOut" }?.set0
                        def fCTV85_LiftsIn = vdlDK.find { it.elementName == "fCTV85_LiftsIn" }?.set0
                        def fCTV85_FuelOut1 = vdlDK.find { it.elementName == "fCTV85_FuelOut" }?.set0
                        def fCTV85_FuelIn1 = vdlDK.find { it.elementName == "fCTV85_FuelIn" }?.set0
                        def comments = vdlDK.find { it.elementName == 'sCTV85_Comments' }.set0
                        sheet?.getRow(lastRow)?.getCell(9)?.setCellValue(new Double(fCTV85_PaxOut).intValue())
                        sheet?.getRow(lastRow)?.getCell(10)?.setCellValue(new Double(fCTV85_PaxIn).intValue())
                        sheet?.getRow(lastRow)?.getCell(11)?.setCellValue(new Double(fCTV85_LiftsOut).intValue())
                        sheet?.getRow(lastRow)?.getCell(12)?.setCellValue(new Double(fCTV85_LiftsIn).intValue())
                        sheet?.getRow(lastRow)?.getCell(13)?.setCellValue(new Double(fCTV85_FuelOut1).intValue())
                        sheet?.getRow(lastRow)?.getCell(14)?.setCellValue(new Double(fCTV85_FuelIn1).intValue())
                        sheet?.getRow(lastRow)?.getCell(15)?.setCellValue(comments)

                        Calendar cStart = Calendar.getInstance()
                        cStart.setTime(endTimeDk)
                        cStart.set(Calendar.MINUTE, 0)
                        cStart.set(Calendar.SECOND, 0)
                        cStart.set(Calendar.MILLISECOND, 0)
                        def fDate = DateUtilExtensions.format(cStart.time, "yyyy-MM-dd'T'HH")
                        def vdlWeather = Forecast.findByDateTimeLike("%${fDate}%")
                        if (!vdlWeather) {
                            def fDate1 = DateUtilExtensions.format(cStart.time, "yyyy-MM-dd")
                            vdlWeather = Forecast.findByDateTimeLike("%${fDate1}%")
                        }
                        if (vdlWeather) {
                            sheet?.getRow(lastRow + 8)?.getCell(0)?.setCellValue(vdlWeather.totalWaveRiskHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(2)?.setCellValue(vdlWeather.totalWaveHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(4)?.setCellValue(vdlWeather.totalWaveMeanDirection)
                            sheet?.getRow(lastRow + 8)?.getCell(6)?.setCellValue(vdlWeather.totalWavePeakPeriod)
                            sheet?.getRow(lastRow + 8)?.getCell(8)?.setCellValue(vdlWeather.swellHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(9)?.setCellValue(vdlWeather.swellHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(10)?.setCellValue(vdlWeather.swellDirection)
                            sheet?.getRow(lastRow + 8)?.getCell(11)?.setCellValue(vdlWeather.swellPeakPeriod)
                            sheet?.getRow(lastRow + 8)?.getCell(12)?.setCellValue(vdlWeather.windWaveHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(13)?.setCellValue(vdlWeather.windWaveHeight)
                            sheet?.getRow(lastRow + 8)?.getCell(14)?.setCellValue(vdlWeather.windDirection10)
                            sheet?.getRow(lastRow + 8)?.getCell(15)?.setCellValue(vdlWeather.windWavePeakPeriod)


                            def boardLanding = VDLSiteData.findByElementNameIlike("%Site Plateform Heading00${itr + 1}%")?.site0
                            sheet?.getRow(lastRow + 11)?.getCell(0)?.setCellValue(boardLanding)
                            sheet?.getRow(lastRow + 11)?.getCell(2)?.setCellValue(vdlWeather.windSpeed10)
                            sheet?.getRow(lastRow + 11)?.getCell(4)?.setCellValue(vdlWeather.windDirection10)
                            sheet?.getRow(lastRow + 11)?.getCell(6)?.setCellValue(vdlWeather.windSpeed50)
                            sheet?.getRow(lastRow + 11)?.getCell(8)?.setCellValue(vdlWeather.windDirection10)
                            sheet?.getRow(lastRow + 11)?.getCell(10)?.setCellValue(vdlWeather.surfaceCurrentSpeed)
                            sheet?.getRow(lastRow + 11)?.getCell(12)?.setCellValue(vdlWeather.surfaceCurrentDirection)


                        }


                        def prevDock
                        def groupStable = [:]
                        def i = 0
                        def iu = 0
                        def docks = []
                        def currentDock
                        dokings.each {
                            if (it.dockingStable == 1) {
                                if (prevDock == 1) {
                                    docks << it
                                    if (dokings.last() == it) {
                                        iu++
                                        groupStable.("T" + iu) = docks
                                    }
                                } else if (prevDock == 0) {
                                    i++
                                    groupStable.("U" + i) = docks
                                    docks = [it]
                                }
                            } else if (it.dockingStable == 0) {
                                if (prevDock == 0 || prevDock == null) {
                                    docks << it
                                    if (dokings.last() == it) {
                                        i++
                                        groupStable.("U" + i) = docks
                                    }
                                } else if (prevDock == 1) {
                                    iu++
                                    groupStable.("T" + iu) = docks

                                    docks = [it]
                                }
                            } else {
                                println("POURQUOI ESCE NULL !!!! ")
                            }
                            prevDock = it.dockingStable
                        }

                        if (i == 0 || iu == 0) {
                            if (docks[0].dockingStable == 1) {
                                groupStable["T1"] = docks
                            } else {
                                groupStable["U1"] = docks
                            }
                        }
                        def indexDocking1 = 1
                        def totalStatbleInter
                        def stableIntervale = []
                        println(indexDockingRow: lastRow + 18 + indexDocking1)
                        def totalDureeDockingStable = 0
                        groupStable.each { k1, v1 ->
                            def firstDokingStable
                            Long dureeDockingStable
                            def lastDokingStable
                            def bowheave
                            firstDokingStable = v1?.first()
                            lastDokingStable = v1?.last()
                            dureeDockingStable = lastDokingStable.dateTimeTrend.time - firstDokingStable.dateTimeTrend.time
                            bowheave = bowHeaveData(v1)

                            if (k1.contains('T')) {

                                stableIntervale << [debut: firstDokingStable.dateTimeTrend, fin: lastDokingStable.dateTimeTrend, stable: true, sizeDdata: v1.size(), bowheave: bowheave]
                                totalDureeDockingStable += dureeDockingStable
                                sheet?.getRow(lastRow + 17 + indexDocking1)?.getCell(2)?.setCellValue(DateUtilExtensions.setTimeZone(new Date(dureeDockingStable), TimeZone.getTimeZone("UTC")))
                                sheet?.getRow(lastRow + 17 + indexDocking1)?.getCell(4)?.setCellValue(bowheave.max)
                                sheet?.getRow(lastRow + 17 + indexDocking1)?.getCell(6)?.setCellValue(bowheave.avg)


                            } else if (k1.contains('U')) {

                                stableIntervale << [debut: firstDokingStable.dateTimeTrend, fin: lastDokingStable.dateTimeTrend, stable: false, sizeDdata: v1.size()]
                                sheet?.getRow(lastRow + 17 + indexDocking1)?.getCell(8)?.setCellValue(bowheave.max)

                            }
                            println(firstDokingStable: firstDokingStable.dateTimeTrend)
                            println(lastDokingStable: lastDokingStable.dateTimeTrend)
                            sheet?.getRow(lastRow + 17 + indexDocking1)?.getCell(0)?.setCellValue(k1)
                            indexDocking1++

                        }
                        println(stableIntervale: stableIntervale)

                        def formulaUpdate1 = sheet?.getRow(lastRow + 15)?.getCell(13)?.getCellFormula()
                        def formulaUpdate2 = sheet?.getRow(lastRow + 15)?.getCell(16)?.getCellFormula()
                        def formulaUpdate3 = sheet?.getRow(lastRow + 20)?.getCell(13)?.getCellFormula()
                        def formulaUpdate4 = sheet?.getRow(lastRow + 20)?.getCell(16)?.getCellFormula()
                        def formulaUpdate5 = sheet?.getRow(lastRow + 25)?.getCell(13)?.getCellFormula()
                        def formulaUpdate6 = sheet?.getRow(lastRow + 25)?.getCell(16)?.getCellFormula()
                        def replacedFormula1 = formulaUpdate1.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        def replacedFormula2 = formulaUpdate2.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        def replacedFormula3 = formulaUpdate3.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        def replacedFormula4 = formulaUpdate4.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        def replacedFormula5 = formulaUpdate5.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        def replacedFormula6 = formulaUpdate6.replaceAll("27", "${lastRow + 18}").replaceAll("36", "${lastRow + 22}")
                        println(formulaUpdate1: replacedFormula1)
                        println(formulaUpdate1: formulaUpdate1)
                        sheet?.getRow(lastRow + 15)?.getCell(13)?.setCellFormula(replacedFormula1)
                        sheet?.getRow(lastRow + 15)?.getCell(16)?.setCellFormula(replacedFormula2)

                        sheet?.getRow(lastRow + 20)?.getCell(13)?.setCellValue(totalDureeDockingStable)
                        sheet?.getRow(lastRow + 20)?.getCell(16)?.setCellFormula(replacedFormula4)
                        sheet?.getRow(lastRow + 25)?.getCell(13)?.setCellFormula(replacedFormula5)
                        sheet?.getRow(lastRow + 25)?.getCell(16)?.setCellFormula(replacedFormula6)
                        //Max slip criterion Z safe (meters)
                        def maxSlipCriterion = vdlDK.find { it.elementName == "fCTV85_MaxVerticalSlip" }?.set0
                        def minTimeWindow = vdlDK.find { it.elementName == "fCTV85_MinTimeWindow" }?.set0
                        def maxPushForce = vdlDK.find { it.elementName == "fCTV85_MaxPushForce" }?.set0
                        sheet?.getRow(lastRow + 33)?.getCell(0)?.setCellValue(new Double(maxSlipCriterion).toInteger())
                        sheet?.getRow(lastRow + 33)?.getCell(3)?.setCellValue(new Double(minTimeWindow).toInteger())


                        //  def listTrendG = v.groupBy {DateUtilExtensions.format(it.dateTimeTrend, "dd/MM/yyyy HH:mm")}
                        //def listFenderPush = []
//                        listTrendG.each {k2,v2->
//                            listFenderPush <<  [x: DateUtilExtensions.format(v2?.first()?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss") , y:  v2?.fenderPushForce?.max() ]
//                        }
//                        LocalDateTime ldt = LocalDateTime.ofInstant(v[0]?.dateTimeTrend?.toInstant(), ZoneId.systemDefault());
//                        LocalDateTime ldt1 = LocalDateTime.ofInstant(v?.last()?.dateTimeTrend?.toInstant(), ZoneId.systemDefault());
//                        def ldtBefore = ldt.minusSeconds(30)
//                        def ldtAfter = ldt1.plusSeconds(30)
//                        Date dateBefore = Date.from(ldtBefore.atZone(ZoneId.systemDefault()).toInstant());
//                        Date dateAfter = Date.from(ldtAfter.atZone(ZoneId.systemDefault()).toInstant());
                        v = v.sort { a, b -> a.dateTimeTrend <=> b.dateTimeTrend }
                        def dateBefore = DateUtils.addSeconds(startTimeDk, -30)
                        def dateAfter = DateUtils.addSeconds(endTimeDk, 30)
                        println(dateBefore: dateBefore)
                        println(dateAfter: dateAfter)
                        def firstTimeLFenderPush = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.fenderPushForce?.doubleValue()]
                        def lastTimeLFenderPush = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.fenderPushForce?.doubleValue()]
                        def listFenderPush = v.findAll { it.dateTimeTrend.seconds % 5 == 0 || it.dateTimeTrend.seconds == 3 || it.dateTimeTrend.seconds == 8 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.fenderPushForce?.doubleValue()] }
                        listFenderPush.add(0, firstTimeLFenderPush)
                        listFenderPush.add(lastTimeLFenderPush)
                        println(listFenderPush: listFenderPush.size())        //collect {}
                        def strData = JsonOutput.toJson(listFenderPush)
                        println(strData: strData)
                        def colorGdt = "{}"
                        def colorGdtMap = [:]

                        stableIntervale.each {
                            if (it.stable) {
                                colorGdtMap["${DateUtilExtensions.format(it.debut, "dd/MM/yyyy HH:mm:ss")}"] = "#36eb3f"
                                colorGdtMap["${DateUtilExtensions.format(it.fin, "dd/MM/yyyy HH:mm:ss")}"] = "#36eb3f"
                            } else {
                                colorGdtMap["${DateUtilExtensions.format(it.debut, "dd/MM/yyyy HH:mm:ss")}"] = "#f00509"
                                colorGdtMap["${DateUtilExtensions.format(it.fin, "dd/MM/yyyy HH:mm:ss")}"] = "#f00509"
                            }

                        }
                        if (colorGdtMap)
                            colorGdt = JsonOutput.toJson(colorGdtMap)

                        def colorStr = colorGdtMap.values().collect { "'${it}'" }.join(",")


                        println(colorGdt: colorGdt)
                        println(colorStr: colorStr)

                        def dataChart = "{" +
                                "  type: 'line'," +
                                "  data: {" +
                                "    datasets: [" +
                                "      {" +
                                "   fill: false," +
                                "   lineTension: 0," +
                                "   borderWidth: 0.5," +
                                "   borderColor: getGradientFillHelper('horizontal', [$colorStr]), \n" +
                                "    gradient: {\n" +
                                "        borderColor: {\n" +
                                "          axis: 'x',\n" +
                                "          colors: $colorGdt" +
                                "       }\n" +
                                "    },\n" +
//                                "   borderJoinStyle: 'miter'," +
                                "        data: " + strData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }" +
                                "    ]" +
                                "  }," +
                                "  options: {" +
                                "    chartArea: {" +
                                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                                "    }," +
                                "      elements: {" +
                                "          point:{" +
                                "              radius: 0" +
                                "          }," +
                                "       line: {" +
                                "            tension: 0" +
                                "        }" +
                                "      }," +
                                "    legend: {" +
                                "            display: false" +
                                "    }," +
                                "    scales: {" +
                                "      xAxes: [{" +
                                "        type: 'time'," +
                                "        time: {" +
                                "            parser: 'DD/MM/YYYY HH:mm:ss'," +
                                "            displayFormats: {" +
                                "            minute: 'HH:mm'" +
                                "            }," +
                                "         }," +
                                "        ticks: {" +
                                "            fontSize: 8," +
                                "            fontColor : 'rgb(0,0,0)', " +
                                "            autoSkip: true ," +
                                "            tickLength: 10, " +
                                "            maxTicksLimit: 30 " +
                                "         }," +
                                "      }]," +
                                "      yAxes: [{" +
                                "        ticks: {" +
                                "          fontSize: 8," +
                                "          fontColor : 'rgb(0,0,0)', " +
                                "          maxTicksLimit: 20, " +
                                "          beginAtZero: true" +
                                "        }," +
                                "        gridLines: {\n" +
                                "                display: true" +
                                "            }" +
                                "      }]" +
                                "    }" +
                                "  }" +
                                "}"

                        QuickChart chart = new QuickChart();
                        chart.setWidth(800);
                        chart.setHeight(150);
                        chart.setConfig(dataChart)
                        def tmpdir = csvToolService.getTempDir()
                        def chartFileName = tmpdir + "fender_${k}.png"
                        chart.toFile(chartFileName);
                        InputStream inputStream1 = new FileInputStream(chartFileName)
                        def bytes = IOUtils.toByteArray(inputStream1)
                        int pictureIdx = sheet.workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                        //close the input stream
                        inputStream1.close();

                        //Returns an object that handles instantiating concrete classes
                        CreationHelper helper = sheet.workbook.getCreationHelper();

                        //Creates the top-level drawing patriarch.
                        Drawing drawing = sheet.createDrawingPatriarch();

                        //Create an anchor that is attached to the worksheet
                        ClientAnchor anchor = helper.createClientAnchor();
                        //set top-left corner for the image
                        anchor.setCol1(0);
                        anchor.setRow1(lastRow + 35);

                        //Creates a picture
                        Picture pict = drawing.createPicture(anchor, pictureIdx);
                        pict.setFillColor(255, 255, 255)
                        //Reset the image to the original size
                        pict.resize();

                        def maxRool = v.rool.max()
                        def sumRoll = v.rool.sum() ?: 0
                        def avgRool = sumRoll / (v.rool.size() ?: 1)

                        def maxYawdevi = v.dockingHeadingDeviation.max()
                        def sumYawdevi = v.dockingHeadingDeviation.sum() ?: 0
                        def totalYaw = v.dockingHeadingDeviation.size() != 0 ? v.dockingHeadingDeviation.size() : 1
                        def avgYawdevi = sumYawdevi / totalYaw


                        sheet?.getRow(lastRow + 44)?.getCell(8)?.setCellValue(vIndex)
                        sheet?.getRow(lastRow + 51)?.getCell(0)?.setCellValue(maxRool)
                        sheet?.getRow(lastRow + 51)?.getCell(4)?.setCellValue(avgRool)
                        sheet?.getRow(lastRow + 51)?.getCell(8)?.setCellValue(maxYawdevi)
                        sheet?.getRow(lastRow + 51)?.getCell(12)?.setCellValue(avgYawdevi)


                        def listDataTrend = v.findAll { it.dateTimeTrend.seconds in [0, 5, 10, 20, 30, 40, 50] }
                        def maxRoll = listDataTrend.rool.max() + (listDataTrend.rool.max() * 0.15)
                        def maxYaw = listDataTrend.dockingHeadingDeviation.max() + (listDataTrend.dockingHeadingDeviation.max() * 0.15)
                        maxRoll = Math.max(maxRoll, maxYaw)
                        def minRoll = listDataTrend.rool.min() * -0.15
                        def roolList = listDataTrend.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.rool?.doubleValue()] }
                        def yawList = listDataTrend.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.dockingHeadingDeviation?.doubleValue() - avgYawdevi] }

                        def firstTimeRoolList = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.rool?.doubleValue()]
                        def lastTimeRoolList = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.rool?.doubleValue()]
                        def firstTimeYaw = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.dockingHeadingDeviation?.doubleValue() - avgYawdevi]
                        def lastTimeYaw = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.dockingHeadingDeviation?.doubleValue() - avgYawdevi]
                        roolList.add(0, firstTimeRoolList)
                        roolList.add(lastTimeRoolList)
                        yawList.add(0, firstTimeYaw)
                        yawList.add(lastTimeYaw)
                        def strRollData = JsonOutput.toJson(roolList)
                        def strYawData = JsonOutput.toJson(yawList)
                        def dataRollYawChart = "{" +
                                "  type: 'line'," +
                                " backgroundColor: 'rgb(255,255,255)'," +
                                " data: {" +
                                "    datasets: [" +
                                "      {" +
                                "       fill: false," +
                                "       label: 'Roll angle'," +
                                "       lineTension: 0.4," +
                                "       borderWidth: 0.5," +
                                "       borderColor: 'rgb(0,0,255)'," +
                                "       data: " + strRollData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }," +
                                "      {" +
                                "       fill: false," +
                                "       label: 'Yaw déviation'," +
                                "       lineTension: 0.4," +
                                "       borderWidth: 0.5," +
                                "       borderColor: 'rgb(255,255,0)'," +
                                "       data: " + strYawData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }" +
                                "    ]" +
                                "  }," +
                                "  options: {" +
                                "     chartArea: {" +
                                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                                "      }," +
                                "      elements: {" +
                                "          point:{" +
                                "              radius: 0" +
                                "          }," +
                                "       line: {" +
                                "            tension: 0.4" +
                                "        }" +
                                "      }," +
                                "    legend: {" +
                                "            display: true" +
                                "    }," +
                                "    scales: {" +
                                "      xAxes: [{" +
                                "        type: 'time'," +
                                "        time: {" +
                                "            parser: 'DD/MM/YYYY HH:mm:ss'," +
                                "            displayFormats: {" +
                                "            minute: 'HH:mm'" +
                                "            }," +
                                "         }," +
                                "        ticks: {" +
                                "            fontSize: 8," +
                                "            fontColor : 'rgb(0,0,0)', " +
                                "            autoSkip: true ," +
                                "          tickLength: 10, " +
                                "              maxTicksLimit: 30 " +
                                "         }" +
                                "      }]," +
                                "      yAxes: [{" +
                                "        ticks: {" +
                                "          fontSize: 8," +
                                "           min: " + 0 + "," +
                                "           max: " + maxRoll + "," +
                                "          fontColor : 'rgb(0,0,0)' " +
                                "        }," +
                                "        gridLines: {\n" +
                                "                display: true" +
                                "            }" +
                                "      }]" +
                                "    }" +
                                "  }" +
                                "}"

                        QuickChart chartRoll = new QuickChart();
                        chartRoll.setWidth(800);
                        chartRoll.setHeight(150);
                        chartRoll.setConfig(dataRollYawChart)
                        def chartRollFileName1 = tmpdir + "roll_${k}.png"
                        chartRoll.toFile(chartRollFileName1);
                        InputStream inputStream2 = new FileInputStream(chartRollFileName1)
                        def bytesRoll = IOUtils.toByteArray(inputStream2)
                        int pictureRollIdx = sheet.workbook.addPicture(bytesRoll, Workbook.PICTURE_TYPE_JPEG);
                        //close the input stream
                        inputStream2.close();

                        Drawing drawingRoll = sheet.createDrawingPatriarch();

                        //Create an anchor that is attached to the worksheet
                        ClientAnchor anchorRoll = helper.createClientAnchor();
                        anchorRoll.setCol1(0);
                        anchorRoll.setRow1(lastRow + 53);

                        //Creates a picture
                        Picture pictRoll = drawingRoll.createPicture(anchorRoll, pictureRollIdx);
                        pictRoll.setFillColor(255, 255, 255)
                        //Reset the image to the original size
                        pictRoll.resize();
                        sheet?.getRow(lastRow + 67)?.getCell(3)?.setCellValue(new Double(maxPushForce).toInteger())

                        def listDataTrend1 = v.findAll { it.dateTimeTrend.seconds in [0, 20, 30, 40, 50] }

                        def maxFenderForce = listDataTrend.fenderPushForce.max()
                        println(maxFenderForce: maxFenderForce)
                        maxFenderForce = maxFenderForce + (maxFenderForce * 0.15)
                        println(maxFenderForce1: maxFenderForce)
                        def firstCppSbPitch = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.cppSbPitch?.doubleValue()]
                        def lastCppSbPitch = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.cppSbPitch?.doubleValue()]
                        def firstCppPSPitch = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.cppPsPitch?.doubleValue()]
                        def lastCppPSPitch = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.cppPsPitch?.doubleValue()]
                        def firstFenderPushForce = [x: DateUtilExtensions.format(dateBefore, "dd/MM/yyyy HH:mm:ss"), y: v[0]?.cppPsPitch?.doubleValue()]
                        def lastFenderPushForce = [x: DateUtilExtensions.format(dateAfter, "dd/MM/yyyy HH:mm:ss"), y: v?.last()?.cppPsPitch?.doubleValue()]

                        def cppSbPitchList = listDataTrend1.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.cppSbPitch?.doubleValue()] }
                        cppSbPitchList.add(0, firstCppSbPitch)
                        cppSbPitchList.add(lastCppSbPitch)
                        def cppPsPitchList = listDataTrend1.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.cppPsPitch?.doubleValue()] }
                        cppPsPitchList.add(0, firstCppPSPitch)
                        cppPsPitchList.add(lastCppPSPitch)
                        def fenderPushList = listDataTrend1.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.fenderPushForce?.doubleValue()] }
                        fenderPushList.add(0, firstFenderPushForce)
                        fenderPushList.add(lastFenderPushForce)

                        def strCppSbPitchData = JsonOutput.toJson(cppSbPitchList)
                        def strCppPsPitch = JsonOutput.toJson(cppPsPitchList)
                        def strFenderPush = JsonOutput.toJson(fenderPushList)

                        def dataCppPitchChart = "{" +
                                "  type: 'line'," +
                                " backgroundColor: 'rgb(255,255,255)'," +
                                " data: {" +
                                "    datasets: [" +
                                "      {" +
                                "       fill: false," +
                                "       label: 'CPP PS PITCH'," +
                                "       lineTension: 0.4," +
                                "       borderWidth: 0.5," +
                                "       borderColor: 'rgb(0,0,255)'," +
                                "       yAxisID: 'y'," +
                                "       data: " + strCppPsPitch.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }," +
                                "      {" +
                                "       fill: false," +
                                "       label: 'CPP SB PITCH'," +
                                "       lineTension: 0.4," +
                                "       borderWidth: 0.5," +
                                "       borderColor: 'rgb(255,0,0)'," +
                                "       yAxisID: 'y'," +
                                "       data: " + strCppSbPitchData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }," +
                                "      {" +
                                "       fill: false," +
                                "       label: 'PUSH FORCE SENSORS'," +
                                "       lineTension: 0.4," +
                                "       borderWidth: 0.5," +
                                "       borderColor: 'rgb(7, 166, 15)'," +
                                "       yAxisID: 'y1'," +
                                "       data: " + strFenderPush.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                                "      }" +
                                "    ]" +
                                "  }," +
                                "  options: {" +
                                "    chartArea: {" +
                                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                                "    }," +
                                "      elements: {" +
                                "          point:{" +
                                "              radius: 0" +
                                "          }," +
                                "       line: {" +
                                "            tension: 0.4" +
                                "        }" +
                                "      }," +
                                "    legend: {" +
                                "            display: true" +
                                "    }," +
                                "    scales: {" +
                                "      xAxes: [{" +
                                "        type: 'time'," +
                                "        time: {" +
                                "            parser: 'DD/MM/YYYY HH:mm:ss'," +
                                "            displayFormats: {" +
                                "            minute: 'HH:mm'" +
                                "            }," +
                                "         }," +
                                "        ticks: {" +
                                "            fontSize: 8," +
                                "            fontColor : 'rgb(0,0,0)', " +
                                "            autoSkip: true ," +
                                "              maxTicksLimit: 30, " +
                                "                 tickThickness: 10, " +
                                "              stepSize : 3 " +
                                "         }" +
                                "      }]," +
                                "      yAxes: [{" +
                                "       id: 'y', " +
                                "       type: 'linear'," +
                                "       display: true," +
                                "       position: 'left'," +
                                "        ticks: {" +
                                "          min: 0," +
                                "          max:  120," +
                                "          fontColor : 'rgb(0,0,0)', " +
                                "          fontSize: 8," +
                                "          callback: function(value, index, ticks) { \n" +
                                "                        return value + ' %';\n" +
                                "                    }" +
                                "        }," +
                                "        gridLines: {\n" +
                                "                display: true" +
                                "            }" +
                                "        }," +
                                "        {" +
                                "           id: 'y1', " +
                                "           type: 'linear'," +
                                "           display: true," +
                                "           position: 'right'," +
                                "           ticks: {" +
                                "               fontSize: 8," +
                                "               fontColor : 'rgb(0,0,0)', " +
                                "                min: 0," +
                                "                max: 120," +
                                "                fontSize: 8," +
                                "          callback: function(value, index, ticks) { \n" +
                                "                        return value + ' kN';\n" +
                                "                    }" +
                                "            }," +
                                "           gridLines: {\n" +
                                "                display: true" +
                                "            }" +
                                "      }]" +
                                "    }" +
                                "  }" +
                                "}"

                        QuickChart chartCppPitch = new QuickChart();
                        chartCppPitch.setWidth(800);
                        chartCppPitch.setHeight(200);
                        chartCppPitch.setConfig(dataCppPitchChart)
                        def chartCppPitchFileName = tmpdir + "cppPitch_${k}.png"
                        chartCppPitch.toFile(chartCppPitchFileName);
                        InputStream inputStream3 = new FileInputStream(chartCppPitchFileName)
                        def bytesCpp = IOUtils.toByteArray(inputStream3)
                        int pictureCppIdx = sheet.workbook.addPicture(bytesCpp, Workbook.PICTURE_TYPE_JPEG);
                        //close the input stream
                        inputStream3.close();

                        Drawing drawingCpp = sheet.createDrawingPatriarch();

                        //Create an anchor that is attached to the worksheet
                        ClientAnchor anchorCpp = helper.createClientAnchor();
                        anchorCpp.setCol1(0);
                        anchorCpp.setRow1(lastRow + 69);

                        //Creates a picture
                        Picture pictCpp = drawingCpp.createPicture(anchorCpp, pictureCppIdx);
                        pictCpp.setFillColor(255, 255, 255)
                        //Reset the image to the original size
                        pictCpp.resize();


                    }

                }

            }
            itr++

        }
        return lastRow + 69

    }

    def speedOverGround(Date dateDepart, Date dateArrive, def voyageId) {
        try {
            def jsonSlurper = new JsonSlurper()
            def trends = BruteData.findAllByDateTimeTrendBetweenAndVoyageId(dateDepart, dateArrive, voyageId)
            def speedList = [speed: [], psFluel: [], stbFluel: [], avgPsPowerOutput: [], avgStbPowerOutput: []]
            println(trendsListSize: trends.size())
            trends.each {
                def object = jsonSlurper.parseText(it.aiValues)
                if (object) {
                    //return Lazy Map
                    object.each {
                        it = (Map) it
                        it.keySet().each { i ->
                            if (i == TrendsDictionary.SPEED_OVER_GROUND && it.get(i) != null) {
                                speedList.speed << new BigDecimal(it.get(i))
                            }
                            if (i == TrendsDictionary.ME_PS_FUEL_CONSUMPTION_RATE && it.get(i) != null) {
                                speedList.psFluel << new BigDecimal(it.get(i))
                            }
                            if (i == TrendsDictionary.ME_STB_FUEL_CONSUMPTION_RATE && it.get(i) != null) {
                                speedList.stbFluel << new BigDecimal(it.get(i))
                            }
                            if (i == TrendsDictionary.BATTERY_PS_POWER_OUTPUT && it.get(i) != null) {
                                speedList.avgPsPowerOutput << new BigDecimal(it.get(i))
                            }
                            if (i == TrendsDictionary.BATTERY_STB_POWER_OUTPUT && it.get(i) != null) {
                                speedList.avgStbPowerOutput << new BigDecimal(it.get(i))
                            }
                        }

                    }

                }

            }
            if (!speedList.isEmpty()) {

                def speedAverages = 0.0
                def psFluel = 0.0
                def stbFluel = 0.0
                def psPowerOutput = 0.0
                def stbPowerOutput = 0.0

                if (!speedList?.speed?.empty)
                    speedAverages = speedList?.speed?.stream()?.mapToDouble({ s -> s?.toDouble() })
                            ?.average()
                            ?.getAsDouble()
                if (!speedList?.psFluel?.empty)
                    psFluel = speedList?.psFluel?.stream()?.mapToDouble({ s -> s?.toDouble() })
                            ?.average()
                            ?.getAsDouble()

                if (!speedList?.stbFluel?.empty)
                    stbFluel = speedList?.stbFluel?.stream()?.mapToDouble({ s -> s?.toDouble() })
                            ?.average()
                            ?.getAsDouble()
                if (!speedList?.avgPsPowerOutput?.empty)
                    psPowerOutput = speedList?.avgPsPowerOutput?.stream()?.mapToDouble({ s -> s?.toDouble() })
                            ?.average()
                            ?.getAsDouble()
                if (!speedList?.avgStbPowerOutput?.empty)
                    stbPowerOutput = speedList.avgStbPowerOutput.stream().mapToDouble({ s -> s.toDouble() })
                            .average()
                            .getAsDouble()

                def timeDiff = (dateArrive.time - dateDepart.time) / 3_600_000
                def distance = speedAverages * timeDiff

                // max, avgSpeed , avgPsFluel, avgStbFluel, avgPsPowerOutput, avgStbPowerOutput
                return [max              : speedList.speed.max(),
                        avgSpeed         : speedAverages,
                        avgPsFluel       : psFluel,
                        avgStbFluel      : stbFluel,
                        avgPsPowerOutput : psPowerOutput,
                        avgStbPowerOutput: stbPowerOutput,
                        timeDiff         : timeDiff,
                        distance         : distance
                ]
            }

        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "speedOverGround", message: ex.message]))
        }
        return [max     : 0,
                avgSpeed: 0]

    }

    def getDistance1(Date dateDepart, Date dateArrive, Integer voyageId, String type) {
        try {
            def trends = BruteData.findAllByDateTimeTrendBetweenAndVoyageId(dateDepart, dateArrive, voyageId)
            def timeDuration = (dateArrive.time - dateDepart.time) / 3_600_000

            //get max speedoverground
            def maxb = trends.max { it.speedOverGround }
            def sizeTrends = trends.size() == 0 ? 1 : trends.size()
            // get avg speedoverground
            def sumb = trends.sum { it.speedOverGround } ?: 0
            def speedAverages = sumb / sizeTrends
            println("-----------Verification $type------------")
            println(dateDepart: dateDepart)
            println(dateArrive: dateArrive)
            println(timeDuration: timeDuration)
            println(sumb: sumb)
            println(sizeTrends: trends.size())
            println("-----------------------")
            // distance == moyen vitesse / temps parcouru
            def distance = speedAverages * timeDuration


            return [max         : maxb?.speedOverGround ?: 0,
                    avgSpeed    : speedAverages,
                    timeDuration: timeDuration,
                    distance    : distance]


        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(JsonOutput.toJson([methode: "speedOverGround", message: ex.message]))
        }

        return [max: 0, avgSpeed: 0, distance: 0]

    }

    def sensorData(List<BruteData> trends) {
        try {
            def jsonSlurper = new JsonSlurper()
            def sensorDataList = [fenderPushForce: []]
            def fenderPushForce = []
            trends.each {
                def object = jsonSlurper.parseText(it.aiValues)
                if (object) {
                    //return Lazy Map
                    object.each {
                        it = (Map) it
                        it.keySet().each { i ->
                            if (i == TrendsDictionary.FENDERS_PUSH_FORCE_SENSORS && it.get(i) != null) {
                                fenderPushForce << new BigDecimal(it.get(i))
                            }

                        }

                    }
                }

            }
            if (!fenderPushForce.isEmpty()) {

                def speedAverages = fenderPushForce.stream().mapToDouble({ s -> s.toDouble() })
                        .average()
                        .getAsDouble()

                return [max: fenderPushForce.max(),
                        avg: speedAverages
                ]
            }

        } catch (Exception ex) {
            ex.printStackTrace()

            log.error(JsonOutput.toJson([methode: "fenderPushForce", message: ex.message]))
        }
        return [max: 0, avg: 0]

    }

    def fenderPushForceList(List<BruteData> trends) {
        try {
            def jsonSlurper = new JsonSlurper()
            def fenderPushForce = []
            trends.each { br ->
                def object = jsonSlurper.parseText(br.aiValues)
                if (object) {
                    //return Lazy Map
                    object.each {
                        it = (Map) it
                        it.keySet().each { i ->
                            if (i == TrendsDictionary.FENDERS_PUSH_FORCE_SENSORS && it.get(i) != null) {
                                fenderPushForce << [date: br.dateTimeTrend, fender: new BigDecimal(it.get(i))]
                            }

                        }

                    }
                }

            }
            return fenderPushForce

        } catch (Exception ex) {
            ex.printStackTrace()

            log.error(JsonOutput.toJson([methode: "fenderPushForce", message: ex.message]))
        }
        return []

    }

    def bowHeaveData(List<BruteData> trends) {
        try {
            println(nbrSconds: trends.size())
            def jsonSlurper = new JsonSlurper()
            def bowHeave = []
            trends.each {
                def object = jsonSlurper.parseText(it.aiValues)
                if (object) {
                    //return Lazy Map
                    object.each {
                        it = (Map) it
                        it.keySet().each { i ->
                            if (i == TrendsDictionary.BOW_HEAVE && it.get(i) != null) {
                                bowHeave << new BigDecimal(it.get(i))
                            }

                        }

                    }
                }

            }
            if (!bowHeave.isEmpty()) {
                println("====DateDebut => ${trends.first().dateTimeTrend}")
                println("====DateFin =>  ${trends.last().dateTimeTrend}")

                println(totalBowHeave: bowHeave.size())
                println(sumBowHeave: bowHeave.sum())
                println(maxBowHeave: bowHeave.max())
                def speedAverages = bowHeave.sum() / bowHeave.size()
                println(avgBowHeave: speedAverages)

                return [max: bowHeave.max(),
                        avg: speedAverages
                ]
            }

        } catch (Exception ex) {
            ex.printStackTrace()

            log.error(JsonOutput.toJson([methode: "bowHeave", message: ex.message]))
        }
        return [max: 0, avg: 0]

    }

    def getFuelOnBoard(BruteData data) {
        try {
            def jsonSlurper = new JsonSlurper()
            def speedList = [levelSsFluel: 0, levelStbFluel: 0]
            def object = jsonSlurper.parseText(data.aiValues)
            if (object) {
                //return Lazy Map
                object.each {
                    it = (Map) it
                    it.keySet().each { i ->
                        if (i == TrendsDictionary.TANK_LEVEL_PS_DAILY_FUEL_OIL && it.get(i) != null) {
                            speedList.levelSsFluel = new BigDecimal(it.get(i))
                        }
                        if (i == TrendsDictionary.TANK_LEVEL_STB_DAILY_FUEL_OIL && it.get(i) != null) {
                            speedList.levelStbFluel = new BigDecimal(it.get(i))
                        }
                    }

                }
                return speedList
            }


        } catch (Exception ex) {
            ex.printStackTrace()

            log.error(JsonOutput.toJson([methode: "speedOverGround", message: ex.message]))
        }
        return [max: 0, avgSpeed: 0]

    }

    def dateDiffInSecond(Date dateDepart, Date dateArrival) {
        return (dateArrival.time - dateDepart.time) / 1000
    }

    public static boolean isRowEmpty(Row row) {


        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    def copyRow(Workbook workbook, Sheet worksheet, Sheet worksheettmpl, int sourceRowNum, int destinationRowNum) {
        // Get the source / new row
        Row newRow = worksheet.getRow(destinationRowNum);
        Row sourceRow = worksheettmpl.getRow(sourceRowNum);
        // If the row exist in destination, push down all rows by 1 else create a new row
        newRow = worksheet.createRow(destinationRowNum);


        copyAnyMergedRegions(worksheet, worksheettmpl, sourceRow, newRow)
        // Loop through source columns to add to new row
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            // Grab a copy of the old/new cell
            Cell oldCell = sourceRow.getCell(i);
            Cell newCell = newRow.createCell(i);
            // If the old cell is null jump to next cell
            if (oldCell == null) {
                newCell = null;
                continue;
            }
            // Copy style from old cell and apply to new cell
            CellStyle newCellStyle = workbook.createCellStyle();
            newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
            ;
            newCell.setCellStyle(newCellStyle);
            // If there is a cell comment, copy
            if (oldCell.getCellComment() != null) {
                newCell.setCellComment(oldCell.getCellComment());
            }
            // If there is a cell hyperlink, copy
            if (oldCell.getHyperlink() != null) {
                newCell.setHyperlink(oldCell.getHyperlink());
            }
            // Set the cell data value
            switch (oldCell.getCellType()) {
                case CellType.BLANK:
                    newCell.setCellValue(oldCell.getStringCellValue());
                    break;
                case CellType.BOOLEAN:
                    newCell.setCellValue(oldCell.getBooleanCellValue());
                    break;
                case CellType.ERROR:
                    newCell.setCellErrorValue(oldCell.getErrorCellValue());
                    break;
                case CellType.FORMULA:
                    def formula = oldCell.getCellFormula()
                    newCell.setCellFormula(formula);
                    break;
                case CellType.NUMERIC:
                    newCell.setCellValue(oldCell.getNumericCellValue());
                    break;
                case CellType.STRING:
                    newCell.setCellValue(oldCell.getRichStringCellValue());
                    break;
                default:
                    println(getCellType: oldCell.getCellType())
                    break
            }
        }
        return worksheet
    }

    private static void copyAnyMergedRegions(Sheet worksheet, Sheet oldWorkshett, Row sourceRow, Row newRow) {
        for (int i = 0; i < oldWorkshett.getNumMergedRegions(); i++)
            copyMergeRegion(worksheet, sourceRow, newRow, oldWorkshett.getMergedRegion(i));
    }

    private static void copyMergeRegion(Sheet worksheet, Row sourceRow, Row newRow, CellRangeAddress mergedRegion) {
        try {
            CellRangeAddress range = mergedRegion;
            if (range.getFirstRow() == sourceRow.getRowNum()) {
                int lastRow = newRow.getRowNum() + (range.getLastRow() - range.getFirstRow());
                CellRangeAddress newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(), lastRow, range.getFirstColumn(), range.getLastColumn());
                worksheet.addMergedRegionUnsafe(newCellRangeAddress);
            }
        } catch (Exception ex) {
            println(ex.message)
        }
    }


    def buildChartMultiAxis(List<BruteData> listDataTrend1, Sheet sheet, def lastRow) {
        log.info(JsonOutput.toJson([methode: "buildChartMultiAxis", listDataTrend1: listDataTrend1.size(), lastRow: lastRow]))


        def cppSbPitchList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.cppSbPitch?.doubleValue()] }
        def cppPsPitchList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.cppPsPitch?.doubleValue()] }
        def mePsSpeedList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.mePsSpeedRpm?.doubleValue()] }
        def meSbSpeedList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.meStbSpeedRpm?.doubleValue()] }
        def elecMotorPsPowerList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.elecMotorPs?.doubleValue()] }
        def elecMotorSbPowerList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.elecMotorStb?.doubleValue()] }
        def strCppSbPitchData = JsonOutput.toJson(cppSbPitchList)
        def strCppPsPitch = JsonOutput.toJson(cppPsPitchList)
        def strMePsSpeed = JsonOutput.toJson(mePsSpeedList)
        def strMeSbSpeed = JsonOutput.toJson(meSbSpeedList)
        def strelecMotorPsPower = JsonOutput.toJson(elecMotorPsPowerList)
        def strelecMotorSbPower = JsonOutput.toJson(elecMotorSbPowerList)


        def dataMultiAxisChart = "{" +
                "  type: 'line'," +
                " backgroundColor: 'rgb(255,255,255)'," +
                " data: {" +
                "    datasets: [" +
                "      {" +
                "       fill: false," +
                "       label: 'ME PS SPEED RPM'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderDash: [5] ," +
                "       borderColor: '#6d071a'," +
                "       yAxisID: 'y'," +
                "       data: " + strMePsSpeed.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ME PSB SPEED RPM'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderDash: [5] ," +
                "       borderColor: '#d473d4'," +
                "       yAxisID: 'y'," +
                "       data: " + strMeSbSpeed.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'CPP PS PITCH'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#6d071a'," +
                "       yAxisID: 'y'," +
                "       data: " + strCppPsPitch.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'CPP SB PITCH'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#d473d4'," +
                "       yAxisID: 'y'," +
                "       data: " + strCppSbPitchData.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ELEC MOTOR PS POWER OUTPUT'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#fdff00'," +
                "       yAxisID: 'y1'," +
                "       data: " + strelecMotorPsPower.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ELEC MOTOR SB POWER OUTPUT'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#ff730b'," +
                "       yAxisID: 'y1'," +
                "       data: " + strelecMotorSbPower.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }" +
                "    ]" +
                "  }," +
                "  options: {" +
                "    chartArea: {" +
                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                "    }," +
                "      elements: {" +
                "          point:{" +
                "              radius: 0" +
                "          }," +
                "       line: {" +
                "            tension: 0.4" +
                "        }" +
                "      }," +
                "    legend: {" +
                "            display: true" +
                "    }," +
                "    scales: {" +
                "      xAxes: [{" +
                "        type: 'time'," +
                "        time: {" +
                "            parser: 'DD/MM/YYYY HH:mm:ss'," +
                "            displayFormats: {" +
                "            minute: 'HH:mm'" +
                "            }," +
                "         }," +
                "        ticks: {" +
                "            fontSize: 8," +
                "            fontColor : 'rgb(0,0,0)', " +
                "            autoSkip: true ," +
                "                 tickThickness: 10, " +
                "              maxTicksLimit: 30 " +
                "         }" +
                "      }]," +
                "      yAxes: [{" +
                "       id: 'y', " +
                "       type: 'linear'," +
                "       display: true," +
                "       position: 'left'," +
                "        ticks: {" +

                "          fontColor : 'rgb(0,0,0)', " +
                "          fontSize: 8" +
                "        }," +
                "        gridLines: {\n" +
                "                display: true" +
                "            }" +
                "        }," +
                "        {" +
                "           id: 'y1', " +
                "           type: 'linear'," +
                "           display: true," +
                "           position: 'right'," +
                "           ticks: {" +
                "               fontSize: 8," +
                "               fontColor : 'rgb(0,0,0)', " +
                "                fontSize: 8" +
                "            }," +
                "           gridLines: {\n" +
                "                display: true" +
                "            }" +
                "      }]" +
                "    }" +
                "  }" +
                "}"

        QuickChart chartCppPitch = new QuickChart();
        chartCppPitch.setWidth(800);
        chartCppPitch.setHeight(150);
        chartCppPitch.setConfig(dataMultiAxisChart)
        def chartCppPitchFileName = csvToolService.getTempDir() + "multiAxis.png"
        chartCppPitch.toFile(chartCppPitchFileName);
        InputStream inputStream3 = new FileInputStream(chartCppPitchFileName)
        def bytesCpp = IOUtils.toByteArray(inputStream3)
        int pictureCppIdx = sheet.workbook.addPicture(bytesCpp, Workbook.PICTURE_TYPE_JPEG);
        //close the input stream
        inputStream3.close();

        CreationHelper helper = sheet.workbook.getCreationHelper();

        Drawing drawingCpp = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchorCpp = helper.createClientAnchor();
        anchorCpp.setCol1(0);
        anchorCpp.setRow1(lastRow);

        //Creates a picture
        Picture pictCpp = drawingCpp.createPicture(anchorCpp, pictureCppIdx);
        pictCpp.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pictCpp.resize();


    }

    def buildChartMultiAxis1(List<BruteData> listDataTrend1, Sheet sheet, def lastRow) {
        log.info(JsonOutput.toJson([methode: "buildChartMultiAxis", listDataTrend1: listDataTrend1.size(), lastRow: lastRow]))


        // def cppSbPitchList = listDataTrend1.findAll {it.dateTimeTrend.seconds == 0}.collect {[x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss") , y:  it?.cppSbPitch?.doubleValue() ]}
        // def cppPsPitchList = listDataTrend1.findAll {it.dateTimeTrend.seconds == 0}.collect {[x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss") , y:   it?.cppPsPitch?.doubleValue() ]}
        def mePsSpeedList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.mePsSpeedRpm?.doubleValue()] }
        def meSbSpeedList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.meStbSpeedRpm?.doubleValue()] }
        def elecMotorPsPowerList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.batterieSbState?.doubleValue()] }
        def elecMotorSbPowerList = listDataTrend1.findAll { it.dateTimeTrend.seconds == 0 && it.dateTimeTrend.minutes % 2 == 0 }.collect { [x: DateUtilExtensions.format(it?.dateTimeTrend, "dd/MM/yyyy HH:mm:ss"), y: it?.elecMotorStb?.doubleValue()] }
        // def strCppSbPitchData =  JsonOutput.toJson(cppSbPitchList)
        // def strCppPsPitch =  JsonOutput.toJson(cppPsPitchList)
        def strMePsSpeed = JsonOutput.toJson(mePsSpeedList)
        def strMeSbSpeed = JsonOutput.toJson(meSbSpeedList)
        def strelecMotorPsPower = JsonOutput.toJson(elecMotorPsPowerList)
        def strelecMotorSbPower = JsonOutput.toJson(elecMotorSbPowerList)


        def dataMultiAxisChart = "{" +
                "  type: 'line'," +
                " backgroundColor: 'rgb(255,255,255)'," +
                " data: {" +
                "    datasets: [" +
                "      {" +
                "       fill: false," +
                "       label: 'ME PS SPEED RPM'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderDash: [5] ," +
                "       borderColor: '#FF0000'," +
                "       yAxisID: 'y'," +
                "       data: " + strMePsSpeed.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ME PSB SPEED RPM'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderDash: [5] ," +
                "       borderColor: '#00FF00'," +
                "       yAxisID: 'y'," +
                "       data: " + strMeSbSpeed.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ELEC MOTOR PS POWER OUTPUT'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#FFFF00'," +
                "       yAxisID: 'y1'," +
                "       data: " + strelecMotorPsPower.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }," +
                "      {" +
                "       fill: false," +
                "       label: 'ELEC MOTOR SB POWER OUTPUT'," +
                "       lineTension: 0.4," +
                "       borderWidth: 0.5," +
                "       borderColor: '#FF9C06'," +
                "       yAxisID: 'y1'," +
                "       data: " + strelecMotorSbPower.replaceAll('\"x\"', "x").replaceAll('\"y\"', "y").replaceAll('\"', "'") +
                "      }" +
                "    ]" +
                "  }," +
                "  options: {" +
                "    chartArea: {" +
                "        backgroundColor: 'rgba(255, 255, 255, 0.4)'\n" +
                "    }," +
                "      elements: {" +
                "          point:{" +
                "              radius: 0" +
                "          }," +
                "       line: {" +
                "            tension: 0.4" +
                "        }" +
                "      }," +
                "    legend: {" +
                "            display: true" +
                "    }," +
                "    scales: {" +
                "      xAxes: [{" +
                "        type: 'time'," +
                "        time: {" +
                "            parser: 'DD/MM/YYYY HH:mm:ss'," +
                "            displayFormats: {" +
                "            minute: 'HH:mm'" +
                "            }," +
                "         }," +
                "        ticks: {" +
                "            fontSize: 8," +
                "            fontColor : 'rgb(0,0,0)', " +
                "            autoSkip: true ," +
                "                 tickThickness: 10, " +
                "              maxTicksLimit: 30 " +
                "         }" +
                "      }]," +
                "      yAxes: [{" +
                "       id: 'y', " +
                "       type: 'linear'," +
                "       display: true," +
                "       position: 'left'," +
                "        ticks: {" +

                "          fontColor : 'rgb(0,0,0)', " +
                "          fontSize: 8" +
                "        }," +
                "        gridLines: {\n" +
                "                display: true" +
                "            }" +
                "        }," +
                "        {" +
                "           id: 'y1', " +
                "           type: 'linear'," +
                "           display: true," +
                "           position: 'right'," +
                "           ticks: {" +
                "               fontSize: 8," +
                "               fontColor : 'rgb(0,0,0)', " +
                "                fontSize: 8" +
                "            }," +
                "           gridLines: {\n" +
                "                display: true" +
                "            }" +
                "      }]" +
                "    }" +
                "  }" +
                "}"

        QuickChart chartCppPitch = new QuickChart();
        chartCppPitch.setWidth(800);
        chartCppPitch.setHeight(150);
        chartCppPitch.setConfig(dataMultiAxisChart)
        def chartCppPitchFileName = csvToolService.getTempDir() + "multiAxis.png"
        chartCppPitch.toFile(chartCppPitchFileName);
        InputStream inputStream3 = new FileInputStream(chartCppPitchFileName)
        def bytesCpp = IOUtils.toByteArray(inputStream3)
        int pictureCppIdx = sheet.workbook.addPicture(bytesCpp, Workbook.PICTURE_TYPE_JPEG);
        //close the input stream
        inputStream3.close();

        CreationHelper helper = sheet.workbook.getCreationHelper();

        Drawing drawingCpp = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchorCpp = helper.createClientAnchor();
        anchorCpp.setCol1(0);
        anchorCpp.setRow1(lastRow);

        //Creates a picture
        Picture pictCpp = drawingCpp.createPicture(anchorCpp, pictureCppIdx);
        pictCpp.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pictCpp.resize();


    }


    def drawMap(int voyageId, Sheet sheet, def rowNum) {
        def context = new GeoApiContext.Builder()
                .apiKey(Helper.first().googleApiKey).connectTimeout(0, TimeUnit.SECONDS)
                .build()
        def polygoneLonLine = VDLSiteData.findAllByElementNameIlike("%Site Longitude0%")
        def polygoneLatLine = VDLSiteData.findAllByElementNameIlike("%Site Latitude0%")

        //def trajectLonLine = VDLSiteData.findAllByElementNameIlike("%Site Plateform OSS Longitude%").sort { a, b -> a.id <=> b.id }
        // def trajectLatLine = VDLSiteData.findAllByElementNameIlike("%Site Plateform OSS Latitude%").sort { a, b -> a.id <=> a.id }
        def trajectLonLine1 = VDLSiteData.findAllByElementNameIlike("%Site Plateform Longitude%").sort { a, b -> a.id <=> b.id }
        def trajectLatLine1 = VDLSiteData.findAllByElementNameIlike("%Site Plateform Latitude%").sort { a, b -> a.id <=> a.id }
//        //  def pathRequest = "http://maps.googleapis.com/maps/api/staticmap"
        //  def strRequest = "center=${trajectLatLine?.first()?.site0},${trajectLonLine?.first()?.site0}&zoom=10&size=600x600"
        StaticMapsRequest req = StaticMapsApi.newRequest(context, new Size(800, 800))
        req.center(new LatLng(new Double("47.211425"), new Double("-2.600584")))
        req.zoom(11)
//        Path path = new Path()
//        path.color("0x065C8C")
//        path.weight(3)
//        path.geodesic(true)

        List<LatLng> positions = []
        for (int i = 1; i <= 10; i++) {
            def elementLonName = (i == 10) ? "Site Longitude0$i" : "Site Longitude00$i"
            def elementLatName = (i == 10) ? "Site Latitude0$i" : "Site Latitude00$i"
            def latitude = polygoneLatLine.find { it.elementName == elementLatName }?.site0
            def longitude = polygoneLonLine.find { it.elementName == elementLonName }?.site0
            if (latitude && new Double(latitude) != 0 && longitude && new Double(longitude) != 0) {
                def latLong = new LatLng(new Double(latitude), new Double(longitude))
                def siteCircle = VDLSiteData.findByTag("fCTV85_SiteCircle.$i")?.site0
                def circleRadius = new Double(siteCircle).intValue()
                if (circleRadius == 0) {
                    println(circleRadius: circleRadius, siteCircle: siteCircle, tag: "fCTV85_SiteCircle.$i")
                    circleRadius = 3000
                }
                def paths = getCircleAsPolyline(latLong, circleRadius)
//                path.addPoint(new LatLng(new Double(latitude), new Double(longitude)));
                println(latitude: latitude, longitude: longitude, circleRadius: circleRadius, i: i)
                EncodedPolyline polyline = new EncodedPolyline(paths)
                req = req.paramAddToList("path", "color:0x21A0FF|fillcolor:0x21A0FF|weight:1|enc:" + polyline.getEncodedPath());
            }
        }
        def voyageTrends = BruteData.findAll("from BruteData b where b.voyageId =:voyageId order by dateTimeTrend asc", [voyageId: voyageId])
        println([voyage: voyageId, nbrVoyageTrends: voyageTrends.size()])
        def modSize = (voyageTrends.size() > 12000) ? 4 : 2

        // EncodedPolyline path = new EncodedPolyline(positions)
//        req.path(path)
        def listPosition = []


        def i = 0
        trajectLonLine1.each {

            def lonPrecis = new Double(it.site0)
            def latD = trajectLatLine1.find { it.tag == "fCTV85_SitePlateformLatitude.$i" }?.site0
            def latPrecis = new Double(latD)

            def speed = 0
            //     def color = setColorGradiant(speed)
            // println([latitude: latPrecis, longitude: lonPrecis, color: color])


            Path path1 = new Path()
            path1.weight(4)
            path1.color("0x000000")
            path1.fillcolor("0x000000")
            if (latPrecis != 0 && lonPrecis != 0) {
                path1.addPoint(new LatLng(latPrecis, lonPrecis))
                path1.addPoint(new LatLng(latPrecis + 0.000001, lonPrecis))
                //    println(path1.toUrlValue())
                req.paramAddToList("path", path1.toUrlValue())
            }
            i++

        }

        def j = 0
        def voyageTrendsResized = voyageTrends.findAll { it.dateTimeTrend.seconds == 0 && (it.dateTimeTrend.minutes % modSize == 0) }
        println(voyageTrendsResized: voyageTrendsResized.size())
        def firstTrends = voyageTrendsResized[0]
        def lastTrends = voyageTrends.last()
        voyageTrendsResized.add(lastTrends)
        voyageTrendsResized.each {
            if (it.gpsLat > 40 && it.gpsLat < 51 && it.gpsLon > -5 && it.gpsLon < -1) {
                log.info(JsonOutput.toJson([methode: "drawMap", message: "Coordonnée GPS correct (lat: $it.gpsLat , lon: $it.gpsLon )"]))

                def speed = it.speedOverGround ?: 0
                def color = setColorGradiant(speed)
                if (j + 1 < voyageTrends.size()) {
                    if (voyageTrendsResized[j + 1] != null) {
                        if (Math.abs(voyageTrendsResized[j + 1].gpsLat - it.gpsLat) > 0.006 ||
                                (Math.abs(voyageTrendsResized[j + 1].gpsLon - it.gpsLon) > 0.006)) {
                            Path path1 = new Path()
                            path1.color(color)
                            path1.geodesic(true)
                            path1.weight(2)
                            if (j == 0)
                                path1.addPoint(new LatLng(firstTrends.gpsLat, firstTrends.gpsLon))

                            path1.addPoint(new LatLng(it.gpsLat, it.gpsLon))
                            path1.addPoint(new LatLng(voyageTrendsResized[j + 1].gpsLat, voyageTrendsResized[j + 1].gpsLon))
                            // println( path1.toUrlValue())
                            req.paramAddToList("path", path1.toUrlValue())
                        }
                    }
                }

            } else {
                log.error(JsonOutput.toJson([methode: "drawMap", message: "Coordonnée GPS incorrect (lat: $it.gpsLat , lon: $it.gpsLon )"]))
            }
            j++
        }
//        simulationTest.each {
//            def speed = 0
//            if (it.lat > 47.2668397 && it.lat < 47.2898397) {
//                speed = 14
//            } else if (it.lat > 47.2468397 && it.lat <= 47.2668397) {
//                speed = 18
//            } else if (it.lat > 47.2068397 && it.lat <= 47.2468397) {
//                speed = 12
//            }
//            def color = setColorGradiant(speed)
//            if (j + 1 < simulationTest.size()) {
//                Path path1 = new Path()
//                path1.color(color)
//                path1.geodesic(true)
//                path1.weight(2)
//                path1.addPoint(it)
//                path1.addPoint(simulationTest[j + 1])
//                req.paramAddToList("path", path1.toUrlValue())
//
//            }
//            j++
//        }

//        for (int i1 = 1; i1 <= 200; i1++) {
//            def latitude = trajectLatLine1.find { it.tag == "fCTV85_SitePlateformLatitude.$i1" }?.site0
//            def longitude = trajectLonLine1.find { it.tag == "fCTV85_SitePlateformLongitude.$i1" }?.site0
//            println(latitude: latitude)
//            println(longitude: longitude)
//
//            if(latitude && longitude && new Double(latitude) != 0 &&  new Double(longitude) != 0) {
//                if (i1 + 1 <= 200) {
//                    def nextLatitude = trajectLatLine1.find { it.tag == "fCTV85_SitePlateformLatitude.${i1 + 1}" }?.site0 ?: "0"
//                    def nextLongitude = trajectLonLine1.find { it.tag == "fCTV85_SitePlateformLongitude.${i1 + 1}" }?.site0 ?: "0"
//                    println(nextLongitude: nextLongitude)
//                    println(nextLatitude: nextLatitude)
//                    if(nextLatitude && nextLongitude && new Double(nextLatitude) != 0 &&  new Double(nextLongitude) != 0) {
//
//                        def trend = BruteData.findByVoyageIdAndGpsLatBetweenAndGpsLonBetween(voyageId, new Double(latitude), new Double(nextLatitude), new Double(nextLongitude), new Double(longitude))
//                        def speed = trend?.speedOverGround ?: 0
//                        def color = setColorGradiant(speed)
//
//                        Path path1 = new Path()
//                        path1.color("0x33B6FF")
//                        path1.fillcolor("0x33B6FF")
//
//                        path1.weight(0)
//                        path1.addPoint(new LatLng(new Double(latitude), new Double(longitude)))
//                        path1.addPoint(new LatLng(new Double(nextLatitude), new Double(nextLongitude)))
//                        println(path1.toUrlValue())
//                        req.paramAddToList("path", path1.toUrlValue())
//                    }
//                }
//            }
//
//        }
        def newReq = req.await()
        ByteArrayInputStream bais = new ByteArrayInputStream(newReq.imageData)
        def tmpdir = csvToolService.getTempDir()

        IOUtils.copy(bais, new File("${tmpdir}file_${voyageId}.png"))
        InputStream inputStreamC = new FileInputStream("${tmpdir}file_${voyageId}.png")
        def bytesCpp = IOUtils.toByteArray(inputStreamC)
        int pictureCppIdx = sheet.workbook.addPicture(bytesCpp, Workbook.PICTURE_TYPE_JPEG);
        //close the input stream

        CreationHelper helper = sheet.workbook.getCreationHelper();

        Drawing drawingCpp = sheet.createDrawingPatriarch();

        //Create an anchor that is attached to the worksheet
        ClientAnchor anchorCpp = helper.createClientAnchor();
        anchorCpp.setCol1(2);
        anchorCpp.setRow1(rowNum - 1);

        //Creates a picture
        Picture pictCpp = drawingCpp.createPicture(anchorCpp, pictureCppIdx);
        pictCpp.setFillColor(255, 255, 255)
        //Reset the image to the original size
        pictCpp.resize(0.4, 0.4);


        // JAUGUE
        InputStream inputStream1 = new FileInputStream("/MARINELEC/BASES DE DONNEES/Template/jaugue.jpeg")
        def bytesJaugue = IOUtils.toByteArray(inputStream1)
        int pictureJaugueIdx = sheet.workbook.addPicture(bytesJaugue, Workbook.PICTURE_TYPE_JPEG);

        CreationHelper helperJaugue = sheet.workbook.getCreationHelper();

        Drawing drawingJaugue = sheet.createDrawingPatriarch();
        ClientAnchor anchorJaugue = helperJaugue.createClientAnchor();
        anchorJaugue.setCol1(14);
        anchorJaugue.setRow1(rowNum);

        Picture pictJaugue = drawingJaugue.createPicture(anchorJaugue, pictureJaugueIdx);
        pictJaugue.setFillColor(255, 255, 255)
        pictJaugue.resize();


    }

    def setColorGradiant(def speed) {
        def color = "0x097909"
        if (speed < 10) {
            color = "0x097909"
        } else if (speed >= 10 && speed < 11) {
            color = "0x0c8e0c"

        } else if (speed >= 11 && speed < 12) {
            color = "0x0C8E0C"

        } else if (speed >= 12 && speed < 13) {
            color = "0x50B20F"
        } else if (speed >= 13 && speed < 14) {
            color = "0x7EC114"
        } else if (speed >= 14 && speed < 15) {
            color = "0xD3DE21"
        } else if (speed >= 15 && speed < 16) {
            color = "0xF6D229"
        } else if (speed >= 16 && speed < 17) {
            color = "0xF69829"
        } else if (speed >= 17 && speed < 18) {
            color = "0xF65929"
        } else if (speed >= 17 && speed < 18) {
            color = "0xF64629"
        } else if (speed <= 18) {
            color = "0xFE3817"
        }
        return color
    }

    private static ArrayList<LatLng> getCircleAsPolyline(LatLng center, int radiusMeters) {
        ArrayList<LatLng> path = new ArrayList<>();
        def EARTH_RADIUS_KM = 6371
        double latitudeRadians = center.lat * Math.PI / 180.0;
        double longitudeRadians = center.lng * Math.PI / 180.0;
        double radiusRadians = radiusMeters / 1000.0 / EARTH_RADIUS_KM;

        double calcLatPrefix = Math.sin(latitudeRadians) * Math.cos(radiusRadians);
        double calcLatSuffix = Math.cos(latitudeRadians) * Math.sin(radiusRadians);

        for (int angle = 0; angle < 361; angle += 10) {
            double angleRadians = angle * Math.PI / 180.0;

            double latitude = Math.asin(calcLatPrefix + calcLatSuffix * Math.cos(angleRadians));
            double longitude = ((longitudeRadians + Math.atan2(Math.sin(angleRadians) * Math.sin(radiusRadians) * Math.cos(latitudeRadians), Math.cos(radiusRadians) - Math.sin(latitudeRadians) * Math.sin(latitude))) * 180) / Math.PI;
            latitude = latitude * 180.0 / Math.PI;

            path.add(new LatLng(latitude, longitude));
        }

        return path;
    }

    /**
     * Given a array of coordinates [longitude, latitude], returns the dms
     * (degrees, minutes, seconds) representation
     *
     * @param coordinates
     *            array of coordinates, with 2+ elements
     * @return dms representation for given array
     */
    private static String processCoordinates(float[] coordinates) {
        String converted0 = decimalToDMS(coordinates[1]);
        final String dmsLat = coordinates[0] > 0 ? ORIENTATIONS[0] : ORIENTATIONS[1];
        converted0 = converted0.concat(" ").concat(dmsLat);

        String converted1 = decimalToDMS(coordinates[0]);
        final String dmsLng = coordinates[1] > 0 ? ORIENTATIONS[2] : ORIENTATIONS[3];
        converted1 = converted1.concat(" ").concat(dmsLng);

        return converted0.concat(", ").concat(converted1);
    }

    /**
     * Given a decimal longitudinal coordinate such as <i>-79.982195</i> it will
     * be necessary to know whether it is a latitudinal or longitudinal
     * coordinate in order to fully convert it.
     *
     * @param coord
     *            coordinate in decimal format
     * @return coordinate in D°M′S″ format
     * @see <a href='https://goo.gl/pWVp60'>Geographic coordinate conversion
     *      (wikipedia)</a>
     */
    private static String decimalToDMS(float coord) {

        float mod = coord % 1;
        int intPart = (int) coord;

        String degrees = String.valueOf(intPart);

        coord = mod * 60;
        mod = coord % 1;
        intPart = (int) coord;
        if (intPart < 0)
            intPart *= -1;

        String minutes = String.valueOf(intPart);

        coord = mod * 60;
        intPart = (int) coord;
        if (intPart < 0)
            intPart *= -1;

        String seconds = String.valueOf(intPart);
        String output = Math.abs(Integer.parseInt(degrees)) + "°" + minutes + "'" + seconds + "\"";

        return output;
    }

}
