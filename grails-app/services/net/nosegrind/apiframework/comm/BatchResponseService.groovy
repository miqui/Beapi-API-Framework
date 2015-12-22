package net.nosegrind.apiframework.comm

import grails.core.GrailsApplication

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/



//import grails.application.springsecurity.SpringSecurityService
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import net.nosegrind.apiframework.ApiCacheService
import org.grails.groovy.grails.commons.*

import javax.servlet.forward.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

//import org.grails.web.sitemesh.GrailsContentBufferingResponse
@CompileStatic
class BatchResponseService extends ApiLayer{

	GrailsApplication grailsApplication
	ApiCacheService apiCacheService

	def handleApiResponse(Object cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		//println("#### [ApiResponseService : handleApiResponse ] ####")

		try{
			if(cache){
				// make 'application/json' default

				if(params.contentType){
					response.setHeader('Authorization', cache['roles'].toString().join(', '))
					List responseList = getApiParams((LinkedHashMap)cache['returns'])
					LinkedHashMap result = parseURIDefinitions(model,responseList)
					//if(params?.apiBatch.combine=='true'){
					//	params.apiCombine["${params.uri}"] = result
					//}
					if(!result){
						response.status = 400
					}else{
						LinkedHashMap content = parseResponseMethod(request, params, result)
						return content
					}
				}else{
					response.status = 400
				}
			}else{
				//return true
				//render(view:params.action,model:model)
			}
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
		}

	}
}