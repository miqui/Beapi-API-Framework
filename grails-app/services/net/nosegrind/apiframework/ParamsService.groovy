package net.nosegrind.apiframework

import grails.converters.JSON
import grails.converters.XML
import grails.core.GrailsApplication
import org.grails.web.util.WebUtils
import org.springframework.web.context.request.RequestContextHolder


/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import javax.servlet.forward.*
import javax.servlet.http.HttpServletRequest
import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.web.context.request.RequestContextHolder

class ParamsService{

    GrailsApplication grailsApplication

    String format
    LinkedHashMap content = [:]
    String queryString

    void initParams(HttpServletRequest request){
        format = request.format.toUpperCase()
        queryString = request.getQueryString()
        if(request."$format"){
            request."$format"?.each() { key,value ->
                content.put(key,value)
            }
        }
    }

    GrailsParameterMap setApiParams(GrailsParameterMap params){
        try{
            if(content){
                content.each{ k,v ->
                    params.put(key,value)
                }
            }
        }catch(Exception e){
            throw new Exception("[ParamsService :: setApiParams] : Exception - full stack trace follows:"+ e);
        }
    }

    GrailsParameterMap setChainParams(GrailsParameterMap params){
        try {
            if (content) {
                content.each { k, v ->
                    if (chain && k == 'chain') {
                        params.apiChain = [:]
                        params.apiChain = content.chain
                        //if (content?.chain) {
                        //    request."${format}".remove('chain')
                        //}
                    }
                }
            }
        }catch(Exception e){
            throw new Exception("[ParamsService :: setChainParams] : Exception - full stack trace follows:"+ e);
        }
    }

    GrailsParameterMap setBatchParams(GrailsParameterMap params){
        try {
            if (content) {
                content.each { k, v ->
                    if (batch && k == 'batch') {
                        params.apiBatch = []
                        v.each { it ->
                            params.apiBatch.add(it)
                        }
                        params.apiBatch = params.apiBatch
                        //if (content?.batch) {
                        //    request.remove('batch')
                        //}
                    }
                }
            }
        }catch(Exception e){
            throw new Exception("[ParamsService :: setBatchParams] : Exception - full stack trace follows:"+ e);
        }
    }

