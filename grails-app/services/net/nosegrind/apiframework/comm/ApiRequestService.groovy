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
import groovy.transform.CompileStatic

@CompileStatic
class ApiRequestService extends ApiLayer{

	static transactional = false
	
	boolean handleApiRequest(Object cache, HttpServletRequest request, GrailsParameterMap params){
		//println("#### [ApiRequestService : handleApiRequest ] ####")
		try{

			// CHECK IF URI HAS CACHE
			if(cache){
				// CHECK ACCESS TO METHOD
				/*
				List roles = cache['roles'] as List
				if(!checkAuth(request,roles)){
					println("no auth")
					return false
				}
				*/

				// CHECK VERSION DEPRECATION DATE
				List deprecated = cache['deprecated'] as List
				if(deprecated?.get(0)){
					if(checkDeprecationDate(deprecated[0].toString())){
						String depMsg = deprecated[1].toString()
						// replace msg with config deprecation message
						String msg = "[ERROR] "+depMsg
						ApiStatuses._400_BAD_REQUEST(msg)?.send()
						return false
					}
				}

				def method = cache['method']?.toString().trim()

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
