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

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import grails.web.servlet.mvc.GrailsParameterMap
import javax.servlet.forward.*
import org.grails.groovy.grails.commons.*
import javax.servlet.http.HttpServletResponse
import groovy.transform.CompileStatic

// extended by Intercepters
@CompileStatic
abstract class TraceCommLayer extends TraceCommProcess{


    @Resource
    TraceService traceService

    /***************************
     * REQUESTS
     ***************************/
    boolean handleApiRequest(ApiDescriptor cache, HttpServletRequest request, HttpServletResponse response, GrailsParameterMap params){
        traceService.startTrace('TraceCommLayer','handleApiRequest')
        try{
            // CHECK ACCESS TO METHOD
            /*
            List roles = cache['roles'] as List
            if(!checkAuth(request,roles)){
                response.status = 401
                response.setHeader('ERROR','Unauthorized Access attempted')
                return false
            }
            */

            // CHECK VERSION DEPRECATION DATE
            List deprecated = cache['deprecated'] as List

            if(deprecated?.get(0)){
                if(checkDeprecationDate(deprecated[0].toString())){
                    String depMsg = deprecated[1].toString()
                    response.status = 400
                    response.setHeader('ERROR',depMsg)
                    traceService.endTrace('TraceCommLayer','handleApiRequest')
                    return false
                }
            }

            def method = cache['method']?.toString().trim()

            // DOES api.methods.contains(request.method)
            if(!isRequestMatch(method,request.method.toString())){
                response.status = 400
                response.setHeader('ERROR',"Request method doesn't match expected method.")
                traceService.endTrace('TraceCommLayer','handleApiRequest')
                return false
            }
            traceService.endTrace('TraceCommLayer','handleApiRequest')
            return true
        }catch(Exception e){
            traceService.endTrace('TraceCommLayer','handleApiRequest')
            throw new Exception("[ApiCommLayer : handleApiRequest] : Exception - full stack trace follows:",e)
        }
    }

    /***************************
    * RESPONSES
     ***************************/
    def handleApiResponse(LinkedHashMap requestDefinitions, List roles, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
        traceService.startTrace('TraceCommLayer','handleApiResponse')
        try{
            String authority = getUserRole() as String
            response.setHeader('Authorization', roles.join(', '))

            List<HashMap> temp = (requestDefinitions["${authority}"])?requestDefinitions["${authority}"] as List<HashMap>:requestDefinitions['permitAll'] as List<HashMap>
            List responseList = temp.collect(){ it.name }

            LinkedHashMap content = [:]
            if(params.controller!='apidoc') {
                LinkedHashMap result = parseURIDefinitions(model, responseList)

                // will parse empty map the same as map with content
                content = parseResponseMethod(request, params, result)
            }else{
                content = parseResponseMethod(request, params, model)
            }
            traceService.endTrace('TraceCommLayer','handleApiResponse')
            return content

        }catch(Exception e){
            traceService.endTrace('TraceCommLayer','handleApiResponse')
            throw new Exception("[ApiCommLayer : handleApiResponse] : Exception - full stack trace follows:",e)
        }
    }


}
