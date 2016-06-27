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
import grails.converters.XML
import grails.util.Metadata
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import grails.plugin.cache.CachePut
import grails.plugin.cache.GrailsValueWrapper
import grails.plugin.cache.GrailsCacheManager
import org.grails.groovy.grails.commons.*
import grails.core.GrailsApplication
import net.nosegrind.apiframework.ApiDescriptor

import static groovyx.gpars.GParsPool.withPool

class ApiCacheService{

	static transactional = false
	
	GrailsApplication grailsApplication
	GrailsCacheManager grailsCacheManager


	
	/*
	 * Only flush on RESTART.
	 * DO NOT flush while LIVE!!!
	 * Need to lock this down to avoid process calling this.
	 */
	void flushAllApiCache(){
		grailsApplication?.controllerClasses?.each { controllerClass ->
			String controllername = controllerClass.logicalPropertyName
			if(controllername!='aclClass'){
				flushApiCache(controllername)
			}
		}
	}

	/*
	 * Only flush on RESTART.
	 * DO NOT flush while LIVE!!!
	 */
	@CacheEvict(value="ApiCache",key="#controllername")
	void flushApiCache(String controllername){} 

	
	@CachePut(value="ApiCache",key="#controllername")
	LinkedHashMap setApiCache(String controllername,LinkedHashMap apidesc){
		return apidesc
	}
	
	@CachePut(value="ApiCache",key="#controllername")
	LinkedHashMap setApiCache(String controllername,String methodname, ApiDescriptor apidoc, String apiversion){
		try{
			def cache = getApiCache(controllername)
			if(!cache[apiversion][methodname]){
				cache[apiversion][methodname] = [:]
			}
			if(cache[apiversion][methodname]){
				cache[apiversion][methodname]['name'] = apidoc.name
				cache[apiversion][methodname]['description'] = apidoc.description
				cache[apiversion][methodname]['receives'] = apidoc.receives
				cache[apiversion][methodname]['returns'] = apidoc.returns
				cache[apiversion][methodname]['doc'] = generateApiDoc(controllername, methodname,apiversion)
			}else{
				throw new Exception("[ApiCacheService :: setApiCache] : sts for controller/action pair of ${controllername}/${methodname}")
			}
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setApiCache] : Exception - full stack trace follows:",e)
		}
	}
	
	Map generateApiDoc(String controllername, String actionname, String apiversion){
		try{
			Map doc = [:]
			def cache = getApiCache(controllername)

			String apiPrefix = "v${Metadata.current.getApplicationVersion()}"

			if(cache){
				String path = "/${apiPrefix}-${apiversion}/${controllername}/${actionname}"
				doc = ['path':path,'method':cache[apiversion][actionname]['method'],'description':cache[apiversion][actionname]['description']]
				if(cache[apiversion][actionname]['receives']){
					
					doc['receives'] = [:]
					for(receiveVal in cache[apiversion][actionname]['receives']){
						if(receiveVal?.key) {
							doc['receives']["$receiveVal.key"] = receiveVal.value
						}
					}
				}
				
				if(cache[apiversion][actionname]['returns']){
					doc['returns'] = [:]
					for(returnVal in cache[apiversion][actionname]['returns']){
						if(returnVal?.key) {
							doc['returns']["$returnVal.key"] = returnVal.value
						}
					}
					doc['json'] = [:]
					doc['json'] = processJson(doc["returns"])
				}
	
			}
			return doc
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: generateApiDoc] : Exception - full stack trace follows:",e)
		}
	}
	
	LinkedHashMap getApiCache(String controllername){
		try{
			def temp = grailsCacheManager?.getCache('ApiCache')
			def cache = temp?.get(controllername)
			if(cache?.get()){
				return cache.get() as LinkedHashMap
			}else{ 
				return [:] 
			}

		}catch(Exception e){
			throw new Exception("[ApiCacheService :: getApiCache] : Exception - full stack trace follows:",e)
		}
	}
	
	List getCacheNames(){
		List cacheNames = []
		cacheNames = grailsCacheManager?.getCache('ApiCache')?.getAllKeys() as List
		return cacheNames
	}

	/*
 * TODO: Need to compare multiple authorities
 */
	private String processJson(LinkedHashMap returns){
		try{
			LinkedHashMap json = [:]
			returns.each{ p ->
				p.value.each{ it ->
					if(it) {
						ParamsDescriptor paramDesc = it

						LinkedHashMap j = [:]
						if (paramDesc?.values) {
							j["$paramDesc.name"] = []
						} else {
							String dataName = (['PKEY', 'FKEY', 'INDEX'].contains(paramDesc?.paramType?.toString())) ? 'ID' : paramDesc.paramType
							j = (paramDesc?.mockData?.trim()) ? ["$paramDesc.name": "$paramDesc.mockData"] : ["$paramDesc.name": "$dataName"]
						}
						withPool(8) {
							j.eachParallel { key, val ->
								if (val instanceof List) {
									def child = [:]
									withPool(8) {
										val.eachParallel { it2 ->
											withPool(8) {
												it2.eachParallel { key2, val2 ->
													child[key2] = val2
												}
											}
										}
									}
									json[key] = child
								} else {
									json[key] = val
								}
							}
						}
					}
				}
			}

			String jsonReturn
			if(json){
				jsonReturn = json as JSON
			}
			return jsonReturn
		}catch(Exception e){
			throw new Exception("[ApiLayerService :: processJson] : Exception - full stack trace follows:",e)
		}
	}
}
