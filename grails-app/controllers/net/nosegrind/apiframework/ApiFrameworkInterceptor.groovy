package net.nosegrind.apiframework


import grails.core.GrailsApplication

import grails.plugin.springsecurity.SpringSecurityService
import net.nosegrind.apiframework.comm.ApiRequestService
import net.nosegrind.apiframework.comm.ApiResponseService
import grails.util.Metadata
import javax.servlet.http.HttpServletResponse

import groovy.transform.CompileStatic
//import net.nosegrind.apiframework.Timer

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/

//@CompileStatic
class ApiFrameworkInterceptor extends Params{

	int order = HIGHEST_PRECEDENCE + 999

	GrailsApplication grailsApplication
	ApiRequestService apiRequestService
	ApiResponseService apiResponseService
	ApiCacheService apiCacheService
	SpringSecurityService springSecurityService

	String entryPoint

	ApiFrameworkInterceptor(){
		String apiVersion = Metadata.current.getApplicationVersion()
		entryPoint = "v${apiVersion}"
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		//println("##### FILTER (BEFORE)")

		Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']

/*
		if (springSecurityService.loggedIn) {
			def principal = springSecurityService.principal
			println("User is logged in")
			List roleNames = principal.authorities*.authority
			println roleNames
		}else{
			println("User NOT LOGGED IN")
		}
*/

		initParams()

		try{

			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){

				LinkedHashMap cache = [:]
				if(params.controller){
					cache = apiCacheService.getApiCache(params.controller.toString())
				}


				if(cache){
					params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']
					LinkedHashMap receives = cache[params.apiObject.toString()][params.action.toString()]['receives'] as LinkedHashMap
					boolean requestKeysMatch = checkURIDefinitions(params,receives)

					if(!requestKeysMatch){
						// return bad status
						response.status = 400
						response.setHeader('ERROR','Expected request variables do not match sent variables')
						return false
					}

					if(!params.action){
						String methodAction = methods[request.method]
						if(!cache[params.apiObject][methodAction]){
							params.action = cache[params.apiObject]['defaultAction']
						}else{
							params.action = methods[request.method]

							// FORWARD FOR REST DEFAULTS WITH NO ACTION
							String[] tempUri = request.getRequestURI().split("/")
							if(tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']){
								forward(controller:params.controller,action:params.action,params:params)
								return false
							}
						}
					}

					// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
					boolean result = apiRequestService.handleApiRequest(cache[params.apiObject.toString()][params.action.toString()], request, response, params)
					return result
				}
			//}
			return false

		}catch(Exception e){
			//log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			println("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:"+e)
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

			LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			LinkedHashMap content = apiResponseService.handleApiResponse(cache[params.apiObject.toString()][params.action.toString()],request,response,newModel,params) as LinkedHashMap

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
