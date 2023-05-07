package sun.wheatherExport.vdlFiles

import grails.plugins.orm.auditable.Auditable
import sun.constante.TypeVDL
import sun.security.User

class VDLFile {
    Long id
    TypeVDL typeVDL
    String recipeName
    String fileName
    Long sizeSet
    Long fileId



    static constraints = {
    }

}
