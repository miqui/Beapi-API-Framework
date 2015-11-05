import grails.util.Metadata

String apiVersion = Metadata.current.getApplicationVersion()
// fix for dots not working with spring security pathing
//String apiVersion = ''
String entryPoint = "/v${apiVersion}".toString()
String batchEntryPoint = "b${apiVersion}"
String chainEntryPoint = "c${apiVersion}"
String tracertEntryPoint = "t${apiVersion}"
String domainEntryPoint = "d${apiVersion}"

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'net.nosegrind.apiframework.Person'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'net.nosegrind.apiframework.PersonRole'

grails.plugin.springsecurity.authority.className = 'net.nosegrind.apiframework.Role'
grails.plugin.springsecurity.rememberMe
grails.plugin.springsecurity.rememberMe.alwaysRemember = true
grails.plugin.springsecurity.rememberMe.cookieName = 'apiTest'
grails.plugin.springsecurity.rememberMe.key = '_grails_'
// grails.plugin.springsecurity.rememberMe.persistent = true
// grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'net.nosegrind.apiframework.PersistentLogin'

grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.ui.encodePassword = false

grails.plugin.springsecurity.auth.forceHttps = false
grails.plugin.springsecurity.auth.loginFormUrl = '/login/auth'
grails.plugin.springsecurity.auth.ajaxLoginFormUrl = '/login/authAjax'
grails.plugin.springsecurity.adh.errorPage = null
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = '/'
grails.plugin.springsecurity.failureHandler.ajaxAuthFailUrl = '/'

grails.plugin.springsecurity.providerNames = ['daoAuthenticationProvider', 'anonymousAuthenticationProvider', 'rememberMeAuthenticationProvider']

// move to RequestMap once stabilized
grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"
grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.fii.rejectPublicInvocations = true

//'JOINED_FILTERS,-securityContextPersistenceFilter,-logoutFilter,-authenticationProcessingFilter,-securityContextHolderAwareRequestFilter,-rememberMeAuthenticationFilter,-anonymousAuthenticationFilter,-exceptionTranslationFilter'
/*
grails.plugin.springsecurity.filterChain.chainMap = [
        "/${entryPoint}/**".toString() : 'none',
        "/${batchEntryPoint}/**".toString() : 'none',
        "/${chainEntryPoint}/**".toString()  'none',
        "/${tracertEntryPoint}/**".toString() : 'none',
        "/${domainEntryPoint}/**".toString() : 'none'
]
*/
grails.plugin.springsecurity.interceptUrlMap = [
        "/${entryPoint}/**" : 'permitAll',
        '/**':              'permitAll',
        '/': ["IS_AUTHENTICATED_FULLY"],
        '/error':           'permitAll',
        '/index':           'permitAll',
        '/index.gsp':       'permitAll',
        '/shutdown':        'permitAll',
        '/assets/**':       'permitAll',
        '/error/**' :       'permitAll',
        '/hook/**' :        'permitAll',
        '/apidoc/**' :      'permitAll',
        '/**/js/**':        'permitAll',
        '/**/css/**':       'permitAll',
        '/**/images/**':    'permitAll',
        '/**/favicon.ico':  'permitAll',
        '/login':           'permitAll',
        '/login/**':        'permitAll',
        '/logout':          'permitAll',
        '/logout/**':       'permitAll'
]




