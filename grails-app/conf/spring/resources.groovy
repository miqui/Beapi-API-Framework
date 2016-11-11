// Place your Spring DSL code here

import grails.plugin.springsecurity.rest.RestAuthenticationFilter
import grails.api.framework.SpringSecurityCORSFilter
import grails.api.framework.TokenCacheValidationFilter
import org.springframework.boot.context.embedded.FilterRegistrationBean

beans = {

    // IMPORTANT! - add this to your Spring config
    /*
    corsFilterDeregistrationBean(FilterRegistrationBean) {
        filter = ref('springSecurityCORSFilter')
        enabled = false
    }

    tokenFilterDeregistrationBean(FilterRegistrationBean) {
        filter = ref('tokenCacheValidationFilter')
        enabled = false
    }

    authFilterDeregistrationBean(FilterRegistrationBean) {
        filter = ref('restAuthenticationFilter')
        enabled = false
    }
    */
}
