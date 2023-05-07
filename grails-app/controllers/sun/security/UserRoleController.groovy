package sun.security

class UserRoleController extends sun.wheatherExport.RestfulController<UserRole> {
	static responseFormats = ['json', 'xml']

    UserRoleController() {
        super(UserRole)
    }

    def index() { }
}
