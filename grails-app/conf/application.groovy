// Added by the Audit-Logging plugin:
grails.plugin.auditLog.auditDomainClassName = 'sun.AuditTrail'

grails.gorm.default.constraints = {
    '*'(nullable: true)
}

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'security.UserRole'
grails.plugin.springsecurity.authority.className = 'security.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        [pattern: '/', access: ['permitAll']],
        [pattern: '/error', access: ['permitAll']],
        [pattern: '/index', access: ['permitAll']],
        [pattern: '/index.gsp', access: ['permitAll']],
        [pattern: '/shutdown', access: ['permitAll']],
        [pattern: '/assets/**', access: ['permitAll']],
        [pattern: '/**/js/**', access: ['permitAll']],
        [pattern: '/**/css/**', access: ['permitAll']],
        [pattern: '/**/images/**', access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/public/**', access: ['permitAll']],
        [pattern: '/api/login', access: ['permitAll']],
        [pattern: '/api/users/forget-password', access: ['permitAll']],
        [pattern: '/api/users/reset-password', access: ['permitAll']],
        [pattern: '/api/logout', access: ['isFullyAuthenticated()']],
        [pattern: '/**', access: ['permitAll']]
]
grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/', filters: 'none'],
        [pattern: '/index', filters: 'none'],
        [pattern: '/assets/**', filters: 'none'],
        [pattern: '/**/js/**', filters: 'none'],
        [pattern: '/**/css/**', filters: 'none'],
        [pattern: '/**/images/**', filters: 'none'],
        [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/public/**', filters: 'none'],
        [pattern: '/api/users/forget-password', filters: 'none'],
        [pattern: '/api/users/reset-password', filters: 'none'],
        [pattern: '/api/**', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
        [pattern: '/**', filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
]

grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.rest.token.validation.useBearerToken = true



