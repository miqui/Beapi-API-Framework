package net.nosegrind.apiframework.comm




/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/

import net.nosegrind.apiframework.ApiDescriptor
import javax.servlet.forward.*
import net.nosegrind.apiframework.comm.ApiLayer
import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.*
//import groovy.transform.CompileStatic

//@CompileStatic
class ApiRequestService extends ApiLayer{

	static transactional = false
	
	boolean handleApiRequest(Object cache, HttpServletRequest request, GrailsParameterMap params){
		//println("#### [ApiRequestService : handleApiRequest ] ####")
		try{
			//setEnv()
			ApiStatuses error = new ApiStatuses()

			// CHECK IF URI HAS CACHE
			if(cache){
				// CHECK ACCESS TO METHOD
				List roles = cache['roles']?.toList()

				// if(!checkAuth(request,roles)){ return false }

				// CHECK VERSION DEPRECATION DATE
				if(cache['deprecated']?.get(0)){
					if(checkDeprecationDate(cache['deprecated'][0])){
						String depMsg = cache['deprecated'][1]
						// replace msg with config deprecation message
						String msg = "[ERROR] ${depMsg}"
						error._400_BAD_REQUEST(msg)?.send()
						return false
					}
				}

				def method = cache['method']?.trim()

				// DOES api.methods.contains(request.method)
				if(!isRequestMatch(method,request.method.toString())){
					return false
				}
				return true
			}
			return false
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:",e)
		}
	}
}
