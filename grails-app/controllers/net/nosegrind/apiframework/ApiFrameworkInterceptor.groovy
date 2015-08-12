package net.nosegrind.apiframework

import grails.config.Config
import grails.core.support.GrailsConfigurationAware

import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletOutputStream

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
//import grails.util.Holders as HOLDER

//import javax.servlet.ServletContext

//import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
//import org.codehaus.groovy.grails.commons.GrailsApplication
//import grails.core.GrailsApplication

class ApiFrameworkInterceptor implements GrailsConfigurationAware {
	
	//int order = HIGHEST_PRECEDENCE + 999
	
	//GrailsApplication grailsApplication
	@Autowired
	ApiRequestService apiRequestService
	@Autowired
	ApiResponseService apiResponseService
	@Autowired
	ApiDomainService apiDomainService
	@Autowired
	ApiCacheService apiCacheService
	
	String apiName
	String apiVersion
	String entryPoint

	void setConfiguration(Config cfg) {
		this.apiName = cfg.apitoolkit.apiName
		this.apiVersion = cfg.info.app.version
		
		String apinameEntrypoint = "${this.apiName}_v${this.apiVersion}"
		String versionEntrypoint = "v${this.apiVersion}"
		this.entryPoint = (this.apiName)?apinameEntrypoint:versionEntrypoint
		
		match(uri:/^(\/${entryPoint}\/*(.+))$/)
	}


	ApiFrameworkInterceptor() {}

	
	boolean before(){
		//println("##### FILTER (BEFORE)")
		
		/*
		 * FIRST DETERMINE
		 *  - HOW ENDPOINT IS BEING CALLED, THEN...
		 *  - WHAT RESOURCE IS BEING CALLED (CONTROLLER/SERVICE/DOMAIN/ETC)
		 *  - FINALLY, RESOLVE ENDPOINT
		 */
				
		def methods = ['get':'show','put':'update','post':'create','delete':'delete']
		try{
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){
				def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]
				if(cache){
					params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']
					if(!params.action){ 
						String methodAction = methods[request.method.toLowerCase()]
						if(!cache[params.apiObject][methodAction]){
							params.action = cache[params.apiObject]['defaultAction'].split('/')[1] 
						}else{
							params.action = methods[request.method.toLowerCase()]
							
							// FORWARD FOR REST DEFAULTS WITH NO ACTION
							def tempUri = request.getRequestURI().split("/")
							if(tempUri[2].contains('dispatch')){
								if("${params.controller}.dispatch" == tempUri[2]){
									if(!cache[params.apiObject]['domainPackage']){
										forward(controller:params.controller,action:params.action,params:params)
										return false
									}
								}
							}
						}
					}
							
					// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
					boolean result = apiRequestService.handleApiRequest(cache,request,params,entryPoint)
					
					
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
									model = [:]
									break
							}


							if(!model && request.method.toLowerCase()!='delete'){
								render(status:HttpServletResponse.SC_BAD_REQUEST)
								return false
							}
							
							if(params?.apiCombine==true){
								model = params.apiCombine
							}
							def newModel = apiResponseService.formatDomainObject(model)
							
							LinkedHashMap content
							if(apiRequestService.chain && params?.apiChain?.order){
								boolean result2 = apiResponseService.handleApiChain(cache, request,response ,newModel,params)
								forward(controller:params.controller,action:params.action,id:params.id)
								return false
							}else if(apiRequestService.batch && params?.apiBatch){
								forward(controller:params.controller,action:params.action,params:params)
								return false
							}else{
								content = apiResponseService.handleApiResponse(cache,request,response,newModel,params)
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
			
	// model is automapped??
    @Override
	boolean after(){
		println("##### FILTER (AFTER)")
        println(model.getClass())
		try{
			if(!model){
				render(status:HttpServletResponse.SC_BAD_REQUEST)
				return false
			}

			if(params?.apiCombine==true){
				model = params.apiCombine
			}

			def newModel = (model)?apiResponseService.convertModel(model):model

			def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]

			LinkedHashMap content
			if(apiResponseService.chain && params?.apiChain?.order){
				boolean result = apiResponseService.handleApiChain(cache, request,response,newModel,params)
				forward(controller:params.controller,action:params.action,id:params.id)
				return false
			}else if(apiResponseService.batch && params?.apiBatch){
				forward(controller:params.controller,action:params.action,params:params)
				return false
			}else{
				content = apiResponseService.handleApiResponse(cache,request,response,newModel,params)
			}
				
			if(content){
				//render(text:content.apiToolkitContent, contentType:"${content.apiToolkitType}", encoding:content.apiToolkitEncoding)

                java.io.PrintWriter writer = response.getWriter()
                response.setContentType("text/json");
                try {
                    writer.print(content.apiToolkitContent)
                    writer.close()
                }catch(java.io.IOException e){
                    println("[ApiToolkitFilters :: apitoolkit.after] : PrintWriter Exception - full stack trace follows:"+e);
                }

                return false
			}
			return false
	   }catch(Exception e){
		   log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
		   return false
	   }
	   return false
	}
}

