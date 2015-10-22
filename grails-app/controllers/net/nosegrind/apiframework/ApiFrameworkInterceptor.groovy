package net.nosegrind.apiframework


import grails.core.GrailsApplication
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper
import net.nosegrind.apiframework.comm.ApiRequestService
import net.nosegrind.apiframework.comm.ApiResponseService
import grails.util.Metadata
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import net.nosegrind.apiframework.Timer

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


class ApiFrameworkInterceptor extends Params{

	int order = HIGHEST_PRECEDENCE + 999

	GrailsApplication grailsApplication
	ApiRequestService apiRequestService
	ApiResponseService apiResponseService
	ApiCacheService apiCacheService
	String entryPoint

	ApiFrameworkInterceptor(){
		String apiVersion = Metadata.current.getApplicationVersion()
		entryPoint = "v${apiVersion}"
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		//println("##### FILTER (BEFORE)")

		Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']

		initParams()

		try{
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){

			LinkedHashMap cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]

			if(cache){
				params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']

				if(!checkURIDefinitions(cache[params.apiObject][params.action]['receives'])){
					// return bad status
					HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getAttribute(RESPONSE_NAME_AT_ATTRIBUTES, RequestAttributes.SCOPE_REQUEST)
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected request variables do not match sent variables")
					return false
				}

				if(!params.action){
					String methodAction = methods[request.method]
					if(!cache[params.apiObject][methodAction]){
						params.action = cache[params.apiObject]['defaultAction']
					}else{
						params.action = methods[request.method]

						// FORWARD FOR REST DEFAULTS WITH NO ACTION
						List tempUri = request.getRequestURI().split("/")
						if(tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']){
							forward(controller:params.controller,action:params.action,params:params)
							return false
						}
					}
				}

				// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
				boolean result = apiRequestService.handleApiRequest(cache,request,params)
				return result
			}
			//}

			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e);
			return false
		}
	}

	boolean after(){
		//println("##### FILTER (AFTER)")
		try{
			LinkedHashMap newModel = [:]

			if (!model) {
				render(status: HttpServletResponse.SC_BAD_REQUEST)
				return false
			} else {
				newModel = apiResponseService.convertModel(model)
			}


			LinkedHashMap content = apiResponseService.handleApiResponse(request,response,newModel,params)

			if(content){
				render(text:content.apiToolkitContent, contentType:"${content.apiToolkitType}", encoding:content.apiToolkitEncoding)
				return false
			}

			return false
		}catch(Exception e){
			log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
			return false
		}

	}

}
