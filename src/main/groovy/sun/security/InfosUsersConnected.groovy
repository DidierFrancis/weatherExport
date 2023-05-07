package sun.security


import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.rest.token.rendering.AccessTokenJsonRenderer
import groovy.json.JsonBuilder
import org.springframework.security.core.GrantedAuthority

class InfosUsersConnected implements AccessTokenJsonRenderer {
    @Override
    String generateJson(AccessToken accessToken) {
        List map = []
        Map<Role, List<sun.wheatherExport.Privileges>> source = accessToken.principal?.autorisations
        source.each { privileges ->

            privileges.value.each {
                p2 ->
                    map.addAll(p2.toJson().privileges)
            }

        }
        def autorizations = map.sort { a, b -> a.niveau <=> b.niveau ?: a.ordre <=> b.ordre }
        Map response = [
                id           : accessToken.principal.userId,
                username     : accessToken.principal.username,
                firstName    : accessToken.principal?.firstName,
                lastName     : accessToken.principal?.lastName,
                email        : accessToken.principal?.email,
                telephone    : accessToken.principal?.telephone,
                access_token : accessToken.accessToken,
                token_type   : "Bearer",
                expires_in   : accessToken.expiration,
                refresh_token: accessToken.refreshToken,
                roles        : accessToken.authorities.collect { GrantedAuthority role -> role.authority },
                privileges   : autorizations
        ]

        return new JsonBuilder(data: response).toPrettyString()
    }


}
