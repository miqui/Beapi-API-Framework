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

import grails.converters.JSON
//import grails.converters.XML
import grails.plugin.cache.CachePut
import grails.plugin.cache.GrailsCacheManager
import org.grails.groovy.grails.commons.*
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService


class LimitCacheService{

	static transactional = false
	
	GrailsApplication grailsApplication
	GrailsCacheManager grailsCacheManager
	SpringSecurityService springSecurityService

	// called through generateJSON()

	public void resetLimitCache(String userId){
		try{
			grailsCacheManager?.getCache('Limit').remove(userId)
			LinkedHashMap cache = ['rateLimitTimestamp':System.currentTimeMillis()/1000, 'rateLimitCurrent': 1,'locked':false]
			setLimitCache(userId, cache)
		}catch(Exception e){
			throw new Exception("[LimitCacheService :: getTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	@CachePut(value="Limit",key="#userId")
	LinkedHashMap setLimitCache(String userId, LinkedHashMap cache){
		try{
			return cache
		}catch(Exception e){
			throw new Exception("[LimitCacheService :: putTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	@CachePut(value="Limit",key="#userId")
	LinkedHashMap incrementLimitCache(String userId){
		try{
			def cache = getLimitCache(userId)
			cache['rateLimitCurrent']++
			return cache
		}catch(Exception e){
			throw new Exception("[LimitCacheService :: putTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	@CachePut(value="Limit",key="#userId")
	LinkedHashMap lockLimitCache(String uri){
		def cache = getLimitCache(userId)
		cache['locked']=true
		return cache
	}

	@CachePut(value="Limit",key="#userId")
	LinkedHashMap checkLimitCache(String userId,String role){
		// check role against config role limit
	}

	LinkedHashMap getLimitCache(String userId){
		try{
			def temp = grailsCacheManager?.getCache('Trace')
			def cache = temp?.get(uri)
			if(cache?.get()){
				return cache.get() as LinkedHashMap
			}else{ 
				return [:] 
			}

		}catch(Exception e){
			throw new Exception("[LimitCacheService :: getTraceCache] : Exception - full stack trace follows:",e)
		}
	}
	
	List getCacheNames(){
		List cacheNames = []
		cacheNames = grailsCacheManager?.getCache('Limit')?.getAllKeys() as List
		return cacheNames
	}
}
