package sun.security


import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.userdetails.GrailsUserDetailsService
import grails.plugin.springsecurity.userdetails.NoStackUsernameNotFoundException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import sun.wheatherExport.Autorisations

class AllUserDetailsService implements GrailsUserDetailsService {

    /**
     * Some Spring Security classes (e.g. RoleHierarchyVoter) expect at least
     * one role, so we give a user with no granted roles this one which gets
     * past that restriction but doesn't grant anything.
     */
    static final List NO_ROLES = [new SimpleGrantedAuthority(SpringSecurityUtils.NO_ROLE)]

    UserDetails loadUserByUsername(String username, boolean loadRoles)
            throws UsernameNotFoundException {
        return loadUserByUsername(username)
    }

    @Transactional(readOnly = true, noRollbackFor = [IllegalArgumentException, UsernameNotFoundException])
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = User.findByUsername(username)
        if (!user) throw new NoStackUsernameNotFoundException()

        def roles = user.authorities

        def authorities = roles.collect {
            new SimpleGrantedAuthority(it.authority)
        }
        def autorisations = [:]
        roles.each { role ->
            //recuperation liste des privileges d'un role
            def privileges = Autorisations.findAllByRole(Role.findByAuthority(role.authority)).collect { autorisation ->
                autorisation.privilege

            }
            def niveaux1 = privileges.findAll {
                it.niveau == 1
            }
            def niveaux2 = privileges.findAll {
                it.niveau == 2
            }
            def niveaux3 = privileges.findAll {
                it.niveau == 3
            }
            def niveaux4 = privileges.findAll {
                it.niveau == 4
            }

            niveaux1.each {
                it.privileges = []
            }
            niveaux2.each {
                it.privileges = []
            }
            niveaux3.each {
                it.privileges = []
            }
            niveaux4.each {
                it.privileges = []
            }

            for (priv4 in niveaux4) {
                for (priv3 in niveaux3) {
                    if (priv4.parent_id == priv3) {
                        priv3.privileges.add(priv4)
                    }
                }
            }

            for (priv3 in niveaux3) {
                for (priv2 in niveaux2) {
                    if (priv3.parent_id == priv2) {
                        priv2.privileges.add(priv3)
                    }
                }
            }
            for (priv2 in niveaux2) {
                for (priv1 in niveaux1) {
                    if (priv2.parent_id == priv1) {
                        priv1.privileges.add(priv2)
                    }
                }
            }

            autorisations.putAt(role, niveaux1)
        }

        return new UsersDetails(user.username, user.password, user.enabled,
                !user.accountExpired, !user.passwordExpired,
                !user.accountLocked, authorities ?: NO_ROLES, user.id,
                user.firstName, user.lastName, user.email, user.telephone, autorisations)
    }
}
