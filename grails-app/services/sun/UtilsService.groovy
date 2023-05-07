package sun

import grails.gorm.transactions.Transactional
import sun.constante.States
import sun.exception.SaqException
import sun.security.Role
import sun.security.User
import sun.wheatherExport.Privileges

@Transactional
class UtilsService {
    //  :Bloquer ou Debloquer un utilisateur
    def bloquerDebloquerUser(User user, String type) {
        def rep = [:]
        if (type == States.BLOCKED.value) {
            user.enabled = false
            rep = [message: "Compte désactivé avec success", status: user.enabled]
        } else {
            user.enabled = true
            rep = [message: "Compte activé avec success", status: user.enabled]
        }
        return rep
    }

    def getPrivileges(def data) {
        Role role = Role.get(data?.idRole as Long)
        if (!role)
            throw new SaqException("400", "Le role est  introuvable")

        def privileges = Privileges.findAll().findAll {
            it.autorisations.findAll {
                it.role == role
            }
        }
        return privileges

    }


}
