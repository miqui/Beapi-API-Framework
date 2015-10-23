package net.nosegrind.apiframework.comm

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/



//import grails.plugin.springsecurity.SpringSecurityService

import grails.web.http.HttpHeaders
import net.nosegrind.apiframework.comm.ApiLayer

import javax.servlet.forward.*
import org.springframework.http.ResponseEntity

import grails.web.servlet.mvc.GrailsParameterMap

import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.grails.web.util.GrailsApplicationAttributes
//import org.grails.web.sitemesh.GrailsContentBufferingResponse

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.ApiDescriptor
import grails.core.GrailsApplication
import net.nosegrind.apiframework.*
import net.nosegrind.apiframework.Timer

class ApiResponseService extends ApiLayer{

	GrailsApplication grailsApplication
	ApiCacheService apiCacheService

	def handleApiResponse(Object cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		//println("#### [ApiResponseService : handleApiResponse ] ####")

		try{
			if(cache){
				// make 'application/json' default

				if(params.contentType){
					response.setHeader('Authorization', cache['roles'].join(', '))
					List responseList = getApiParams(request,cache['returns'])
					LinkedHashMap result = parseURIDefinitions(model,responseList)
					LinkedHashMap content = parseResponseMethod(request, params, result)
					return content
				}
			}else{
				//return true
				//render(view:params.action,model:model)
			}
		}catch(Exception e){
			//throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
			println("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:"+e)
		}

	}
}