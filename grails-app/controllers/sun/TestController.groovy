package sun

import grails.converters.JSON
import grails.web.context.ServletContextHolder
import org.apache.commons.io.FileUtils
import sun.constante.TypeException
import sun.exception.SaqException
import sun.plugins.jasper.JasperExportFormat
import sun.plugins.jasper.JasperReportDef
import sun.security.User

import java.text.NumberFormat

import static sun.exception.SaqTryCatch.check

class TestController {
    static responseFormats = ['json', 'xml']
    def jasperService

    def save() {
        def u = new User(password: 'mara', username: 'mara')
        def rs = check(u, TypeException.SAVE, "Bay fall dioum gua")

        render((rs as User) as JSON)
    }

    def delete() {
        def rs = check(User.findById(1), TypeException.SAVE, "Bay fall dioum gua")
        if (rs instanceof SaqException) {
            render(rs as JSON)
            return
        }
        render([message: "Deleted"] as JSON)
    }

    def update() {}

    def find() {}

    def testContratJasper() {
        String tempDir = System.getProperty("java.io.tmpdir")
        String appPath = "${ServletContextHolder.servletContext.getRealPath("/")}"
        def c = Calendar.getInstance()
        def month = c.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale.FRANCE).toUpperCase()
        LinkedHashMap<String, String> params = [NOM: "CONTRAT DE LOCATION IMMOBILIERE ${month} ${c.get(Calendar.YEAR)}".toString(), LOGO_PATH: appPath + 'reports/logo.png']
        params.DATE = " ${month} ${c.get(Calendar.YEAR)}".toString()
        params.FILLIALE = 'DIDIER FRANCIS SILOMITO TENDENG'
        params.INSTITUTION = 'SUN-TELECOM'
        params.POLICE = 'A500120000B001'
        params.PRODUIT = 'APARTEMENT'
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE)
        format.maximumFractionDigits = 2
        def prime_collect = '100 000 000'
        def surprime = '100 000 000'
        def prime = '100 000 000'
        def cionGest = '50000'
        params.MNT_PRIME_COLLECT = '10 000'
        params.MNT_PRIME = '10 000 XOF'
        params.MNT_SURPRIME = '10 000 XOF'
        params.MNT_CION = '10 000 XOF'

        def reportDef = new JasperReportDef(name: 'sama_test.jrxml', folder: appPath + "reports", fileFormat: JasperExportFormat.PDF_FORMAT)

        reportDef.parameters = params
        def f = new File("${tempDir}/test_contrat_${new Date().time}.pdf")
        FileUtils.writeByteArrayToFile(f, jasperService.generateReport(reportDef).toByteArray())
        downloadFile1(f)
    }

    def downloadFile1(def file) {
        if (file) {
            response.setContentType("application/vnd.ms-excel")
            response.setHeader("Content-disposition", "attachment; filename=${file.name}")
            response.outputStream << file.bytes
        } else {
            response.sendError(404)
        }
    }
}
