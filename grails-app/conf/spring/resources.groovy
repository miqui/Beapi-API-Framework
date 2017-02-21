/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

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
