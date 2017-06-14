/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
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

import grails.util.Holders
//import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.transform.CompileStatic
import org.springframework.http.HttpStatus


@CompileStatic
class ApiFrameworkInterceptor extends ApiCommLayer{

	int order = HIGHEST_PRECEDENCE + 999

	@Resource
	GrailsApplication grailsApplication
	ApiCacheService apiCacheService
	SpringSecurityService springSecurityService
	WebhookService webhookService

	// TODO: detect and assign apiObjectVersion from uri
	String entryPoint = "v${Metadata.current.getProperty(Metadata.APPLICATION_VERSION, String.class)}"
	String format
	List formats = ['XML', 'JSON']
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
		if (formats.contains(format)) {
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
						render(status: 400, text: "Expected request method '${expectedMethod}' does not match sent method '${mthd.getKey()}'")
						return false
					}

					params.max = (params.max!=null)?params.max:0
					params.offset = (params.offset!=null)?params.offset:0


					// CHECK FOR REST ALTERNATIVES
					if (restAlt) {
						// PARSE REST ALTS (TRACE, OPTIONS, ETC)
						String result = parseRequestMethod(mthd, params)
						if (result) {
							byte[] contentLength = result.getBytes( "ISO-8859-1" )
							if(checkLimit(contentLength.length)) {
								render(text: result, contentType: request.contentType)
								return false
							}else{
								render(status: 400, text: 'Rate Limit exceeded. Please wait'+getThrottleExpiration()+'seconds til next request.')
								return false
							}
						}
					}

					// CHECK REQUEST VARIABLES MATCH ENDPOINTS EXPECTED VARIABLES
					println("${params.controller}/${params.action}")
					LinkedHashMap receives = cache[params.apiObject][params.action.toString()]['receives'] as LinkedHashMap
					//boolean requestKeysMatch = checkURIDefinitions(params, receives)
					if (!checkURIDefinitions(params, receives)) {
						render(status: HttpStatus.BAD_REQUEST.value(), text: 'Expected request variables for endpoint do not match sent variables')
						response.flushBuffer()
						return false
					}

					// RETRIEVE CACHED RESULT; DON'T CACHE LISTS
					if (cache[params.apiObject][params.action.toString()]['cachedResult']) {
						String authority = getUserRole() as String
						String domain = ((String) params.controller).capitalize()

						JSONObject json = (JSONObject) cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()]
						if(!json){
							return false
						}else{
							if (isCachedResult((Integer) json.get('version'), domain)) {
								String result = cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()] as String
								byte[] contentLength = result.getBytes( "ISO-8859-1" )
								if(checkLimit(contentLength.length)) {
									render(text: result, contentType: request.contentType)
									return false
								}else{
									render(status: 400, text: 'Rate Limit exceeded. Please wait'+getThrottleExpiration()+'seconds til next request.')
									response.flushBuffer()
									return false
								}
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
						boolean result = handleApiRequest(cachedEndpoint['deprecated'] as List, (cachedEndpoint['method'])?.toString(), mthd, response, params)
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

		List unsafeMethods = ['PUT','POST','DELETE']
		try {
			LinkedHashMap newModel = [:]
			if (params.controller != 'apidoc') {
				if (!model) {
					render(status: HttpServletResponse.SC_NOT_FOUND, text: 'No resource returned / domain is empty')
					response.flushBuffer()
					return false
				} else {
					newModel = convertModel(model)
				}
			} else {
				newModel = model as LinkedHashMap
			}

			// store webhook
			if(unsafeMethods.contains(request.method.toUpperCase())) {
				// if controller/action ROLES/HOOK has roles, is HOOKABLE
				LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
				if (cache) {
					List hookRoles = cache["${params.apiObject}"]["${params.action}"]['hookRoles'] as List
					if(hookRoles.size()>0) {
						webhookService.postData(params.controller.toString(), newModel, params.action.toString())
					}
				}
			}

			//LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			ApiDescriptor cachedEndpoint = cache[params.apiObject][(String) params.action] as ApiDescriptor

			// TEST FOR NESTED MAP; WE DON'T CACHE NESTED MAPS
			boolean isNested = false
			if (newModel != [:]) {
				Object key = newModel?.keySet()?.iterator()?.next()
				if (newModel[key].getClass().getName() == 'java.util.LinkedHashMap') {
					isNested = true
				}

				String content = handleApiResponse(cachedEndpoint['returns'] as LinkedHashMap, cachedEndpoint['roles'] as List, mthd, format, response, newModel, params)

				byte[] contentLength = content.getBytes( "ISO-8859-1" )
				if (content) {
					// STORE CACHED RESULT
					String format = request.format.toUpperCase()
					String authority = getUserRole() as String

					if (!newModel) {
						apiCacheService.setApiCachedResult((String) params.controller, (String) params.apiObject, (String) params.action, authority, format, content)
					}
					if(checkLimit(contentLength.length)) {
						render(text: content, contentType: request.contentType)
						return false
					}else{
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: 'Rate Limit exceeded. Please wait'+getThrottleExpiration()+'seconds til next request.')
						return false
					}
				}
			}else{
				render(text: newModel, contentType: request.contentType)
			}

			return false
		}catch(Exception e){
			log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
			return false
		}
	}

}
