
import grails.util.Metadata

String apiVersion = Metadata.current.getApplicationVersion()
String entryPoint = "/v${apiVersion}".toString()
String batchEntryPoint = "b${apiVersion}"
String chainEntryPoint = "c${apiVersion}"
String tracertEntryPoint = "t${apiVersion}"
String domainEntryPoint = "d${apiVersion}"

// Added by the Spring Security Core plugin:
grails {
    plugin {
        springsecurity {
            securityConfigType = "InterceptUrlMap"
            userLookup {
                userDomainClassName = 'net.nosegrind.apiframework.Person'
                authorityJoinClassName = 'net.nosegrind.apiframework.PersonRole'
            }
            authority.className = 'net.nosegrind.apiframework.Role'
            rememberMe {
                alwaysRemember = true
                cookieName = 'apiTest'
                key = '_grails_'
                //persistent = true
                //persistentToken.domainClassName = 'net.nosegrind.apiframework.PersistentLogin'
            }

            logout.postOnly = false
            ui.encodePassword = false

            auth {
                forceHttps = false
                loginFormUrl = '/login/auth/'
                ajaxLoginFormUrl = '/login/authAjax/'
            }
            failureHandler {
                defaultFailureUrl = '/'
                ajaxAuthFailUrl = '/'
            }

            providerNames = ['daoAuthenticationProvider', 'anonymousAuthenticationProvider', 'rememberMeAuthenticationProvider']

        }
    }
}

//'JOINED_FILTERS,-securityContextPersistenceFilter,-logoutFilter,-authenticationProcessingFilter,-securityContextHolderAwareRequestFilter,-rememberMeAuthenticationFilter,-anonymousAuthenticationFilter,-exceptionTranslationFilter'
grails.plugin.springsecurity.filterChain.chainMap = [
        "/${entryPoint}/**": 'none',
        "/${batchEntryPoint}/**": 'none',
        "/${chainEntryPoint}/**": 'none',
        "/${tracertEntryPoint}/**": 'none',
        "/${domainEntryPoint}/**": 'none'
]

grails.plugin.springsecurity.interceptUrlMap = [
        '/':                ['permitAll'],
        '/error':           ['permitAll'],
        '/index':           ['permitAll'],
        '/index.gsp':       ['permitAll'],
        '/shutdown':        ['permitAll'],
        '/assets/**':       ['permitAll'],
        '/error/**' :       ['permitAll'],
        '/hook/**' :        ['permitAll'],
        '/apidoc/**' :      ['permitAll'],
        '/**/js/**':        ['permitAll'],
        '/**/css/**':       ['permitAll'],
        '/**/images/**':    ['permitAll'],
        '/**/favicon.ico':  ['permitAll'],
        '/login':           ['permitAll'],
        '/login/**':        ['permitAll'],
        '/logout':          ['permitAll'],
        '/logout/**':       ['permitAll']
]




