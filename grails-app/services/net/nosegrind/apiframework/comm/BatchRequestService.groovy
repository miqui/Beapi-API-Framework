package net.nosegrind.apiframework.comm

import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/
import org.grails.groovy.grails.commons.*

import javax.servlet.forward.*
import javax.servlet.http.HttpServletRequest

//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import javax.servlet.http.HttpServletResponse

@CompileStatic
class BatchRequestService extends ApiLayer{

	static transactional = false


	boolean handleApiRequest(Object cache, HttpServletRequest request, HttpServletResponse response, GrailsParameterMap params){
		println("#### [ApiRequestService : handleApiRequest ] ####")
		try{

			// CHECK IF URI HAS CACHE
			if(cache){
				// CHECK ACCESS TO METHOD
				List roles = cache['roles'] as List
				if(!checkAuth(request,roles)){
					println("no roles")
					response.status = 401
					response.setHeader('ERROR','Unauthorized Access attempted')
					return false
				}

				// CHECK VERSION DEPRECATION DATE
				List deprecated = cache['deprecated'] as List
				if(deprecated?.get(0)){
					if(checkDeprecationDate(deprecated[0].toString())){
						println("is deprecated")
						String depMsg = deprecated[1].toString()
						response.status = 400
						response.setHeader('ERROR',depMsg)
						return false
					}
				}

				def method = cache['method']?.toString().trim()

				// DOES api.methods.contains(request.method)
				if(!isRequestMatch(method,request.method.toString())){
					response.status = 400
					response.setHeader('ERROR',"Request method doesn't match expected method.")
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
