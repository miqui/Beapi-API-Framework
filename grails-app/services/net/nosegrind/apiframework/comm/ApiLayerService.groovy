package net.nosegrind.apiframework.comm

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 ****************************************************************************
*/


import grails.converters.JSON
import grails.converters.XML
import org.springframework.web.context.request.RequestAttributes

import javax.servlet.http.HttpServletResponse

//import java.lang.reflect.Method
import grails.core.GrailsApplication

import javax.servlet.forward.*
import javax.servlet.http.HttpServletRequest

import java.text.SimpleDateFormat

import org.grails.groovy.grails.commons.*

import grails.web.servlet.mvc.GrailsParameterMap



import org.grails.web.util.WebUtils

import org.springframework.web.context.request.RequestContextHolder as RCH

import org.springframework.web.context.request.ServletRequestAttributes
import net.nosegrind.apiframework.*


class ApiLayerService{

	static transactional = false
	
	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService
	ApiCacheService apiCacheService

	boolean chain = true
	boolean batch = true
	boolean localauth = true
	
	ApiStatuses errors = new ApiStatuses()
	
	void setEnv(){
		this.batch = grailsApplication.config.apitoolkit.batching.enabled
		this.chain = grailsApplication.config.apitoolkit.chaining.enabled
		this.localauth = grailsApplication.config.apitoolkit.localauth.enabled
	}
	
	private HttpServletRequest getRequest(){
		//return RCH.currentRequestAttributes().currentRequest
		HttpServletRequest request = ((ServletRequestAttributes) RCH.currentRequestAttributes()).getRequest()
		return request
	}
	
	private HttpServletResponse getResponse(){
		HttpServletResponse response = ((ServletRequestAttributes) RCH.getRequestAttributes()).getAttribute(RESPONSE_NAME_AT_ATTRIBUTES, RequestAttributes.SCOPE_REQUEST)
		return response
	}

	/*
	GrailsParameterMap getParams(HttpServletRequest request,GrailsParameterMap params){
		try{
			String type = params.format
			request."${type}"?.each() { key,value ->
				params.put(key,value)
			}
			return params
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: getParams] : Exception - full stack trace follows:",e)
		}
	}
*/

	boolean checkAuth(HttpServletRequest request, List roles){
		try{
			boolean hasAuth = false
			roles.each{
				if(request.isUserInRole(it)){
					hasAuth = true
				}
			}
			return hasAuth
		}catch(Exception e) {
			throw new Exception("[ApiLayerService :: checkAuth] : Exception - full stack trace follows:",e)
		}
	}

	// set version,controller,action / controller,action
	List parseUri(String uri, String entrypoint){
		if(uri[0]=='/'){ uri=uri[1..-1] }
		List uriVars = uri.split('/')
		if(uriVars.size()==3){
			List temp2 = entrypoint.split('-')
			if(temp2.size()>1){
				// version
				uriVars[0] = temp2[1]
				return uriVars
			}else{
				uriVars.drop(1)
				return uriVars
			}
		}else{
			return uriVars
		}
	}

