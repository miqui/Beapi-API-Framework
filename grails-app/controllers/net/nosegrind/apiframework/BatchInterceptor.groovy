package net.nosegrind.apiframework

import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.util.Metadata
import net.nosegrind.apiframework.comm.BatchRequestService
import net.nosegrind.apiframework.comm.BatchResponseService

import javax.servlet.http.HttpServletResponse

//import net.nosegrind.apiframework.Timer

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/

//@CompileStatic
class BatchInterceptor extends Params{

	int order = HIGHEST_PRECEDENCE + 999

	GrailsApplication grailsApplication
	BatchRequestService batchRequestService
	BatchResponseService batchResponseService
	ApiCacheService apiCacheService
	SpringSecurityService springSecurityService

	String entryPoint

	BatchInterceptor(){
		String apiVersion = Metadata.current.getApplicationVersion()
		entryPoint = "b${apiVersion}"
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		println("##### BATCHINTERCEPTOR (BEFORE)")

		Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']

		initParams('batch')


		try{

			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){

				LinkedHashMap cache = [:]
				if(params.controller){
					cache = apiCacheService.getApiCache(params.controller.toString())
				}


				if(cache){
					params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']
					LinkedHashMap receives = cache[params.apiObject.toString()][params.action.toString()]['receives'] as LinkedHashMap
					boolean requestKeysMatch = checkURIDefinitions(cache[params.apiObject.toString()][params.action.toString()]['method'] as String,params,receives)

					if(!requestKeysMatch){
						render(status:HttpServletResponse.SC_BAD_REQUEST, text: 'Expected request variables do not match sent variables')
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
					boolean result = batchRequestService.handleApiRequest(cache[params.apiObject.toString()][params.action.toString()], request, response, params)
					return result
				}
			//}
			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			return false
		}
	}

	boolean after(){
		println("##### FILTER (AFTER)")
		try{
			LinkedHashMap newModel = [:]

			if (!model) {
				render(status:HttpServletResponse.SC_NOT_FOUND , text: 'No resource returned')
				return false
			} else {
				newModel = batchResponseService.convertModel(model)
			}

			println("newmodel: "+newModel)

			LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			LinkedHashMap content
			if(batchEnabled && params?.apiBatch){
				forward(controller:params.controller, action:params.action,params:params)
				return false
			}

			if(batchEnabled && params?.apiBatch){
				forward(controller:params.controller, action:params.action,params:params)
				return false
			}

			content = batchResponseService.handleApiResponse(cache[params.apiObject.toString()][params.action.toString()],request,response,newModel,params) as LinkedHashMap

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
