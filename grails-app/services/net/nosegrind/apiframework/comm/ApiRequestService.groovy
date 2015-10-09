package net.nosegrind.apiframework.comm

import net.nosegrind.apiframework.comm.ApiLayerService
import net.nosegrind.apiframework.ParamsService

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import javax.servlet.forward.*

import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.*

class ApiRequestService extends ApiLayerService{

	ParamsService paramsService

	static transactional = false
	
	boolean handleApiRequest(LinkedHashMap cache, HttpServletRequest request, GrailsParameterMap params){
		try{
			setEnv()
			ApiStatuses error = new ApiStatuses()

			// CHECK IF URI HAS CACHE
			if(cache[params.apiObject][params.action]){
				// CHECK ACCESS TO METHOD
				List roles = cache[params.apiObject][params.action]['roles']?.toList()

				// if(!checkAuth(request,roles)){ return false }

				// CHECK VERSION DEPRECATION DATE
				if(cache[params.apiObject][params.action]['deprecated']?.get(0)){
					if(checkDeprecationDate(cache[params.apiObject][params.action]['deprecated'][0])){
						String depMsg = cache[params.apiObject][params.action]['deprecated'][1]
						// replace msg with config deprecation message
						String msg = "[ERROR] ${depMsg}"
						error._400_BAD_REQUEST(msg)?.send()
						return false
					}
				}

				def method = cache[params.apiObject][params.action]['method']?.trim()

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
