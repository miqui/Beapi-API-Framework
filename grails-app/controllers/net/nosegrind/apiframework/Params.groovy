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

import static groovyx.gpars.GParsPool.withPool
import grails.converters.JSON
import grails.converters.XML
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper

import net.nosegrind.apiframework.Timer
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
        //println("#### [ParamsService : setBatchParams ] ####")
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

    /*
    void setApiParams(HttpServletRequest request){
        try{
            GrailsParameterMap params = RequestContextHolder.currentRequestAttributes().params
            if(request."$format"){
                request."$format".each{ k,v ->
                    params.put(k,v)
                }
            }
        }catch(Exception e){
            throw new Exception("[ParamsService :: setApiParams] : Exception - full stack trace follows:"+ e);
        }
    }
    */

    LinkedHashMap getApiObjectParams(LinkedHashMap definitions){
        //println("#### [ParamsService : getApiObjectParams ] ####")
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
    }

    List getApiParams(LinkedHashMap definitions){
        //println("#### [ParamsService : getApiParams ] ####")
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

    boolean checkURIDefinitions(String method,GrailsParameterMap params,LinkedHashMap requestDefinitions){
        println("#### [ParamsService : checkUriDefinitions ] ####")
        List reservedNames = ['batchLength','batchInc']
        // put in check to see if if app.properties allow for this check
        try{
            List requestList = getApiParams(requestDefinitions)

            Map methodParams = getMethodParams(params)
            if(method==request.method.toUpperCase()) {
                List paramsList = methodParams.keySet() as List
                // remove constants from check
                reservedNames.each(){
                    paramsList.remove(it)
                }

                if (paramsList.size() == requestList.intersect(paramsList).size()) {
                    return true
                }
            }
            return false
        }catch(Exception e) {
           throw new Exception("[ApiLayerService :: checkURIDefinitions] : Exception - full stack trace follows:",e)
        }
    }

    List getRedirectParams(GrailsParameterMap params){
        //println("#### [ParamsService : getRedirectParams ] ####")
        def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
        //def uri = HOLDER.getServletContext().getControllerActionUri(request)
        return uri[1..(uri.size()-1)].split('/')
    }

    Map getMethodParams(GrailsParameterMap params){
        //println("#### [ParamsService : getMethodParams ] ####")
        try{
            Map paramsRequest = [:]
            paramsRequest = params.findAll { it2 -> !optionalParams.contains(it2.key) }
            return paramsRequest
        }catch(Exception e){
            throw new Exception("[ParamsService :: getMethodParams] : Exception - full stack trace follows:",e)
        }
    }

    /*
    Map parseContentType(HttpServletRequest request, GrailsParameterMap params, Map map, LinkedHashMap returns){
        String content
        String encoding = (params.encoding)?params.encoding:"UTF-8"
        LinkedHashMap result = parseURIDefinitions(request,map,returns)
        switch(format){
            case 'XML':
                content = result as XML
                break
            case 'JSON':
            default:
                content = result as JSON
        }

        return ['content':content,'type':format,'encoding':encoding]
    }
    */

    /*
     * TODO: Need to compare multiple authorities
     */
    LinkedHashMap getApiDoc(){
        //println("#### [ParamsService : getApiDoc ] ####")
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

                        def authority = springSecurityService.principal.authorities*.authority[0]
                        newDoc[params.action] = ['path':path,'method':method,'description':description]
                        if(doc.receives){
                            newDoc[params.action].receives = [:]
                            doc.receives.each{ it ->
                                if(authority==it.key || it.key=='permitAll'){
                                    it.value.each(){ it2 ->
                                        it2.getProperties().each(){ it3 ->
                                            if(paramDescProps.contains(it3.key)){
                                                newDoc[params.action].receives[it3.key] = it3.value
                                            }
                                        }
                                    }
                                    //newDoc[params.action].receives[it.key] = it.value
                                }
                            }
                        }

                        if(doc.returns){
                            newDoc[params.action].returns = [:]
                            List jsonReturns = []
                            doc.returns.each(){ v ->
                                if(authority==v.key || v.key=='permitAll'){
                                    jsonReturns.add(['${v.key}':v.value])
                                    v.value.each(){ v2 ->
                                        v2.getProperties().each(){ v3 ->
                                            if(paramDescProps.contains(v3.key)){
                                                newDoc[params.action].returns[v3.key] = v3.value
                                            }
                                        }
                                    }
                                    //newDoc[params.action].returns[v.key] = v.value
                                }
                            }

                            //newDoc[params.action].json = processJson(newDoc[params.action].returns)
                            newDoc[params.action].json = processJson(jsonReturns)
                        }

                        if(doc.errorcodes){
                            doc.errorcodes.each{ it ->
                                newDoc[params.action].errorcodes.add(it)
                            }
                        }
                        return newDoc
                    }
                }
            }
            return [:]
        }catch(Exception e){
            throw new Exception("[ApiResponseService :: getApiDoc] : Exception - full stack trace follows:",e)
        }
    }

    /*
 * TODO: Need to compare multiple authorities
 */
    private String processJson(LinkedHashMap returns){
        //println("#### [ParamsService : processJson ] ####")
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
            throw new Exception("[ApiLayerService :: processJson] : Exception - full stack trace follows:",e)
        }
    }

}