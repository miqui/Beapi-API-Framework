package net.nosegrind.apiframework

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import grails.converters.JSON
import grails.converters.XML

import java.lang.reflect.Method
import java.util.HashSet;
import java.util.Map;
import javax.lang.model.element.Element

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import grails.plugin.cache.CachePut
import grails.plugin.cache.GrailsValueWrapper
import grails.plugin.cache.GrailsCacheManager

import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.springframework.web.context.request.RequestContextHolder as RCH
import grails.core.GrailsApplication

import net.nosegrind.apiframework.ApiDescriptor

import net.nosegrind.apiframework.*

import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.GParsPool.withPool

class ApiCacheService{

	static transactional = false
	
	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService
	
	def apiLayerService
	//ApiToolkitService apiToolkitService
	GrailsCacheManager grailsCacheManager
	//CacheManager cacheManager

	
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

	/*
	@CacheEvict(value="ApiCache",key="#controllername")
	Map resetApiCache(String controllername,String method,ApiDescriptor apidoc){
		setApiCache(controllername,method,apidoc)
	}
	*/
	
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
				cache[apiversion][methodname]['errorcodes'] = apidoc.errorcodes
				cache[apiversion][methodname]['doc'] = generateApiDoc(controllername, methodname,apiversion)
			}else{
				throw new Exception("[ApiCacheService :: setApiCache] : sts for controller/action pair of ${controllername}/${methodname}")
			}
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setApiCache] : Exception - full stack trace follows:",e)
		}
	}

	/*
	@CachePut(value="ApiCache",key="#controllername")
	LinkedHashMap setApiDocCache(String controllername,String methodname, String apiversion, Map apidoc){
		try{
			def cache = getApiCache(controllername)
			if(cache[apiversion][methodname]){
				cache[apiversion][methodname]['doc'] = generateApiDoc(controllername, methodname, apiversion)
			}else{
				throw new Exception("[ApiCacheService :: setApiCache] : No Cache exists for controller/action pair of ${controllername}/${methodname}")
			}
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setApiDocCache] : Exception - full stack trace follows:",e)
		}
	}
	*/
	
	Map generateApiDoc(String controllername, String actionname, String apiversion){
		try{
			Map doc = [:]
			def cache = getApiCache(controllername)
			String apiPrefix = "v"+grailsApplication.metadata['app.version'] as String
			
			if(cache){
				String path = "/${apiPrefix}-${apiversion}/${controllername}/${actionname}"
				doc = ['path':path,'method':cache[apiversion][actionname]['method'],'description':cache[apiversion][actionname]['description']]
				if(cache[apiversion][actionname]['receives']){
	
					doc['receives'] = [:]
					for(receiveVal in cache[apiversion][actionname]['receives']){
						doc['receives']["$receiveVal.key"] = receiveVal.value
					}
				}
				
				if(cache[apiversion][actionname]['returns']){
					doc['returns'] = [:]
					for(returnVal in cache[apiversion][actionname]['returns']){
						doc['returns']["$returnVal.key"] = returnVal.value
					}
					doc['json'] = [:]
					doc['json'] = processJson(doc["returns"])
				}
				
				//if(cont["${actionname}"]["${apiversion}"]["errorcodes"]){
				//	doc["errorcodes"] = processDocErrorCodes(cont[("${actionname}".toString())][("${apiversion}".toString())]["errorcodes"] as HashSet)
				//}
	
			}
			return doc
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: generateApiDoc] : Exception - full stack trace follows:",e)
		}
	}
	
	LinkedHashMap getApiCache(String controllername){
		try{
			def cache = grailsCacheManager?.getCache('ApiCache')?.get(controllername)
			if(cache?.get()){
				return cache.get() as LinkedHashMap
			}else{ 
				return [:] 
			}

		}catch(Exception e){
			//throw new Exception("[ApiCacheService :: getApiCache] : Exception - full stack trace follows:",e)
			println("[ApiCacheService :: getApiCache] : Exception - full stack trace follows:"+e)
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
		//println("#### [ApiCacheService : processJson ] ####")
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