	/*
	void setApiCache(String controllername,LinkedHashMap apidoc){
		apiCacheService.setApiCache(controllername,apidoc)
		apidoc.each(){ k1,v1 ->
			if(k1!='currentStable'){
				v1.each() { k2,v2 ->
					if(!['deprecated','defaultAction','domainPackage'].contains(k2)){
						def doc = generateApiDoc(controllername, k2, k1)
						apiCacheService.setApiDocCache(controllername,k2,k1,doc)
					}
				}
			}
		}
		def cache = apiCacheService.getApiCache(controllername)
	}
	*/


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
			throw new Exception("[ApiLayerService :: checkDeprecationDate] : Exception - full stack trace follows:",e)
		}
	}
	

	
	/*
	private ArrayList processDocErrorCodes(HashSet error){
		try{
			def errors = error as List
			def err = []
			errors.each{ v ->
				def code = ['code':v.code,'description':"${v.description}"]
				err.add(code)
			}
			return err
		}catch(Exception e){
			throw new Exception("[ApiLayerService :: processDocErrorCodes] : Exception - full stack trace follows:",e)
		}
	}
	*/
	
	// api call now needs to detect request method and see if it matches anno request method
	boolean isApiCall(){
		try{
			HttpServletRequest request = getRequest()
			GrailsParameterMap params = RCH.currentRequestAttributes().params
			String uri = request.forwardURI.split('/')[1]
			String api
			if(params.apiObject){
				api = (apiName)?"v${params.apiVersion}-${params.apiObject}" as String:"v${params.apiVersion}-${params.apiObject}" as String
			}else{
				api = (apiName)?"v${params.apiVersion}" as String:"v${params.apiVersion}" as String
			}
			return uri==api
		}catch(Exception e){
			throw new Exception("[ApiLayerService :: isApiCall] : Exception - full stack trace follows:",e)
		}
	}
	
	protected void setParams(HttpServletRequest request,GrailsParameterMap params){
		try{
			String type = params.format
			request."$type"?.each() { key,value ->
				params.put(key,value)
			}
		}catch(Exception e){
			throw new Exception("[ApiLayerService :: setParams] : Exception - full stack trace follows:",e)
		}
	}
	
	/*
	 * Returns chainType
	 * 0 = blankchain
	 * 1 = prechain
	 * 2 = postchain
	 * 3 = illegal combination
	 */
	protected int checkChainedMethodPosition(LinkedHashMap cache,HttpServletRequest request, GrailsParameterMap params, List uri, Map path){
		println("#### checkChainedMethodPosition")
		println(cache)
		try{
			boolean preMatch = false
			boolean postMatch = false
			boolean pathMatch = false

			List keys = path.keySet() as List
			Integer pathSize = keys.size()

			String controller = uri[0]
			String action = uri[1]
			Long id = uri[2].toLong()

			// prematch check
			String currentMethod = Method["${request.method.toString()}"].toString()
			println("currentMethod:"+currentMethod)
			String methods = cache[params.apiObject][action]['method'].trim()
			println("methods:"+methods)
			
			if(currentMethod!=methods && methods=='GET'){
				if(['prechain','postchain'].contains(params?.apiChain?.type)){
					preMatch = true
				}
			}else{
				if(methods == currentMethod && params?.apiChain?.type=='blankchain'){
					preMatch = true
				}
			}

		
			// postmatch check
			if(pathSize>=1){
				println("pathSize>=1")
				String last=path[keys[pathSize-1]]
				if(last && (last!='return' || last!='null')){
					List last2 = keys[pathSize-1].split('/')

					println(last2)
					println(last2[0])
					cache = apiCacheService.getApiCache(last2[0])
					println(cache)
					methods = cache[params.apiObject][last2[1]]['method'].trim()
					println("methods2:"+methods)
					if(methods=='GET'){
						if(methods != currentMethod && params?.apiChain?.type=='postchain'){
							postMatch = true
						}
					}else{
						if(methods == currentMethod){
							postMatch = true
						}
					}
				}else{
					postMatch = true
				}
			}

			// path check
			int start = 1
			int end = pathSize-2
			println("${start} > ${end}")
			if(start<end){
				//println("${start} > ${end}")
				keys[0..(pathSize-1)].each{ val ->
					if(val){
						println("val : "+val)
						List temp2 = val.split('/')
						println(temp2)
						println(temp2[0])
						cache = apiCacheService.getApiCache(temp2[0])
						methods = cache[params.apiObject][temp2[1]]['method'].trim()

						if(methods=='GET'){
							if(methods == currentMethod && params?.apiChain?.type=='blankchain'){
								pathMatch = true
							}
						}else{
							if(methods == currentMethod){
								pathMatch = true
							}
						}
					}
				}
			}

			println("${pathMatch} / ${preMatch} / ${postMatch}")
			if(pathMatch || (preMatch && postMatch)){
				if(params?.apiChain?.type=='blankchain'){
					return 0
				}else{
					return 3
				}
			}else{
				if(preMatch){
					setParams(request,params)
					return 1
				}else if(postMatch){
					setParams(request,params)
					return 2
				}
			}

			if(params?.apiChain?.type=='blankchain'){
				return 0
			}else{
				return 3
			}


		}catch(Exception e){
			//throw new Exception("[ApiLayerService :: checkChainedMethodPosition] : Exception - full stack trace follows:",e)
			println("[ApiLayerService :: checkChainedMethodPosition] : Exception - full stack trace follows:"+e)
		}
	}

	boolean isRequestMatch(String protocol,String method){
		if(['TRACERT','OPTIONS','HEAD'].contains(method)){
			return true
		}else{
			if(protocol == method){
				return true
			}else{
				return false
			}
		}
		return false
	}
}
