package sun.security


import sun.wheatherExport.RestfulController

class RoleController extends RestfulController<Role> {
	static responseFormats = ['json', 'xml']

    RoleController() {
        super(Role)
    }

}
