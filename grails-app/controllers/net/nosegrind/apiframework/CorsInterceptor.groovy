package net.nosegrind.apiframework

import grails.compiler.GrailsCompileStatic

/**
 * Add Cross-Origin Resource Sharing (CORS) headers for Grails applications. These headers make it possible for
 * Javascript code served from a different host to easily make calls to your application.
 *
 * @see https://github.com/davidtinker/grails-cors
 */
@GrailsCompileStatic
class CorsInterceptor {

    CorsService corsService

    CorsInterceptor() {
        matchAll() // match all controllers
    }

    boolean before() {
        !corsService.processPreflight(request, response)
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

}
