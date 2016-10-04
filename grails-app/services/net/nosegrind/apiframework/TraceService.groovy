package net.nosegrind.apiframework

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

import grails.core.GrailsApplication

import org.grails.groovy.grails.commons.*
import javax.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder as RCH
import org.springframework.web.context.request.ServletRequestAttributes

import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest

class TraceService {

	GrailsApplication grailsApplication
	TraceCacheService traceCacheService

	static transactional = false


	private HttpServletRequest getRequest(){
		HttpServletRequest request = ((ServletRequestAttributes) RCH.currentRequestAttributes()).getRequest()
		return request
	}


	public void startTrace(String className, String methodName){
		Long mStart = System.currentTimeMillis()
		String uri = getRequest().forwardURI
		LinkedHashMap cache = traceCacheService.getTraceCache(uri)
		String loc = "${className}/${methodName}".toString()

		if(!cache['calls'] || cache['calls']==0){
			cache['calls'] = [:]
		}

		Long order = (cache['calls'])?cache['calls'].size()+1:1
		cache['calls']["${order}"] = [:]
		cache['calls']["${order}"][loc] = [:]
		cache['calls']["${order}"][loc]['start'] = mStart
		cache['calls']["${order}"][loc]['stop'] = 0
		traceCacheService.setTraceMethod(uri, cache)
	}

	public LinkedHashMap endTrace(String className, String methodName) {
		String uri = getRequest().forwardURI
		LinkedHashMap cache = traceCacheService.getTraceCache(uri)
		String loc = "${className}/${methodName}".toString()
		Long order = cache['calls'].size()

		// if uri!= loc, walk backwards through cache and find
		// 'loc' that matches and has empty 'stop' value
		if (cache.calls?."${order}"?."${loc}" == null) {
			for(int i = order; i > 0; i--){
				if (cache.calls?."${i}"?."${loc}" != null) {
					cache['calls']["${i}"]["${loc}"]['stop'] = System.currentTimeMillis()
					break;
				}
			}
		}else {
			cache['calls']["${order}"][loc]['stop'] = System.currentTimeMillis()
		}

		return traceCacheService.setTraceMethod(uri, cache)
	}

	public LinkedHashMap endAndReturnTrace(String className, String methodName){
		String uri = getRequest().forwardURI
		LinkedHashMap returnCache = endTrace(className, methodName)
		LinkedHashMap newTrace = processTrace(returnCache)
		traceCacheService.flushCache(uri)
		return newTrace
	}

	private LinkedHashMap processTrace(LinkedHashMap cache){
		LinkedHashMap newTrace = [:]
		cache['calls'].sort{ a, b -> b.key <=> a.key }
		cache['calls'].each() { it ->
			Long startTime = 0
			Long stopTime = 0
			it.value.each() { it2 ->
					String loc = it2.key
					if(startTime==0){ startTime=it2.value['start'] }
					stopTime = it2.value['stop']
					newTrace[it.key] = ['time': getElapsedTime(it2.value['start'], it2.value['stop']), 'loc': loc]
			}
			newTrace['elapsedTime'] = getElapsedTime(startTime,stopTime)
		}
		return newTrace
	}

	private Long getElapsedTime(Long startTime, Long stopTime){
		Long elapsedTime = stopTime - startTime
		if(elapsedTime>=0) {
			return elapsedTime
		}else{
			throw new Exception("[TraceService :: getElapsedTime] : Exception - stopTime is less that startTime")
		}
	}
}
