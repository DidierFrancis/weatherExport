package sun.wheatherExport

class UserGateway {
    String username
    String password
    String code
    String urlBase
    String urlConnexion
    String clientId
    String clientSecret
    String grantType
    String scope
    Date tokenDate = new Date()
    String token

    static constraints = {
        code unique: true
    }
    static mapping = {
        token sqlType: 'text'
        password sqlType: 'text'
    }
}
