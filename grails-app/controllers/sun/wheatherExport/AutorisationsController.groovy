package sun.wheatherExport


import groovy.sql.Sql
import org.springframework.transaction.annotation.Transactional
import sun.security.Role
import sun.dto.RoleDTO

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

class AutorisationsController extends  RestfulController<Autorisations>{
	static responseFormats = ['json', 'xml']
    def dataSource
    AutorisationsController() {
        super(Autorisations)
    }



    @Transactional
    def addAutorisations(RoleDTO rolePrivilegeDTO) {
        if (rolePrivilegeDTO == null) {
            render status: NOT_FOUND
            return
        }
        try {

            Role role = Role.findById(rolePrivilegeDTO.role)
            if (role == null) {
                throw new Exception("ce roleId n'existe pas ${role.id}")
            }
            def allAutorisations = Autorisations.findAllByRole(role)
            allAutorisations.each {
                def query = "delete from autorisations where id=${it.id}"
                def sql = new Sql(dataSource)
                sql.execute(query)
            }
            def privileges = rolePrivilegeDTO.privileges
            privileges.each {
                Privileges privilege = Privileges.findById(it)
                if (privilege == null) {
                    throw new Exception("ce PrivilegeId n'existe pas ${it}")
                }

                def autorisations = new Autorisations(role: role, privilege: privilege)
                autorisations.save()
                return autorisations
            }
            respond data: role, responseCode: OK.value()
        }
        catch (Exception ex) {
            ex.printStackTrace()
            render model: [errors: [ex.message]], template: '/errors/custom'
        }
    }}
