package sun

import grails.converters.JSON
import grails.util.Environment
import grails.util.Holders
import grails.web.context.ServletContextHolder
import org.apache.commons.lang.time.DateUtils
import org.ini4j.Ini
import org.ini4j.IniPreferences
import org.springframework.beans.factory.annotation.Value
import sun.exception.SaqException
import sun.helpers.DateUtilExtensions
import sun.security.Role
import sun.security.User
import sun.security.UserRole
import sun.weatherExport.DailyReportsJob
import sun.wheatherExport.Helper
import sun.wheatherExport.PathFile
import sun.wheatherExport.vdlFiles.BruteData

import java.util.prefs.Preferences

import static sun.exception.ExceptionUtils.errorsAsTabList

class BootStrap {

    def vdlFileService
    def gatewayService


    def init = { servletContext ->
        registrerSaqException()
        //testLib()
        bootstrap()
    }
    def destroy = {
    }

    def bootstrap() {

        Ini ini = new Ini(new File("/MARINELEC/config.ini"));
        Preferences prefs = new IniPreferences(ini);
        def paths = ['FILE_BRUTE_PATH1', 'FILE_BRUTE_PATH2', 'FILE_BRUTE_PATH3', 'FILE_MATRIX_PATH', 'FILE_MATRIX_TEMPLATE', 'FOLDER_TRENDS', 'FOLDER_VOYAGE', 'FOLDER_DOCKING', 'FILE_VDL_BASE',
                     'FILE_VDL_CUSTOMER', 'FILE_VDL_SITE', 'FILE_VDL_MASTER', 'FILE_DICO_TRENDS', 'FILE_DTN']
        paths.each {
            def p = prefs.node("FilePaths").get(it, null)
            if (p) {
                PathFile pf = new PathFile()
                pf.name = it
                pf.path = p
                if (p.contains("VDL")) {
                    pf.type = 'FILE'
                } else {
                    pf.type = 'DIR'
                }
                pf.save()
            }

        }

        gatewayService.getDateRapport()
        gatewayService.loginToDTNApi()
        gatewayService.getDTNForecast()

        vdlFileService.getFileData()
        DailyReportsJob.triggerNow()


    }

    /**
     * method for register SaqException domain for JSON rendering
     * @return the marshaller object
     */
    def registrerSaqException() {
        JSON.registerObjectMarshaller(SaqException) { SaqException it ->
            def output = [:]
            output['code'] = it.code
            output['message'] = it.message
            output['debugMessage'] = it.debugMessage

            if (it.errors != null) {
                if (!it.errors.allErrors.isEmpty()) {
                    output['erreurs'] = errorsAsTabList(it.errors)
                }
            }
            if (it.exception != null) {
                output['exception'] = it.exception.message
            }
            return output
        }
    }

    def testLib() {
    }


}
