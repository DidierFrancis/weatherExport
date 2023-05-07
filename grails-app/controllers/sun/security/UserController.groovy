package sun.security

import grails.gorm.transactions.Transactional
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import sun.constante.Constantes
import sun.constante.States
import sun.constante.TypeException
import sun.exception.SaqException
import sun.exception.SaqTryCatch

import java.time.temporal.ChronoUnit

import static org.springframework.http.HttpStatus.OK

class UserController extends sun.wheatherExport.RestfulController<User> {
    def springSecurityService
    def utilsService
    static responseFormats = ['json', 'xml']

    UserController() {
        super(User)
    }

    @Transactional
    def save(User user) {
        if (!user) {
            throw new SaqException("400", "Aucune information reçue pour procéder à l'enregistrement de l'utilisateur")
        }
        //Check uniqueness
        SaqTryCatch.check(User.findByEmail(user.email), TypeException.UNIQUE, "L'email ${user.email} est déjà utilisité, veuillez en choisir un autre")
        SaqTryCatch.check(User.findByTelephone(user.telephone), TypeException.UNIQUE, "Le numéro de téléphone ${user.telephone} est déjà utilisité, veuillez en choisir un autre")
        SaqTryCatch.check(User.findByUsername(user.username), TypeException.UNIQUE, "Le nom d'utilisateur ${user.username} est déjà utilisité, veuillez en choisir un autre")
        user = SaqTryCatch.check(user, TypeException.SAVE, "Erreur lors de l'enregistrement de l'utilisateur") as User

        render model: [user: user], view: 'show'
    }

    @Transactional
    def forgetPassword() {
        def email = request.JSON.email as String
        if (StringUtils.isEmpty(email))
            throw new SaqException("400", "L'email est obligatoire pour effectuer cette opération")

        def user = SaqTryCatch.check(User.findByEmail(email), TypeException.FIND, "L'email ${email} n'existe pas , veuillez vérifier les données saisies") as User

        String token = RandomStringUtils.randomNumeric(6)
        def now = new Date()
        def expiry = Date.from(now.toInstant().plus(1, ChronoUnit.DAYS))
        def userToken //= new UserResetToken(user: user, token: token, expiryDate: expiry)
        userToken //= SaqTryCatch.check(userToken, TypeException.SAVE, "Erreur lors de l'opération") as UserResetToken
        def tabMailTo = ["${user.email}".toString()]
        def tabTo = ["${user.email}"]

//        sendMail(tabMailTo, Constantes.REINITIALISAION_PWD, "Veuillez trouver le jeton de reinitialisation de votre mot de passe <b>${token}</b><br><br>" +
//                'NB: <i style="color: red;font-family: monospace;">Le jeton n\'est valable que pour 24h!<i/>', true)

        render model: ['message': "Veuillez consulter votre mail pour la réinitialisation de votre mot de passe !"], view: 'resetPassword'
    }

    @Transactional
    def changePassword() {
        def oldPassword = request.JSON.oldPassword as String
        def newPassword = request.JSON.newPassword as String
        def confirmPassword = request.JSON.confirmPassword as String
        def username = springSecurityService.getPrincipal()?.username as String
        User user = SaqTryCatch.check(User.findByUsername(username), TypeException.FIND, "Vous devez vous connecter pour pour changer de mot de passe") as User

        if (StringUtils.isEmpty(newPassword) || StringUtils.isEmpty(confirmPassword) || StringUtils.isEmpty(oldPassword)) {
            throw new SaqException("400", "Tous les arguments(`oldPassword`,`newPassword`, `confirmPassword`) sont obligatoires")
        }
        if (!springSecurityService.passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new SaqException("400", "Votre ancien mot de passe est incorrect")
        }
        if (newPassword != confirmPassword) {
            throw new SaqException("400", "Le mot de passe et sa confirmation ne sont pas identiques!")
        }

        user.password = "${newPassword}"
        user.hasPasswordUpdated = true
        user = SaqTryCatch.check(user, TypeException.SAVE, "Erreur lors de la modification du mot de passe") as User

        render model: ['message': "Mot de passe modifié avec succes!"], view: 'resetPassword'

    }

    def getUserByUsername() {
        def username = request.JSON.username as String
        if (StringUtils.isEmpty(username)) {
            throw new SaqException("400", "Veuilllez specifier nom de l'utilisateur")
        }
        def user = SaqTryCatch.check(User.findByUsername(username), TypeException.FIND, "Aucun utilisateur avec le username ${username} n'a été trouvé") as User

        render model: [user: user], view: 'show'
    }

    //   :Bloquer ou Debloquer un Utilisateur
    def bloquerDebloquerUtilisateur() {
        def email = request.JSON.email as String
        def action = request.JSON.action as String

        def user = SaqTryCatch.check(User.findByEmail(email), TypeException.FIND, "Aucun utilisateur avec l'email ${email} n'a été trouvé") as User

        if (StringUtils.isEmpty(action)) {
            throw new SaqException("400", "L'action est requis pour effectuer cette opération")
        }

        if (!States.values()*.value.contains(action.toLowerCase())) {
            throw new SaqException("400", "L'action doit etre `bloquer` ou `debloquer`!")
        }

        respond data: utilsService.bloquerDebloquerUser(user, action), responseCode: OK.value()
    }
}
