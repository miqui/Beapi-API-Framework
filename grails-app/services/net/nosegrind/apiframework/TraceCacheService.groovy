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
//import grails.converters.XML
import grails.plugin.cache.CachePut
import grails.plugin.cache.GrailsCacheManager
import org.grails.groovy.grails.commons.*
import grails.core.GrailsApplication
/*
* Want to be able to :
*  - cache each 'class/method' and associated start/end times and order  in which they are called
*  - prior to API response return, generateJSON() and return JSON as response
 */

class TraceCacheService{

	static transactional = false
	
	GrailsApplication grailsApplication
	GrailsCacheManager grailsCacheManager
	
	// called through generateJSON()

	public void flushCache(String uri){
		try{
			grailsCacheManager?.getCache('Trace').clear()
			//Cache cache = grailsCacheManager.getCache('Trace')
			//cache.clear()
		}catch(Exception e){
			throw new Exception("[TraceCacheService :: getTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	@CachePut(value="Trace",key="#uri")
	LinkedHashMap putTraceCache(String uri, LinkedHashMap cache){
		try{
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: putTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	@CachePut(value="Trace",key="#uri")
	LinkedHashMap setTraceMethod(String uri,LinkedHashMap cache){
		try{
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setTraceCache] : Exception - full stack trace follows:",e)
		}
	}

	LinkedHashMap getTraceCache(String uri){
		try{
			def temp = grailsCacheManager?.getCache('Trace')
			def cache = temp?.get(uri)
			if(cache?.get()){
				return cache.get() as LinkedHashMap
			}else{ 
				return [:] 
			}

		}catch(Exception e){
			throw new Exception("[TraceCacheService :: getTraceCache] : Exception - full stack trace follows:",e)
		}
	}
	
	List getCacheNames(){
		List cacheNames = []
		cacheNames = grailsCacheManager?.getCache('Trace')?.getAllKeys() as List
		return cacheNames
	}
}
