package net.nosegrind.apiframework

import grails.web.servlet.mvc.GrailsParameterMap
import net.nosegrind.apiframework.comm.BatchRequestService
import net.nosegrind.apiframework.comm.BatchResponseService
import grails.util.Metadata

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


class BatchInterceptor extends Params{

	int order = HIGHEST_PRECEDENCE + 998

	BatchRequestService batchRequestService
	BatchResponseService batchResponseService
	ApiDomainService apiDomainService
	ApiCacheService apiCacheService


	String entryPoint

	BatchInterceptor(){
		String apiVersion = Metadata.current.getApplicationVersion()
		entryPoint = "b${apiVersion}"
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		println("##### BATCHINTERCEPTOR (BEFORE)")

		Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']
		
		paramsService.initParams(request)
		paramsService.setBatchParams(request)

		println(params)

		try{
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){
			def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]
			if(cache){

				params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']

				println("################## before checkURIDefinitions: ")
				if(!paramsService.checkURIDefinitions(request,cache[params.apiObject][params.action]['receives'])){
					//return bad status
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected request variables do not match sent variables")
					return false
				}
				println("################## after checkURIDefinitions: ")

				if(!params.action){
					String methodAction = methods[request.method.toLowerCase()]
					if(!cache[params.apiObject][methodAction]){
						params.action = cache[params.apiObject]['defaultAction']
					}else{
						params.action = methods[request.method.toLowerCase()]

						// FORWARD FOR REST DEFAULTS WITH NO ACTION
						def tempUri = request.getRequestURI().split("/")
						if(tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']){
							forward(controller:params.controller,action:params.action,params:params)
							return false
						}
					}
				}

				// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
				boolean result = batchRequestService.handleApiRequest(cache,request,params,this.entryPoint)

				//HANDLE DOMAIN RESOLUTION
				if(cache[params.apiObject]['domainPackage']){
					// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
					if(result){
						def model
						switch(methods[request.method.toLowerCase()]){
							case 'show':
								model = apiDomainService.showInstance(cache,params)
								break
							case 'update':
								model = apiDomainService.updateInstance(cache,params)
								break
							case 'create':
								model = apiDomainService.createInstance(cache,params)
								break
							case 'delete':
								model = apiDomainService.deleteInstance(cache,params)
								if(!model) {
									model = [:]
								}
								break
						}

						if(!model && request.method.toLowerCase()!='delete'){
							render(status:HttpServletResponse.SC_BAD_REQUEST)
							return false
						}

						if(params?.apiCombine==true){
							model = params.apiCombine
						}
						def newModel = batchResponseService.formatDomainObject(model)

						LinkedHashMap content
						if(batchRequestService.batch && params?.apiBatch){
							forward(controller:params.controller,action:params.action,params:params)
							return false
						}else{
							content = batchResponseService.handleApiResponse(cache,request,response,newModel,params)
						}

						if(request.method.toLowerCase()=='delete' && content.apiToolkitContent==null){
							render(status:HttpServletResponse.SC_OK)
							return false
						}else{
							render(text:content.apiToolkitContent, contentType:"${content.apiToolkitType}", encoding:content.apiToolkitEncoding)
							return false
						}
					}
					//return result
				}else{
					return result
				}
			}
			//}

			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e);
			return false
		}
	}

	boolean after(){
		println("##### BATCHINTERCEPTOR (AFTER)")
		try {
			Map newModel = [:]

			if (!model) {
				render(status: HttpServletResponse.SC_BAD_REQUEST)
				return false
			} else {
				newModel = batchResponseService.convertModel(model)
			}

			LinkedHashMap cache = apiCacheService.getApiCache(params?.controller)
			LinkedHashMap content = batchResponseService.handleApiResponse(cache,request,response,newModel,params)
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

