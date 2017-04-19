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
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpSession

import org.springframework.security.core.context.SecurityContextHolder as SCH
import java.text.SimpleDateFormat

import net.nosegrind.apiframework.RequestMethod
import static groovyx.gpars.GParsPool.withPool
import grails.converters.JSON
import grails.converters.XML
import grails.web.servlet.mvc.GrailsParameterMap

import javax.servlet.forward.*
import org.grails.groovy.grails.commons.*
import grails.core.GrailsApplication
import grails.util.Holders
import org.springframework.web.context.request.RequestContextHolder as RCH
import org.grails.core.DefaultGrailsDomainClass
import org.springframework.beans.factory.annotation.Autowired

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import net.nosegrind.apiframework.ApiCacheService

// extended by ApiCommLayer
abstract class ApiCommProcess{

    @Resource
    GrailsApplication grailsApplication

    @Autowired
    ApiCacheService apiCacheService

    List formats = ['text/html','text/json','application/json','text/xml','application/xml']
    List optionalParams = ['method','format','contentType','encoding','action','controller','v','apiCombine', 'apiObject','entryPoint','uri']

    boolean batchEnabled = Holders.grailsApplication.config.apitoolkit.batching.enabled
    boolean chainEnabled = Holders.grailsApplication.config.apitoolkit.chaining.enabled


