/*
 * The MIT License (MIT)
 * Copyright 2014 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright/trademark notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.nosegrind.apiframework

import grails.converters.JSON
import javax.annotation.Resource
import grails.core.GrailsApplication
//import net.nosegrind.apiframework.ApiDescriptor
import grails.plugin.springsecurity.SpringSecurityService
import groovy.json.JsonSlurper
import groovy.util.XmlSlurper;
import grails.util.Metadata

//import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.transform.CompileStatic


@CompileStatic
class TraceInterceptor extends TraceCommLayer{

	int order = HIGHEST_PRECEDENCE + 997

	@Resource
	GrailsApplication grailsApplication

	//ApiRequestService apiRequestService
	//ApiResponseService apiResponseService
	ApiCacheService apiCacheService
	TraceService traceService
	SpringSecurityService springSecurityService

	// TODO: detect and assign apiObjectVersion from uri
	String entryPoint = "t${Metadata.current.getProperty(Metadata.APPLICATION_VERSION, String.class)}"

	TraceInterceptor(){
		// TODO: detect and assign apiObjectVersion from uri
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		//log.info('##### FILTER (BEFORE)')
		traceService.startTrace('TraceInterceptor','before')

		String format = (request?.format)?request.format:'JSON';
		Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']
		boolean restAlt = (['OPTIONS','TRACE','HEAD'].contains(request.method))?true:false

		// Init params
		if (['XML', 'JSON'].contains(format)) {
			LinkedHashMap dataParams = [:]
			switch (format) {
				case 'XML':
					String xml = request.XML.toString()
					if(xml!='[:]') {
						def slurper = new XmlSlurper()
						slurper.parseText(xml).each() { k, v ->
							dataParams[k] = v
						}
						request.setAttribute('XML', dataParams)
					}
					break
				case 'JSON':
					String json = request.JSON.toString()
					if(json!='[:]') {
						def slurper = new JsonSlurper()
						slurper.parseText(json).each() { k, v ->
							dataParams[k] = v
						}
						request.setAttribute('JSON', dataParams)
					}
					break
			}
		}


		try{
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){

			LinkedHashMap cache = (params.controller)? apiCacheService.getApiCache(params.controller.toString()):[:]


			if(params.controller=='apidoc'){
				if(cache){
					params.apiObject = (params.apiObjectVersion) ? params.apiObjectVersion : cache['currentStable']['value']
					params.action = (params.action == null) ? cache[params.apiObject]['defaultAction'] : params.action
					traceService.endTrace('TraceInterceptor','before')
					return true
				}
				// TODO : return false and render message/error code ???
			}else{
				if(cache) {
					params.apiObject = (params.apiObjectVersion) ? params.apiObjectVersion : cache['currentStable']['value']
					params.action = (params.action == null) ? cache[params.apiObject]['defaultAction'] : params.action

					String expectedMethod = cache[params.apiObject][params.action.toString()]['method'] as String
					if(!checkRequestMethod(expectedMethod,restAlt)) {
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: "Expected request method '${expectedMethod}' does not match sent method '${request.method}'")
						traceService.endTrace('TraceInterceptor','before')
						return false
					}

					// Check for REST alternatives
					if (!restAlt) {

						// Check that sent request params match expected endpoint params for principal ROLE
						LinkedHashMap receives = cache[params.apiObject][params.action.toString()]['receives'] as LinkedHashMap
						boolean requestKeysMatch = checkURIDefinitions(params, receives)

						if (!requestKeysMatch) {
							render(status: HttpServletResponse.SC_BAD_REQUEST, text: 'Expected request variables do not match sent variables')
							traceService.endTrace('TraceInterceptor','before')
							return false
						}
					}else{
						LinkedHashMap result = parseRequestMethod(request, params)
						if(result){
							render(text:result.apiToolkitContent, contentType:"${result.apiToolkitType}", encoding:result.apiToolkitEncoding)
						}
						traceService.endTrace('TraceInterceptor','before')
						return false
					}

					if(params.action==null || !params.action){
						String methodAction = methods[request.method]
						if(!cache[(String)params.apiObject][methodAction]){
							params.action = cache[(String)params.apiObject]['defaultAction']
						}else{
							params.action = methods[request.method]

							// FORWARD FOR REST DEFAULTS WITH NO ACTION
							String[] tempUri = request.getRequestURI().split("/")
							if(tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']){
								forward(controller:params.controller,action:params.action)
								traceService.endTrace('TraceInterceptor','before')
								return false
							}
						}
					}

					// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
					ApiDescriptor cachedEndpoint = cache[(String)params.apiObject][(String)params.action] as ApiDescriptor
					boolean result = handleApiRequest(cachedEndpoint, request, response, params)
					traceService.endTrace('TraceInterceptor','before')
					return result
				}
			}
			// no cache found
			traceService.endTrace('TraceInterceptor','before')
			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			traceService.endTrace('TraceInterceptor','before')
			return false
		}
	}

	boolean after(){
		//log.info('##### FILTER (AFTER)')
		traceService.startTrace('TraceInterceptor','after')
		try{
			LinkedHashMap newModel = [:]

			if(params.controller!='apidoc') {
				if (!model) {
					render(status: HttpServletResponse.SC_NOT_FOUND, text: 'No resource returned / domain is empty')
					traceService.endTrace('TraceInterceptor','after')
					return false
				} else {
					newModel = convertModel(model)
				}
			}else{
				newModel = model as LinkedHashMap
			}


			LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			ApiDescriptor cachedEndpoint = cache[params.apiObject][(String)params.action] as ApiDescriptor
			String content = handleApiResponse(cachedEndpoint['returns'] as LinkedHashMap,cachedEndpoint['roles'] as List,request,response,newModel,params) as LinkedHashMap

			if(content){
				traceService.endTrace('TraceInterceptor','after')
				LinkedHashMap traceContent = traceService.endAndReturnTrace('TraceInterceptor','after');
				String tcontent = traceContent as JSON
				render(text:tcontent, contentType:request.contentType)

				return false
			}

			return false
		}catch(Exception e){
			log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
			traceService.endTrace('TraceInterceptor','after')
			return false
		}
	}

}
