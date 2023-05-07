package sun.dto

import grails.validation.Validateable

class RoleDTO implements Validateable {
    String name
    Long role
    List<Long> privileges


    static constraints = {
        name nullable: true
        role nullable: true
        privileges nullable: true

    }


}
