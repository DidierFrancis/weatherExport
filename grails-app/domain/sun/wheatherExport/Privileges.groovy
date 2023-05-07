package sun.wheatherExport

import grails.plugins.orm.auditable.Auditable

class Privileges implements Auditable {
    String code
    String libelle
    Integer niveau
    String lien
    String icon
    String isMenu
    Date createdAt = new Date()

    //audit-trail fields
    String userCreate
    Date dateCreated
    String userUpdate
    Date lastUpdated

    static hasMany = [autorisations: Autorisations,
                      privileges   : Privileges]
    static belongsTo = [parent_id: Privileges]

    static mapping = {
        version false
    }

    def listIndexedColumns() {
        return ['code', 'libelle', 'niveau', 'lien']
    }
    static constraints = {
        code nullable: true, unique: true
        libelle nullable: true
        niveau nullable: true
        lien nullable: true
        icon nullable: true
        isMenu nullable: true, maxSize: 3
        parent_id nullable: true
    }

    def displayField() {
        return 'libelle'
    }

    def toJson() {
        def tab = []
        tab.add([id      : id, code: code, lien: lien, icon: icon, isMenu: isMenu, niveau: niveau, libelle: libelle, selected: true, createdAt: createdAt, parent_id: parent_id ? parent_id.id : null,
                 children: privileges.collect { p2 ->
                     [id      : p2.id, code: p2.code, lien: p2.lien, icon: p2.icon, isMenu: p2.isMenu, niveau: p2.niveau, libelle: p2.libelle, selected: true, createdAt: p2.createdAt, parent_id: p2.parent_id ? p2.parent_id.id : null,
                      children: p2.privileges.collect { p3 ->
                          [id      : p3.id, code: p3.code, lien: p3.lien, icon: p3.icon, isMenu: p3.isMenu, niveau: p3.niveau, libelle: p3.libelle, selected: true, createdAt: p3.createdAt, parent_id: p3.parent_id ? p3.parent_id.id : null,
                           children: p3.privileges.collect { p4 -> [id: p4.id, code: p4.code, lien: p4.lien, icon: p4.icon, isMenu: p4.isMenu, niveau: p4.niveau, libelle: p4.libelle, selected: true, createdAt: p4.createdAt, parent_id: p4.parent_id ? p4.parent_id.id : null, children: []] }]
                      }]
                 }])


        return [privileges: tab.unique()]

    }

}
