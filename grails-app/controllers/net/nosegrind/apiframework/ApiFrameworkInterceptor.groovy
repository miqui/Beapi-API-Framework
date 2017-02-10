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

import javax.annotation.Resource
import grails.core.GrailsApplication
//import net.nosegrind.apiframework.ApiDescriptor
import grails.plugin.springsecurity.SpringSecurityService

//import net.nosegrind.apiframework.RequestMethod
import groovy.json.JsonSlurper
import grails.util.Metadata
//import groovy.json.internal.LazyMap
//import grails.converters.JSON
//import grails.converters.XML
import org.grails.web.json.JSONObject

//import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.transform.CompileStatic


@CompileStatic
class ApiFrameworkInterceptor extends ApiCommLayer{

	int order = HIGHEST_PRECEDENCE + 999

	@Resource
	GrailsApplication grailsApplication

	ApiCacheService apiCacheService
	SpringSecurityService springSecurityService

	// TODO: detect and assign apiObjectVersion from uri
	String entryPoint = "v${Metadata.current.getProperty(Metadata.APPLICATION_VERSION, String.class)}"
	String format
	String mthdKey
	RequestMethod mthd
	LinkedHashMap cache = [:]

	ApiFrameworkInterceptor(){
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		//println('##### FILTER (BEFORE)')
		
		// TESTING: SHOW ALL FILTERS IN CHAIN
		//def filterChain = grailsApplication.mainContext.getBean('springSecurityFilterChain')
		//println(filterChain)

		format = (request?.format)?request.format:'JSON';
		mthdKey = request.method.toUpperCase()
		mthd = (RequestMethod) RequestMethod[mthdKey]

		//Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']
		boolean restAlt = RequestMethod.isRestAlt(mthd.getKey())

		// TODO: Check if user in USER roles and if this request puts user over 'rateLimit'


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
				default:
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
			// INITIALIZE CACHE
			cache = (params.controller)? apiCacheService.getApiCache(params.controller.toString()) as LinkedHashMap:[:]

			// IS APIDOC??
			if(params.controller=='apidoc'){
				if(cache){
					params.apiObject = (params.apiObjectVersion) ? params.apiObjectVersion : cache['currentStable']['value']
					params.action = (params.action == null) ? cache[params.apiObject]['defaultAction'] : params.action
					return true
				}
				return false
			}else{
				if(cache) {
					params.apiObject = (params.apiObjectVersion) ? params.apiObjectVersion : cache['currentStable']['value']
					params.action = (params.action == null) ? cache[params.apiObject]['defaultAction'] : params.action

					// CHECK REQUEST METHOD FOR ENDPOINT
					// NOTE: expectedMethod must be capitolized in IO State file
					String expectedMethod = cache[params.apiObject][params.action.toString()]['method'] as String
					if (!checkRequestMethod(mthd,expectedMethod, restAlt)) {
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: "Expected request method '${expectedMethod}' does not match sent method '${mthd.getKey()}'")
						return false
					}

					// CHECK FOR REST ALTERNATIVES
					if (restAlt) {
						// PARSE REST ALTS (TRACE, OPTIONS, ETC)
						String result = parseRequestMethod(mthd, params)
						if (result) {
							render(text: result, contentType: request.contentType)
							return false
						}
					}

					// CHECK REQUEST VARIABLES MATCH ENDPOINTS EXPECTED VARIABLES
					LinkedHashMap receives = cache[params.apiObject][params.action.toString()]['receives'] as LinkedHashMap
					//boolean requestKeysMatch = checkURIDefinitions(params, receives)
					if (!checkURIDefinitions(params, receives)) {
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: 'Expected request variables do not match sent variables')
						return false
					}

					// RETRIEVE CACHED RESULT
					if (cache[params.apiObject][params.action.toString()]['cachedResult']) {
						String authority = getUserRole() as String
						String domain = ((String) params.controller).capitalize()

						JSONObject json = (JSONObject) cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()]
						if(!json){
							return false
						}else{
							if (isCachedResult((Integer) json.get('version'), domain)) {
								def result = cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()]
								render(text: result, contentType: request.contentType)
								return false
							}
						}
					} else {
						if (params.action == null || !params.action) {
							String methodAction = mthd.toString()
							if (!cache[(String) params.apiObject][methodAction]) {
								params.action = cache[(String) params.apiObject]['defaultAction']
							} else {
								params.action = mthd.toString()

								// FORWARD FOR REST DEFAULTS WITH NO ACTION
								String[] tempUri = request.getRequestURI().split("/")
								if (tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']) {
									forward(controller: params.controller, action: params.action)
									return false
								}
							}
						}

						// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
						ApiDescriptor cachedEndpoint = cache[(String) params.apiObject][(String) params.action] as ApiDescriptor
						boolean result = handleApiRequest(cachedEndpoint['deprecated'] as List, cachedEndpoint['method']?.toString().trim(), mthd, response, params)

						return result
					}
				}
			}
			// no cache found

			return false

		}catch(Exception e){
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			return false
		}
	}

	boolean after(){
		//println('##### FILTER (AFTER)')
		try{
			LinkedHashMap newModel = [:]

			if(params.controller!='apidoc') {
				if (!model) {
					render(status: HttpServletResponse.SC_NOT_FOUND, text: 'No resource returned / domain is empty')
					return false
				} else {
					newModel = convertModel(model)
				}
			}else{
				newModel = model as LinkedHashMap
			}

			//LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			ApiDescriptor cachedEndpoint = cache[params.apiObject][(String)params.action] as ApiDescriptor

			String content = handleApiResponse(cachedEndpoint['returns'] as LinkedHashMap,cachedEndpoint['roles'] as List,mthd,format,response,newModel,params)

			if(content){
				// STORE CACHED RESULT
				String format = request.format.toUpperCase()
				String authority = getUserRole() as String
				apiCacheService.setApiCachedResult((String)params.controller, (String) params.apiObject,(String)params.action, authority, format, content)

				render(text:content, contentType:request.contentType)
				return false
			}

			return false
		}catch(Exception e){
			log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
			return false
		}
	}

}
