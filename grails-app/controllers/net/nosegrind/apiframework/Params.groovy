/*
 * The MIT License (MIT)
 * Copyright 2014 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright/trademark notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.nosegrind.apiframework

import grails.converters.JSON
import grails.converters.XML
import javax.servlet.http.HttpServletRequest

import static groovyx.gpars.GParsPool.withPool
import grails.converters.JSON
import grails.converters.XML
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper

import javax.servlet.forward.*
import org.grails.groovy.grails.commons.*

import grails.util.Holders

abstract class Params{

    List formats = ['text/html','text/json','application/json','text/xml','application/xml']
    List optionalParams = ['method','format','contentType','encoding','action','controller','v','apiCombine', 'apiObject','entryPoint','uri','testvar']
    boolean batchEnabled = Holders.grailsApplication.config.apitoolkit.batching.enabled
    boolean chainEnabled = Holders.grailsApplication.config.apitoolkit.chaining.enabled


    /* set params for this 'loop'; these will NOT forward
    *
     */
    void setBatchParams(GrailsParameterMap params){
        if (batchEnabled) {
            def batchVars = request.getAttribute(request.format.toUpperCase())
            if(!request.getAttribute('batchLength')){ request.setAttribute('batchLength',batchVars['batch'].size()) }
            batchVars['batch'][request.getAttribute('batchInc').toInteger()].each() { k,v ->
                params."${k}" = v
            }
        }
    }

    void setChainParams(GrailsParameterMap params){
        if (chainEnabled) {
            params.apiChain = content?.chain
        }
    }


    LinkedHashMap getApiObjectParams(LinkedHashMap definitions){
        try{
            LinkedHashMap apiList = [:]
            definitions.each{ key,val ->
                if(request.isUserInRole(key) || key=='permitAll'){
                    val.each{ it ->
                        if(it){
                            apiList[it.name] = it.paramType
                        }
                    }
                }
            }
            return apiList
        }catch(Exception e){
            throw new Exception("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:",e)
        }
        return [:]
    }

    List getApiParams(LinkedHashMap definitions){
        try{
            List apiList = []
            definitions.each(){ key, val ->
                if (request.isUserInRole(key) || key == 'permitAll') {
                    val.each(){ it2 ->
                        apiList.add(it2.name)
                    }
                }
            }

            return apiList
        }catch(Exception e){
            throw new Exception("[ParamsService :: getApiParams] : Exception - full stack trace follows:",e)
        }
    }

    boolean checkRequestMethod(String method, boolean restAlt){
        if(!restAlt) {
            return (method == request.method.toUpperCase()) ? true : false
        }
        return true
    }

    boolean checkURIDefinitions(GrailsParameterMap params,LinkedHashMap requestDefinitions){
        List reservedNames = ['batchLength','batchInc']
        // put in check to see if if app.properties allow for this check
        try{
            List requestList = getApiParams(requestDefinitions)

            Map methodParams = getMethodParams(params)

            List paramsList = methodParams.keySet() as List

            // remove reservedNames from List
            reservedNames.each(){ paramsList.remove(it) }

            if (paramsList.size() == requestList.intersect(paramsList).size()) {
                return true
            }

            return false
        }catch(Exception e) {
           throw new Exception("[ApiLayerService :: checkURIDefinitions] : Exception - full stack trace follows:",e)
        }
        return false
    }

    LinkedHashMap parseRequestMethod(HttpServletRequest request, GrailsParameterMap params){
        LinkedHashMap data = [:]
        String defaultEncoding = grailsApplication.config.apitoolkit.encoding
        String encoding = request.getHeader('accept-encoding')?request.getHeader('accept-encoding'):defaultEncoding
        switch(request.method) {
            case 'PURGE':
                // cleans cache; disabled for now
                break;
            case 'TRACE':
                // placeholder
                break;
            case 'HEAD':
                // placeholder
                break;
            case 'OPTIONS':
                String doc = getApiDoc(params)
                data = ['content':doc,'contentType':request.getAttribute('contentType'),'encoding':encoding]
                break;
        }

        return ['apiToolkitContent':data.content,'apiToolkitType':request.getAttribute('contentType'),'apiToolkitEncoding':encoding]
    }

    List getRedirectParams(GrailsParameterMap params){
        def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
        return uri[1..(uri.size()-1)].split('/')
    }

    Map getMethodParams(GrailsParameterMap params){
        try{
            Map paramsRequest = [:]
            paramsRequest = params.findAll { it2 -> !optionalParams.contains(it2.key) }
            return paramsRequest
        }catch(Exception e){
            throw new Exception("[ParamsService :: getMethodParams] : Exception - full stack trace follows:",e)
        }
        return [:]
    }


    Boolean apiRoles(List list) {
        if(springSecurityService.principal.authorities*.authority.any { list.contains(it) }){
            return true
        }
        return false
    }


    String getApiDoc(GrailsParameterMap params){
        // TODO: Need to compare multiple authorities
        // TODO: check for ['doc'][role] in cache; if none, continue

        LinkedHashMap newDoc = [:]
        List paramDescProps = ['paramType','idReferences','name','description']
        try{
            def controller = grailsApplication.getArtefactByLogicalPropertyName('Controller', params.controller)
            if(controller){
                def cache = (params.controller)?apiCacheService.getApiCache(params.controller):null
                if(cache){
                    if(cache[params.apiObject][params.action]){

                        def doc = cache[params.apiObject][params.action].doc
                        def path = doc?.path
                        def method = doc?.method
                        def description = doc?.description


                        //def authority = springSecurityService.principal.authorities*.authority[0]
                        newDoc[params.action] = ['path':path,'method':method,'description':description]
                        if(doc.receives){
                            newDoc[params.action].receives = []

                            doc.receives.each{ it ->
                                if(apiRoles([it.key]) || it.key=='permitAll'){
                                    it.value.each(){ it2 ->
                                        LinkedHashMap values = [:]
                                        it2.each(){ it3 ->
                                            if(paramDescProps.contains(it3.key)){
                                                values[it3.key] = it3.value
                                            }
                                        }
                                        if(values) {
                                            newDoc[params.action].receives.add(values)
                                        }
                                    }

                                }
                            }
                        }

                        if(doc.returns){
                            newDoc[params.action].returns = []
                            List jsonReturns = []
                            doc.returns.each(){ v ->
                                if(apiRoles([v.key]) || v.key=='permitAll'){
                                    jsonReturns.add(["${v.key}":v.value])
                                    v.value.each(){ v2 ->
                                        LinkedHashMap values3 = [:]
                                        v2.each(){ v3 ->
                                            if(paramDescProps.contains(v3.key)){
                                                values3[v3.key] = v3.value
                                            }
                                        }
                                        if(values3) {
                                            newDoc[params.action].returns.add(values3)
                                        }
                                    }
                                    //newDoc[params.action].returns[v.key] = v.value
                                }
                            }

                            //newDoc[params.action].json = processJson(newDoc[params.action].returns)

                            newDoc[params.action].json = processJson(jsonReturns[0] as LinkedHashMap)
                        }

                        if(doc.errorcodes){
                            doc.errorcodes.each{ it ->
                                newDoc[params.action].errorcodes.add(it)
                            }
                        }

                        // store ['doc'][role] in cache

                        return newDoc as JSON
                    }
                }
            }
            return [:]
        }catch(Exception e){
            throw new Exception("[ApiLayer :: getApiDoc] : Exception - full stack trace follows:",e)
        }
    }


    private String processJson(LinkedHashMap returns){
        // TODO: Need to compare multiple authorities
        try{
            LinkedHashMap json = [:]
            returns.each{ p ->
                p.value.each{ it ->
                    if(it) {
                        ParamsDescriptor paramDesc = it

                        LinkedHashMap j = [:]
                        if (paramDesc?.values) {
                            j["$paramDesc.name"] = []
                        } else {
                            String dataName = (['PKEY', 'FKEY', 'INDEX'].contains(paramDesc?.paramType?.toString())) ? 'ID' : paramDesc.paramType
                            j = (paramDesc?.mockData?.trim()) ? ["$paramDesc.name": "$paramDesc.mockData"] : ["$paramDesc.name": "$dataName"]
                        }
                        withPool(20) { pool ->
                            j.eachParallel { key, val ->
                                if (val instanceof List) {
                                    def child = [:]
                                    withExistingPool(pool, {
                                        val.eachParallel { it2 ->
                                            withExistingPool(pool, {
                                                it2.eachParallel { key2, val2 ->
                                                    child[key2] = val2
                                                }
                                            })
                                        }
                                    })
                                    json[key] = child
                                } else {
                                    json[key] = val
                                }
                            }
                        }
                    }
                }
            }

            String jsonReturn
            if(json){
                jsonReturn = json as JSON
            }
            return jsonReturn
        }catch(Exception e){
            throw new Exception("[ParamsService :: processJson] : Exception - full stack trace follows:",e)
        }
    }

}