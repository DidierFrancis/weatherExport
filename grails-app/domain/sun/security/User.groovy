package sun.security

import grails.compiler.GrailsCompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils
import sun.constante.Constantes

import javax.persistence.Transient

//import static sun.saq.lib.utilities.Utilities.sendMail

@GrailsCompileStatic
@EqualsAndHashCode(includes = 'username')
@ToString(includes = 'username', includeNames = true, includePackage = false)
class User implements Serializable {
    private static final long serialVersionUID = 1

    String username
    String password
    String firstName
    String lastName
    String displayName
    String email
    String telephone
    boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
//    boolean firstConnection = true
    boolean hasPasswordUpdated = false
    Date createdAt = new Date()

    @Transient
    transient String clearPassword
    static transients = ['clearPassword']

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static constraints = {
        password nullable: false, blank: false, password: true
        username nullable: false, blank: false, unique: true
    }

    static mapping = {
        password column: '`password`'
    }

    def beforeValidate() {
        if (username != "admin")
            username = email
        if (!password) {
            clearPassword = RandomStringUtils.randomAlphanumeric(10)
            password = clearPassword
        }

        displayName = "${firstName ?: ''} ${lastName ?: ''}"
    }

    def afterInsert() {
        def tabTo = ["${email}"]
//        sendMail(tabTo as Collection<String>, Constantes.CREATION_COMPTE,
//                "<p>Bienvenue <span><b> ${firstName}</span><span>${lastName}</b> </span>" + "</p>" +
//                        "<p>    Votre inscription a été bien prise en compte.<br/>" +
//                        "Votre identifiant est: <span>${email}</span><br/>" +
//                        "Votre mot de passe par defaut est: <span>${clearPassword}</span><br/>" +
//                        '<i style="color: red;font-family: monospace;">Vous pouvez vous connectez pour le modifier!<i/>', true)
    }


}