    // set params for this 'loop'; these will NOT forward
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
            if(!params.apiChain){ params.apiChain = [:] }
            def chainVars = request.JSON
            if(!request.getAttribute('chainLength')){ request.setAttribute('chainLength',chainVars['chain'].size()) }
            chainVars['chain'].each() { k,v ->
                params.apiChain[k] = v
            }
        }
    }

    // TODO
    boolean checkRateLimit(){
        try{
            if(session['rateLimitTimestamp']==null){
                println("... setting timestamp")
                session['rateLimitTimestamp'] = System.currentTimeMillis()/1000
            }
            if(session['rateLimitCurrent']==null){
                println("... setting ratelimit")
                session['rateLimitCurrent'] = 1
                SCH.setContext(session['SPRING_SECURITY_CONTEXT'])
            }else{
                println("... updating ratelimit")
                session['SPRING_SECURITY_CONTEXT'] = SCH.getContext()
                int inc = session['SPRING_SECURITY_CONTEXT']['rateLimitCurrent']+1
                session['SPRING_SECURITY_CONTEXT']['rateLimitCurrent'] = inc
                SCH.setContext(session['SPRING_SECURITY_CONTEXT'])
            }
            session['SPRING_SECURITY_CONTEXT'] = SCH.getContext()
            println("rateLimitCurrent : "+session['SPRING_SECURITY_CONTEXT']['rateLimitCurrent'])
            return false
        }catch(Exception e){
            println(e)
            //throw new Exception("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:",e)
        }
    }

    // TODO
    void setSession(){

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

    /*
    * TODO : DEPRECATED
    public List getApiParams(LinkedHashMap definitions){
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
    */

    String getUserRole() {
        String authority = 'permitAll'
        if (springSecurityService.loggedIn){
            authority = springSecurityService.principal.authorities*.authority[0]
        }
        return authority
    }

    boolean checkAuth(HttpServletRequest request, List roles){

        try {
            boolean hasAuth = false
            if (springSecurityService.loggedIn) {
                def principal = springSecurityService.principal
                List userRoles = principal.authorities*.authority
                roles.each {
                    if (userRoles.contains(it) || it=='permitAll') {
                        hasAuth = true
                    }
                }
            }else{
                //println("NOT LOGGED IN!!!")
            }
            return hasAuth
        }catch(Exception e) {
            throw new Exception("[ApiCommProcess :: checkAuth] : Exception - full stack trace follows:",e)
        }
    }


    boolean checkDeprecationDate(String deprecationDate){
        try{
            def ddate = new SimpleDateFormat("MM/dd/yyyy").parse(deprecationDate)
            def deprecated = new Date(ddate.time)
            def today = new Date()
            if(deprecated < today ) {
                return true
            }
            return false
        }catch(Exception e){
            throw new Exception("[ApiCommProcess :: checkDeprecationDate] : Exception - full stack trace follows:",e)
        }
    }

    boolean checkRequestMethod(RequestMethod mthd,String method, boolean restAlt){
        if(!restAlt) {
            return (mthd.getKey() == method) ? true : false
        }
        return true
    }

    // TODO: put in OPTIONAL toggle in application.yml to allow for this check
    boolean checkURIDefinitions(GrailsParameterMap params,LinkedHashMap requestDefinitions){
        List reservedNames = ['batchLength','batchInc','chainInc','apiChain','_','max','offset']
        try{
            String authority = getUserRole() as String
            List temp = (requestDefinitions["${authority}"])?requestDefinitions["${authority}"] as List:(requestDefinitions['permitAll'][0]!=null)? requestDefinitions['permitAll'] as List:[]
            List requestList = (temp!=null)?temp.collect(){ it.name }:[]

            Map methodParams = getMethodParams(params)

            List paramsList = methodParams.keySet() as List

            // remove reservedNames from List
            reservedNames.each(){ paramsList.remove(it) }

            if (paramsList.size() == requestList.intersect(paramsList).size()) {
                return true
            }

            return false
        }catch(Exception e) {
           throw new Exception("[ApiCommProcess :: checkURIDefinitions] : Exception - full stack trace follows:",e)
        }
        return false
    }

    String parseResponseMethod(RequestMethod mthd, String format, GrailsParameterMap params, LinkedHashMap result){
        String content
        //String defaultEncoding = Holders.grailsApplication.config.apitoolkit.encoding
        //String encoding = request.getHeader('accept-encoding')?request.getHeader('accept-encoding'):defaultEncoding
        switch(mthd.getKey()) {
            case 'PURGE':
                // cleans cache; disabled for now
                break;
            case 'TRACE':
                break;
            case 'HEAD':
                break;
            case 'OPTIONS':
                String doc = getApiDoc(params)
                content = doc
                break;
            case 'GET':
            case 'PUT':
            case 'POST':
            case 'DELETE':
                switch(format){
                    case 'XML':
                        content = result as XML
                        break
                    case 'JSON':
                    default:
                        content = result as JSON
                }
                break;
        }

        return content
    }

    String parseRequestMethod(RequestMethod mthd, GrailsParameterMap params){
        String content
        //String defaultEncoding = grailsApplication.config.apitoolkit.encoding
        //String encoding = request.getHeader('accept-encoding')?request.getHeader('accept-encoding'):defaultEncoding
        switch(mthd.getKey()) {
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
                content = getApiDoc(params)
                break;
        }

        return content
    }

    LinkedHashMap parseURIDefinitions(LinkedHashMap model,ArrayList responseList){
        if(model[0].getClass().getName()=='java.util.LinkedHashMap'){
            model.each(){ key,val ->
                model[key] = this.parseURIDefinitions(val,responseList)
            }
            return model
        }else {
            //try {
                String msg = 'Error. Invalid variables being returned. Please see your administrator'

                //List paramsList
                //Integer msize = model.size()
                List paramsList = (model.size()==0)?[:]:model.keySet() as List

                paramsList?.removeAll(optionalParams)


                if (!responseList.containsAll(paramsList)) {
                    paramsList.removeAll(responseList)
                    paramsList.each() { it2 ->
                        model.remove("${it2}".toString())
                    }

                    if (!paramsList) {
                        return [:]
                    } else {
                        return model
                    }
                } else {
                    return model
                }

            //} catch (Exception e) {
             //   throw new Exception("[ApiCommProcess :: parseURIDefinitions] : Exception - full stack trace follows:", e)
            //}
        }
    }

    boolean isRequestMatch(String protocol,RequestMethod mthd){
        if(RequestMethod.isRestAlt(mthd.getKey())){
            return true
        }else{
            if(protocol == mthd.getKey()){
                return true
            }else{
                return false
            }
        }
        return false
    }

    /*
    * TODO : USED FOR TEST
    List getRedirectParams(GrailsParameterMap params){
        def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
        return uri[1..(uri.size()-1)].split('/')
    }
    */

    Map getMethodParams(GrailsParameterMap params){
        try{
            Map paramsRequest = [:]
            List myList = [1,2,3,4];
            paramsRequest = params.findAll { it2 -> !optionalParams.contains(it2.key) }
            return paramsRequest
        }catch(Exception e){
            throw new Exception("[ApiCommProcess :: getMethodParams] : Exception - full stack trace follows:",e)
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
            throw new Exception("[ApiCommProcess :: getApiDoc] : Exception - full stack trace follows:",e)
        }
    }

    // Used by getApiDoc
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
            throw new Exception("[ApiCommProcess :: processJson] : Exception - full stack trace follows:",e)
        }
    }

    LinkedHashMap convertModel(Map map){
        try{
            LinkedHashMap newMap = [:]
            String k = map.entrySet().toList().first().key

            if(map && (!map?.response && !map?.metaClass && !map?.params)){
                if (DomainClassArtefactHandler.isDomainClass(map[k].getClass())) {
                    newMap = formatDomainObject(map[k])
                    return newMap
                } else if(['class java.util.LinkedList', 'class java.util.ArrayList'].contains(map[k].getClass().toString())) {
                    newMap = formatList(map[k])
                    return newMap
                } else if(['class java.util.Map', 'class java.util.LinkedHashMap'].contains(map[k].getClass().toString())) {
                    newMap = formatMap(map[k])
                    return newMap
                }
            }
            return newMap
        }catch(Exception e){
            throw new Exception("[ApiCommProcess :: convertModel] : Exception - full stack trace follows:",e)
        }
    }

    // PostProcessService
    LinkedHashMap formatDomainObject(Object data){
        try{
            LinkedHashMap newMap = [:]

            newMap.put('id',data?.id)
            newMap.put('version',data?.version)

            DefaultGrailsDomainClass d = new DefaultGrailsDomainClass(data.class)
            d.persistentProperties.each() { it ->
                if((DomainClassArtefactHandler.isDomainClass(data[it.name].getClass()))){
                    newMap["${it.name}Id"] = data[it.name].id
                }else{
                    newMap[it.name] = data[it.name]
                    
                }
            }
            return newMap
        }catch(Exception e){
            throw new Exception("[ApiCommProcess :: formatDomainObject] : Exception - full stack trace follows:",e)
        }
    }

    // PostProcessService
    LinkedHashMap formatMap(LinkedHashMap map){
        LinkedHashMap newMap = [:]
        map.each(){ key,val ->
            if(val){
                if(DomainClassArtefactHandler.isDomainClass(val.getClass())){
                    newMap[key]=formatDomainObject(val)
                }else{
                    newMap[key] = ((val in java.util.ArrayList || val in java.util.List) || val in java.util.Map)?val:val.toString()
                }
            }
        }
        return newMap
    }

    // PostProcessService
    LinkedHashMap formatList(List list){
        LinkedHashMap newMap = [:]
        list.eachWithIndex(){ val, key ->
            if(val){
                if(DomainClassArtefactHandler.isDomainClass(val.getClass())){
                    newMap[key]=formatDomainObject(val)
                }else{
                    newMap[key] = ((val in java.util.ArrayList || val in java.util.List) || val in java.util.Map)?val:val.toString()
                }
            }
        }
        return newMap
    }


    boolean isCachedResult(Integer version, String className){
        Class clazz = grailsApplication.domainClasses.find { it.clazz.simpleName == className }.clazz

        def c = clazz.createCriteria()
        def currentVersion = c.get {
            projections {
                property('version')
            }
            maxResults(1)
            order("version", "desc")
        }

        return (currentVersion > version)?false:true
    }

    boolean isChain(HttpServletRequest request){
        String contentType = request.getAttribute('contentType')
        try{
            switch(contentType){
                case 'text/xml':
                case 'application/xml':
                    if(request.XML?.chain){
                        return true
                    }
                    break
                case 'text/json':
                case 'application/json':
                default:
                    if(request.JSON?.chain){
                        return true
                    }
                    break
            }
            return false
        }catch(Exception e){
            throw new Exception("[ApiResponseService :: isChain] : Exception - full stack trace follows:",e)
        }


    }
}
