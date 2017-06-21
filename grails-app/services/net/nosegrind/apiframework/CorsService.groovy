/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

package net.nosegrind.apiframework

import grails.transaction.Transactional
import grails.util.Environment

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.json.JsonSlurper
import groovy.util.XmlSlurper

import java.util.Enumeration
import org.springframework.http.HttpStatus

import com.google.common.io.CharStreams

@Transactional
class CorsService {

    def grailsApplication

    boolean processPreflight(HttpServletRequest request, HttpServletResponse response) {
        Map corsInterceptorConfig = (Map) grailsApplication.config.corsInterceptor
println("process preflight...")
        String[] includeEnvironments = corsInterceptorConfig['includeEnvironments']?: null
        String[] excludeEnvironments = corsInterceptorConfig['excludeEnvironments']?: null
        String[] allowedOrigins = corsInterceptorConfig['allowedOrigins']?: null

        if( excludeEnvironments && excludeEnvironments.contains(Environment.current.name) )  { // current env is excluded
            // skip
            return false
        } else if( includeEnvironments && !includeEnvironments.contains(Environment.current.name) )  {  // current env is not included
            // skip
            return false
        }

        String origin = request.getHeader("Origin")
        boolean options = ("OPTIONS" == request.method)

        if (options) {
            response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, TRACE, PATCH, OPTIONS")
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Headers", "Cache-Control, Pragma, WWW-Authenticate, Origin, authorization, Content-Type, Access-Control-Request-Headers")
                //response.setHeader("Access-Control-Allow-Headers","*")
                response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, PATCH, OPTIONS")
                response.setHeader("Access-Control-Max-Age", "3600")
                //request.getHeader("Access-Control-Request-Headers")
            }
            //response.status = HttpStatus.OK.value()
        }

        if(allowedOrigins && allowedOrigins.contains(origin)) { // request origin is on the white list
            // add CORS access control headers for the given origin
            response.setHeader("Access-Control-Allow-Origin", origin)
            response.setHeader("Access-Control-Allow-Credentials", "true")
            response.status = HttpStatus.OK.value()
            //return false
        } else if( !allowedOrigins ) { // no origin; white list
            // add CORS access control headers for all origins
            response.setHeader("Access-Control-Allow-Origin", origin ?: "*")
            response.setHeader("Access-Control-Allow-Credentials", "true")
            response.status = HttpStatus.OK.value()
            //return false
        }

        return options
    }
}
