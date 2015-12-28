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

import grails.core.GrailsApplication

//import grails.application.springsecurity.SpringSecurityService
import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest;

class ApiDomainService{

	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService

	static transactional = false
	
	def showInstance(LinkedHashMap cache, GrailsParameterMap params){
		def domainInstance
		try{
			domainInstance = grailsApplication.getDomainClass(cache[params.apiObject]['domainPackage']).newInstance()
		}catch(Exception e){
			log.error("[ApiDomainService :: showInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", e);
		}

		return domainInstance.get(params.id.toLong())
	}
	
	def createInstance(LinkedHashMap cache, GrailsParameterMap params){
		def domain
		try{
			domain = grailsApplication.getDomainClass(cache[params.apiObject]['domainPackage'])
		}catch(Exception e){
			log.error("[ApiDomainService :: createInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", e);
		}
		def request = getRequest()
		def apiParams = getApiObjectParams(request,cache[params.apiObject][params.action]['receives'])

		def domainInstance = domain.newInstance()
		apiParams.each{ k,v ->
			if(apiParams[k]=='FKEY'){
				def index = k[0..-3]
				def type = domain.getPropertyByName(index).type
				
				domainInstance[index] = type.get(params[index].toLong())
			}else{
				domainInstance[k] = params[k]
			}
		}

		if(!domainInstance.save(flush:true)){
			log.error("[ApiDomainService :: createInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", domainInstance.errors.allErrors);
		}else{
			return domainInstance
		}
		return null
	}
	
	def updateInstance(LinkedHashMap cache, GrailsParameterMap params){
		def domain
		try{
			domain = grailsApplication.getDomainClass(cache[params.apiObject]['domainPackage'])
		}catch(Exception e){
			log.error("[ApiDomainService :: updateInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", e);
		}
		def request = getRequest()
		def apiParams = getApiObjectParams(request,cache[params.apiObject][params.action]['receives'])

		def domainInstance
		def pkeys = apiParams.collect(){ if(it.value == 'PKEY'){ it.key } }
		if(pkeys.contains('id')){
			domainInstance = domain.clazz.get(params.id.toLong())
		}

		apiParams.each{ k,v ->
			if(apiParams[k]=='FKEY'){
				def index = k[0..-3]
				def type = domain.getPropertyByName(index).type
				domainInstance[index] = type.get(params[index].toLong())
			}else{
				if(apiParams[k]!='PKEY'){
					domainInstance[k] = params[k]
				}
			}
		}

		if(!domainInstance.save(flush:true)){
			log.error("[ApiDomainService :: updateInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", domainInstance.errors.allErrors);
		}else{
			return domainInstance
		}
		return null
	}
	
	String deleteInstance(LinkedHashMap cache, GrailsParameterMap params){
		def domainInstance
		try{
			def request = getRequest()
			def apiParams = getApiObjectParams(request,cache[params.apiObject][params.action]['receives'])
			def pkeys = apiParams.collect(){ if(it.value == 'PKEY'){ it.key } }
			try{
				if(pkeys.contains('id')){
					domainInstance = grailsApplication.getDomainClass(cache[params.apiObject]['domainPackage']).clazz.get(params.id.toLong())
				}
			}catch(Exception e){
				log.error("[ApiDomainService :: deleteInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", e);
			}
	
			if(!domainInstance.delete(flush:true)){
				log.error("[ApiDomainService :: deleteInstance] : Could not find domain package '${domainPackage}' - full stack trace follows:", domainInstance.errors.allErrors);
			}else{
				return null
			}
		}catch(Exception e){
			log.error("[ApiDomainService :: deleteInstance] : Could not find domain - full stack trace follows:", e);
		}
	}

	private HttpServletRequest getRequest(){
		//return RCH.currentRequestAttributes().currentRequest
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest()
		return request
	}

	LinkedHashMap getApiObjectParams(HttpServletRequest request, LinkedHashMap definitions){
		//println("#### [ParamsService : getApiObjectParams ] ####")
		try{
			LinkedHashMap apiList = [:]
			definitions.each{ key,val ->
				if(request.isUserInRole(key) || key=='permitAll'){
					val.each{ it ->
						if(it){
							apiList[it.name] = it.paramType
						}
					}
				}
			}
			return apiList
		}catch(Exception e){
			//throw new Exception("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:",e)
			println("[ParamsService :: getApiObjectParams] : Exception - full stack trace follows:"+e)
		}
	}
}