    LinkedHashMap getApiObjectParams(HttpServletRequest request, LinkedHashMap definitions){
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

    List getApiParams(HttpServletRequest request, LinkedHashMap definitions){
        try{
            List apiList = []
            definitions.each{ key,val ->
                if(request.isUserInRole(key) || key=='permitAll'){
                    val.each{ it ->
                        if(it){
                            apiList.add(it.name)
                        }
                    }
                }
            }
            println("apilist : "+apiList)
            return apiList
        }catch(Exception e){
            throw new Exception("[ParamsService :: getApiParams] : Exception - full stack trace follows:",e)
        }
    }

    boolean checkURIDefinitions(HttpServletRequest request, LinkedHashMap requestDefinitions){
        println("####### paramsService:checkUriDefinitions ########")
        // put in check to see if if app.properties allow for this check
        try{
            List optionalParams = ['format','action','controller','apiName_v','contentType', 'encoding','apiChain', 'apiBatch', 'apiCombine', 'apiObject','apiObjectVersion', 'chain']
            List requestList = getApiParams(request, requestDefinitions)
            HashMap params = getMethodParams()

            //GrailsParameterMap params = RCH.currentRequestAttributes().params
            List paramsList = params."${request.method.toLowerCase()}".keySet() as List
            println("#### paramsList : "+paramsList)
            paramsList.removeAll(optionalParams)
            println("#### paramsList : "+paramsList)
            println("#### requestListList : "+requestList)
            if(paramsList.containsAll(requestList)){
                paramsList.removeAll(requestList)
                println("#### paramsListShouldBeEmpty : "+paramsList)
                if(!paramsList){
                    return true
                }
            }
            return false
        }catch(Exception e) {
         //   //throw new Exception("[ApiLayerService :: checkURIDefinitions] : Exception - full stack trace follows:",e)
            println("[ParamsService :: checkURIDefinitions] : Exception - full stack trace follows:"+e)
        }
    }

    List getRedirectParams(){
        def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
        //def uri = HOLDER.getServletContext().getControllerActionUri(request)
        return uri[1..(uri.size()-1)].split('/')
    }

    HashMap getMethodParams(){
        println("### getMethodParams")
        try{
            boolean isChain = false
            List optionalParams = ['action','controller','v','contentType', 'encoding','apiChain', 'apiBatch', 'apiCombine', 'apiObject','apiObjectVersion', 'chain']
            //HttpServletRequest request = getRequest()
            GrailsParameterMap params = RequestContextHolder.currentRequestAttributes().params
            Map paramsRequest = params.findAll {
                if(it.key=='apiChain'){ isChain=true }
                return !optionalParams.contains(it.key)
            }
            println("paramsRequest : "+paramsRequest)
            println("isChain : "+isChain)
            Map paramsGet = [:]
            Map paramsPost = [:]
            if(isChain){
                println("ischain - make sure to get id")
                paramsPost = paramsRequest
            }else{
                paramsGet = WebUtils.fromQueryString(queryString ?: "")
                paramsPost = paramsRequest.minus(paramsGet)
                if(paramsPost['id']){
                    paramsGet['id'] = paramsPost['id']
                    paramsPost.remove('id')
                }
            }
            return ['get':paramsGet,'post':paramsPost]
        }catch(Exception e){
            //throw new Exception("[ParamsService :: getMethodParams] : Exception - full stack trace follows:",e)
println("[ParamsService :: getMethodParams] : Exception - full stack trace follows:"+e)
        }
    }

    LinkedHashMap parseURIDefinitions(HttpServletRequest request, LinkedHashMap model,LinkedHashMap responseDefinitions){
        //try{
        ApiStatuses errors = new ApiStatuses()
        String msg = 'Error. Invalid variables being returned. Please see your administrator'
        List optionalParams = ['action','controller','apiName_v','contentType', 'encoding','apiChain', 'apiBatch', 'apiCombine', 'apiObject','apiObjectVersion', 'chain']
        List responseList = getApiParams(request,responseDefinitions)

        HashMap params = getMethodParams()
        //GrailsParameterMap params = RCH.currentRequestAttributes().params
        List paramsList = model.keySet() as List
        paramsList.removeAll(optionalParams)
        if(!responseList.containsAll(paramsList)){
            paramsList.removeAll(responseList)
            paramsList.each(){ it ->
                model.remove("${it}".toString())
            }
            if(!paramsList){
                errors._400_BAD_REQUEST(msg).send()
                return [:]
            }else{
                return model
            }
        }else{
            return model
        }
        //}catch(Exception e){
        //	throw new Exception("[ApiResponseService :: parseURIDefinitions] : Exception - full stack trace follows:",e)
        //}
    }

    void popBatch(){}

    void popChain(){}

    void assignContentTypeParam(){}

    void dropContentTypeParam(){}

    Map parseResponseMethod(HttpServletRequest request, GrailsParameterMap params, Map map, LinkedHashMap returns){
        Map data = [:]
        switch(request.method) {
            case 'PURGE':
                // cleans cache; disabled for now
                break;
            case 'TRACE':
                break;
            case 'HEAD':
                break;
            case 'OPTIONS':
                String contentType = (params.contentType)?params.contentType:'application/json'
                String encoding = (params.encoding)?params.encoding:"UTF-8"
                LinkedHashMap doc = getApiDoc(params)
                data = ['content':doc,'contentType':contentType,'encoding':encoding]
                break;
            case 'GET':
                println("GET")
                if(map?.isEmpty()==false){
                    data = parseContentType(request,params, map, returns)
                }
                break;
            case 'PUT':
                if(!map.isEmpty()){
                    data = parseContentType(request,params, map, returns)
                }
                break;
            case 'POST':
                if(!map.isEmpty()){
                    data = parseContentType(request,params, map, returns)
                }
                break;
            case 'DELETE':
                if(!map.isEmpty()){
                    data = parseContentType(request,params, map, returns)
                }
                break;
        }
        return ['apiToolkitContent':data.content,'apiToolkitType':data.contentType,'apiToolkitEncoding':data.encoding]
    }

    Map parseContentType(HttpServletRequest request, GrailsParameterMap params, Map map, LinkedHashMap returns){
        String content
        String contentType = "application/${format.toLowerCase()}"
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

    /*
     * TODO: Need to compare multiple authorities
     */
    LinkedHashMap getApiDoc(GrailsParameterMap params){
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
                                                //println("receives > ${it3.key} : ${it3.value}")
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
                                                //println("receives > ${v3.key} : ${v3.value}")
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
        def json = [:]
        returns.each{ p ->
            p.value.each{ it ->

                ParamsDescriptor paramDesc = it

                def j = [:]
                if(paramDesc?.values){
                    j[paramDesc.name]=[]
                }else{
                    String dataName=(['PKEY','FKEY','INDEX'].contains(paramDesc.paramType.toString()))?'ID':paramDesc.paramType
                    j = (paramDesc?.mockData?.trim())?["${paramDesc.name}":paramDesc.mockData]:["${paramDesc.name}":dataName]
                }
                j.each(){ key,val ->
                    if(val instanceof List){
                        def child = [:]
                        val.each(){ it2 ->
                            it2.each(){ key2,val2 ->
                                child[key2] = val2
                            }
                        }
                        json[key] = child
                    }else{
                        json[key]=val
                    }
                }
            }
        }

        if(json){
            json = json as JSON
        }
        return json
    }
}