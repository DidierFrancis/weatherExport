package sun.wheatherExport

import grails.plugins.orm.auditable.Auditable

class Autorisations implements Auditable{

    Date createdAt = new Date()

    //audit-trail fields
    String userCreate
    Date dateCreated
    String userUpdate
    Date lastUpdated

    static belongsTo = [privilege: Privileges, role: sun.security.Role]

    static constraints = {
    }
}
