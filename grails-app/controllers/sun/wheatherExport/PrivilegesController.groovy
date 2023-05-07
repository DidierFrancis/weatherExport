package sun.wheatherExport

class PrivilegesController extends RestfulController<Privileges> {
    static responseFormats = ['json', 'xml']
    def utilsService

    PrivilegesController() {
        super(Privileges)
    }

    def listePrivilegesV1(Integer max) {
        params.max = max ?: 10
        return [
                data  : listAllResources(params).findAll {
                    it.niveau == 1
                },
                total : listAllResources(params).findAll {
                    it.niveau == 1
                }.size(),
                max   : params.max,
                offset: params.int("offset") ?: 0,
                sort  : params.sort,
                order : params.order]

    }


    def getPrivileges(Integer max) {
        params.max = max ?: 10
        def errors = new ArrayList()
        def data = request.JSON
        if (data == null) {
            errors.add("Veuilllez specifier les donnees")
        }
        if (!errors.isEmpty()) {
            render model: [errors: errors], template: '/errors/custom'
            return
        }
        try {
            def privileges = utilsService.getPrivileges(data)
            if (privileges.isEmpty()) {
                respond data: privileges, responseCode: 200
            } else {
                def toReturn = [:]
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


                toReturn.putAt(data, niveaux1)

                render(
                        model:
                                [data  : toReturn,
                                 total : privileges.findAll {
                                     it.niveau == 1
                                 }.size(),
                                 max   : params.max,
                                 offset: params.int("offset") ?: 0],
                        view: 'listePrivileges'
                )
            }
        }
        catch (Exception ex) {
            render model: [errors: [ex.message]], template: '/errors/custom'
        }
    }
}
