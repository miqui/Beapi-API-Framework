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
	List formats = ['XML', 'JSON']
	String mthdKey
	RequestMethod mthd
	LinkedHashMap cache = [:]

	ApiFrameworkInterceptor(){
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		println('##### FILTER (BEFORE)')

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
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: "Expected request method '${expectedMethod}' does not match sent method '${mthd.getKey()}'")
						return false
					}

					params.max = (params.max)?params.max:0
					params.offset = (params.offset)?params.offset:0


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
						render(status: HttpServletResponse.SC_BAD_REQUEST, text: 'Expected request variables for endpoint do not match sent variables')
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
						println(cachedEndpoint['method'])
						boolean result = handleApiRequest(cachedEndpoint['deprecated'] as List, (cachedEndpoint['method'])?.toString(), mthd, response, params)

						return result
					}
				}
			}
			// no cache found

			return false

		}catch(Exception e){
			//log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			return false
		}
	}

	boolean after(){
		println('##### FILTER (AFTER)')

		try {
			LinkedHashMap newModel = [:]
			if (params.controller != 'apidoc') {
				if (!model) {
					render(status: HttpServletResponse.SC_NOT_FOUND, text: 'No resource returned / domain is empty')
					return false
				} else {
					newModel = convertModel(model)
				}
			} else {
				newModel = model as LinkedHashMap
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

				if (content) {
					// STORE CACHED RESULT
					String format = request.format.toUpperCase()
					String authority = getUserRole() as String

					if (!newModel) {
						apiCacheService.setApiCachedResult((String) params.controller, (String) params.apiObject, (String) params.action, authority, format, content)
					}
					render(text: content, contentType: request.contentType)
					return false
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
