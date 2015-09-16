// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'net.nosegrind.apitoolkit.Person'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'net.nosegrind.apitoolkit.PersonRole'
grails.plugin.springsecurity.authority.className = 'net.nosegrind.apitoolkit.Role'

grails.plugin.springsecurity.rememberMe.alwaysRemember = true
grails.plugin.springsecurity.rememberMe.cookieName="apiTest"
grails.plugin.springsecurity.rememberMe.key="_grails_"
//grails.plugin.springsecurity.rememberMe.persistent = true
//grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'net.nosegrind.apitoolkit.PersistentLogin'
//grails.plugin.springsecurity.auth.forceHttps=true

grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.ui.encodePassword = false

//grails.plugin.springsecurity.fii.rejectPublicInvocations = false
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        '/**':             ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/':                              ['permitAll'],
        '/index':                         ['permitAll'],
        '/index.gsp':                     ['permitAll'],
        '/**/js/**':                      ['permitAll'],
        '/**/css/**':                     ['permitAll'],
        '/**/images/**':                  ['permitAll'],
        '/**/favicon.ico':                ['permitAll'],
        '/login/**':					  ['permitAll'],
        '/logout/**':          		      ['permitAll']
]