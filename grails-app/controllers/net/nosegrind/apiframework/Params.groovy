package net.nosegrind.apiframework



/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/

import static groovyx.gpars.GParsPool.withPool
import grails.converters.JSON
import grails.converters.XML

import groovy.json.JsonSlurper
import org.grails.web.util.WebUtils
import net.nosegrind.apiframework.Timer
import javax.servlet.forward.*
import org.grails.groovy.grails.commons.*


abstract class Params{

    def formats = ['text/html','text/json','application/json','text/xml','application/xml']
    List optionalParams = ['method','format','contentType','encoding','action','controller','v','apiCombine', 'apiObject']

    void initParams() {
        //println("#### [ParamsService : initParams ] ####")
        params.method = request.method
        List tempType = request.getHeader('Content-Type')?.split(';')
        params.encoding = (tempType != null && tempType?.size() > 1) ? tempType[1] : 'UTF-8'
        String type = (tempType?.size() > 0) ? tempType[0] : (request.getHeader('Content-Type')) ? request.getHeader('Content-Type') : 'application/json'
        params.contentType = (type) ? formats.find{ type.startsWith(it) }[0].toString() : type
        //String queryString = request.getQueryString()
        params.format = request.format

        if (request?."${params.format}") {
            LinkedHashMap content = [:]
            switch (params.format) {
                case 'XML':
                    String xml = request."${params.format}".toString()
                    def slurper = new XmlSlurper()
                    slurper.parseText(json).each(){ k,v ->
                        params.put(k, v)
                    }
                    break
                case 'JSON':
                    String json = request."${params.format}".toString()
                    def slurper = new JsonSlurper()

                    withPool{
                        slurper.parseText(json).eachParallel { k, v ->
                            params.put(k, v)
                        }
                    }
                    break
                default:
                    break
            }

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
            //throw new Exception("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:",e)
            println("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:"+e)
        }
    }

    List getApiParams(LinkedHashMap definitions){
        //println("#### [ParamsService : getApiParams ] ####")
        //try{
            List apiList = []
            definitions.each(){ key, val ->
                if (request.isUserInRole(key) || key == 'permitAll') {
                    val.each(){ it2 ->
                        if (it2) {
                            apiList.add(it2.name)
                        }
                    }
                }
            }

            return apiList
        //}catch(Exception e){
        //    throw new Exception("[ParamsService :: getApiParams] : Exception - full stack trace follows:",e)
        //}
    }

    boolean checkURIDefinitions(LinkedHashMap requestDefinitions){
        //println("#### [ParamsService : checkUriDefinitions ] ####")
        // put in check to see if if app.properties allow for this check
        try{
            List requestList = getApiParams(requestDefinitions)
            HashMap methodParams = getMethodParams()

            //GrailsParameterMap params = RCH.currentRequestAttributes().params
            //println(request.method)
            //println(methodParams)
            List paramsList = methodParams."${request.method.toLowerCase()}".keySet() as List

            paramsList.removeAll(optionalParams)
            if(paramsList.containsAll(requestList)){
                paramsList.removeAll(requestList)
                if(!paramsList){
                    return true
                }
            }
            return false
        }catch(Exception e) {
            //throw new Exception("[ApiLayerService :: checkURIDefinitions] : Exception - full stack trace follows:",e)
            println("[ParamsService :: checkURIDefinitions] : Exception - full stack trace follows:"+e)
        }
    }

    List getRedirectParams(){
        //println("#### [ParamsService : getRedirectParams ] ####")
        def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
        //def uri = HOLDER.getServletContext().getControllerActionUri(request)
        return uri[1..(uri.size()-1)].split('/')
    }

    HashMap getMethodParams(){
        //println("#### [ParamsService : getMethodParams ] ####")
        try{
            Map paramsRequest = [:]
            //withPool(4) {
                paramsRequest = params.findAll { it2 -> !optionalParams.contains(it2.key) }
            //}
            Map paramsGet = [:]
            Map paramsPost = [:]

            paramsGet = WebUtils.fromQueryString(request.queryString ?: "")
            paramsPost = paramsRequest.minus(paramsGet)
            if(paramsPost['id']){
                paramsGet['id'] = paramsPost['id']
                paramsPost.remove('id')
            }

            return ['get':paramsGet,'post':paramsPost]
        }catch(Exception e){
            //throw new Exception("[ParamsService :: getMethodParams] : Exception - full stack trace follows:",e)
            println("[ParamsService :: getMethodParams] : Exception - full stack trace follows:"+e)
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
        println("#### [ParamsService : processJson ] ####")
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
                        withPool(20) {
                            j.eachParallel { key, val ->
                                if (val instanceof List) {
                                    def child = [:]
                                    withExistingPool {
                                        val.eachParallel { it2 ->
                                            withExistingPool {
                                                it2.eachParallel { key2, val2 ->
                                                    child[key2] = val2
                                                }
                                            }
                                        }
                                    }
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