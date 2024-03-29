package sun.plugins.jasper

import grails.plugins.*
import net.sf.jasperreports.j2ee.servlets.ImageServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean

class JasperGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/jasperDemo/demo.gsp",
        "grails-app/views/error.gsp",
        "public/reports/**",
        "**/test/**",
        "docs/**"
    ]

    def title = "Jasper Plugin"
    def author = "Craig Andrews"
    def authorEmail = "candrews@integralblue.com"
    def description = '''
    Adds easy support for launching sun.jasper reports from GSP pages.
    After installing, run your application and request (app-url)/sun.jasper/demo for a demonstration and instructions.
    '''
    def profiles = ['web']

    def documentation = "http://www.grails.org/plugin/sun.jasper"
    def license = "APACHE"
    def developers = [	[ name: "Burt Beckwith", email: "burt@burtbeckwith.com" ],
                          [ name: "Puneet Behl", email: "puneet.behl007@gmail.com" ],
                          [ name: "Mansi Arora", email: "mansi.arora@tothenew.com" ],
                          [ name: "Manvendra Singh", email: "manvendrask@live.com" ],
    ]
    def issueManagement = [system: "JIRA", url: "https://github.com/puneetbehl/grails-sun.jasper/issues"]
    def scm = [url: "https://github.com/puneetbehl/grails-sun.jasper"]

    Closure doWithSpring() { {->
        imageServlet(ImageServlet)
        dispatchServletRegistrationBean(ServletRegistrationBean) {
            servlet = ref('imageServlet')
            urlMappings = ["/reports/image"]
        }
    } }

}
