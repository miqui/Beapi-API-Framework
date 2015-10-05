package net.nosegrind.apiframework

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import net.nosegrind.apiframework.comm.ChainRequestService
import net.nosegrind.apiframework.comm.ChainResponseService
import org.springframework.beans.factory.annotation.Autowired
import grails.util.Metadata
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest

import static grails.artefact.controller.support.RequestForwarder$Trait$Helper.forward

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


//class ChainInterceptor implements GrailsConfigurationAware{
class ChainInterceptor{

    int order = HIGHEST_PRECEDENCE + 997
	
	//def grailsApplication

	@Autowired
	ChainRequestService apiChainRequestService
    @Autowired
	ChainResponseService apiChainResponseService
    @Autowired
	ApiDomainService apiDomainService
    @Autowired
	ApiCacheService apiCacheService

    String entryPoint

	ChainInterceptor(){
		String apiVersion = Metadata.current.getApplicationVersion()
		this.entryPoint = "c${apiVersion}"

		match(uri:"/${this.entryPoint}/**")
	}

	boolean before(){
		println("##### CHAININTERCEPTOR (BEFORE)")
println(request.forwardURI)
		println(request.getParameterMap())
		params.format = request.format.toUpperCase()


		println("params:"+params)
def json = request."${params.format}"
		println("################### JSON : " + json)


		def methods = ['get':'show','put':'update','post':'create','delete':'delete']
		try{
			println("trying... ${params.controller}/${params.action}")
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){
				def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]
				if(cache){
					println("has cache...")
					params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']
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

					println("params before handlApiRequest : "+params)
					// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
					boolean result = apiChainRequestService.handleApiRequest(cache,request,params,this.entryPoint)
					println("after handleApiRequest...")

					json = request."${params.format}"
					println("################### JSON : " + json)

					println("params:"+params)
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
							def newModel = apiChainResponseService.formatDomainObject(model)
							
							LinkedHashMap content
							if(apiChainRequestService.chain && params?.apiChain?.order){
								boolean result2 = apiChainResponseService.handleApiChain(cache, request,response ,newModel,params)
								println("result2 : "+result2)
								forward([controller:params.controller,action:params.action,id:params.id] as Map)
								return false
							}else{
								content = apiChainResponseService.handleApiResponse(cache,request,response,newModel,params)
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
						println("result : "+result)
						return result
					}
				}
			//}
			
			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e);
			println("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:"+ e);
			return false
		}
	}
			
	// model is automapped??

	boolean after(){
		println("##### CHAININTERCEPTOR (AFTER)")

		println(request.forwardURI)
		//try{
			println("trying...")
			if(!model){
				println("NO MODEL")
				//render(status:HttpServletResponse.SC_BAD_REQUEST)
				return false
			}

			if(params?.apiCombine==true){
				model = params.apiCombine
			}

			def newModel = (model)?apiChainResponseService.convertModel(model):model
			def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]

			LinkedHashMap content

			println("format :"+params.format)
			println("chain : "+params.apiChain)
			println("order : "+params.apiChain.order)

			if(apiChainResponseService.chain && params?.apiChain?.order){
				boolean result = apiChainResponseService.handleApiChain(cache, request,response,newModel,params)
				String uri = "/${entryPoint}/${params.controller}/${params.action}/${params.id}"
				println("uri:"+uri)

				println("about to forward...")

				forward(URI:uri,params:[apiObject:params.apiObject,apiChain:params.apiChain])
				return false
			}

            content = apiChainResponseService.handleApiResponse(cache,request,response,newModel,params)
				
			if(content){
                render(text:content.apiToolkitContent, contentType:"${content.apiToolkitType}", encoding:content.apiToolkitEncoding)
                return false
			}

			return false
		/*
	   }catch(Exception e){
			println("#### catching error...")
		   //log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
		   println("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:"+e)
			return false
	   }
	   */

	}
}

