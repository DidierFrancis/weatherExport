package sun.security

import grails.plugin.springsecurity.rest.RestAuthenticationFailureHandler
import groovy.json.JsonOutput
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException

import javax.security.auth.login.AccountExpiredException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UsersAuthentifications extends RestAuthenticationFailureHandler {

    void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpStatus.BAD_REQUEST.value())
        response.setContentType('application/json')
        if (exception instanceof AccountExpiredException) {
            response.getWriter().write(JsonOutput.toJson([message: 'springSecurity.errors.login.expired', responseCode: HttpStatus.BAD_REQUEST.value()]))

        } else if (exception instanceof CredentialsExpiredException) {
            response.getWriter().write(JsonOutput.toJson([message: 'springSecurity.errors.login.passwordExpired', responseCode: HttpStatus.BAD_REQUEST.value()]))
        } else if (exception instanceof DisabledException) {
            response.getWriter().write(JsonOutput.toJson([message: 'votre compte a été désactivé', responseCode: HttpStatus.BAD_REQUEST.value()]))

        } else if (exception instanceof LockedException) {
            response.getWriter().write(JsonOutput.toJson([message: 'votre connexion a été   verrouillée', responseCode: HttpStatus.BAD_REQUEST.value()]))

        } else {
            response.getWriter().write(JsonOutput.toJson([message: 'login ou mot de passe incorrect(s)', responseCode: HttpStatus.BAD_REQUEST.value()]))

        }
        response.addHeader('WWW-Authenticate', 'Bearer')
        response.getWriter().flush()
    }
}


