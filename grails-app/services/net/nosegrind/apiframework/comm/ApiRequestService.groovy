package net.nosegrind.apiframework.comm

import net.nosegrind.apiframework.comm.ApiLayerService

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import javax.servlet.forward.*

import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import javax.servlet.http.HttpServletRequest

class ApiRequestService extends ApiLayerService{

	static transactional = false
	
	boolean handleApiRequest(LinkedHashMap cache, HttpServletRequest request, GrailsParameterMap params, String entryPoint){
		try{
			setEnv()
			
			ApiStatuses error = new ApiStatuses()
			setApiParams(request, params)
			// CHECK IF URI HAS CACHE
			if(cache[params.apiObject][params.action]){
				// CHECK ACCESS TO METHOD
				List roles = cache[params.apiObject][params.action]['roles']?.toList()
				
				/*
				if(!checkAuth(request,roles)){
					return false
				}
				*/

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

				if(!checkURIDefinitions(request,cache[params.apiObject][params.action]['receives'])){
					// return bad status
					String msg = 'Expected request variables do not match sent variables'
					error._400_BAD_REQUEST(msg)?.send()
					return false
				}else{
					return true
				}

			}
			return false
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:",e)
		}
	}
	
	protected void setApiParams(HttpServletRequest request, GrailsParameterMap params){
		try{
            String contentType = params.format

            if(request?."${contentType}"){
                request?."${contentType}".each{ k,v ->
                        params[k]=v
                }
            }
			
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: setApiParams] : Exception - full stack trace follows:"+ e);
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
