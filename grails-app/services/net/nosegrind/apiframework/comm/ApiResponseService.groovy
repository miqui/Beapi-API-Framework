package net.nosegrind.apiframework.comm

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/



//import grails.plugin.springsecurity.SpringSecurityService

import grails.web.http.HttpHeaders
import net.nosegrind.apiframework.comm.ApiLayer

import javax.servlet.forward.*
import org.springframework.http.ResponseEntity

import grails.web.servlet.mvc.GrailsParameterMap

import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.grails.web.util.GrailsApplicationAttributes
//import org.grails.web.sitemesh.GrailsContentBufferingResponse

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest

import grails.core.GrailsApplication
import net.nosegrind.apiframework.*
import net.nosegrind.apiframework.Timer

class ApiResponseService extends ApiLayer{

	GrailsApplication grailsApplication


	def handleApiResponse(LinkedHashMap cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		//println("#### [ApiResponseService : handleApiResponse ] ####")

		try{
			if(cache){
				if(cache[params.apiObject][params.action]){
					// make 'application/json' default

					if(params.contentType){
							response.setHeader('Authorization', cache[params.apiObject][params.action]['roles'].join(', '))
							List responseList = getApiParams(request,cache[params.apiObject][params.action]['returns'])
							LinkedHashMap result = parseURIDefinitions(model,responseList)
							Map content = parseResponseMethod(request, params, result)
							return content
					}
				}else{
					//return true
					//render(view:params.action,model:model)
				}
			}
		}catch(Exception e){
			//throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
			println("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:"+e)
		}

	}
	
	boolean validateUrl(String url){
		String[] schemes = ["http","https"]
		UrlValidator urlValidator = new UrlValidator(schemes)
		return urlValidator.isValid(url)
	}
	
	boolean isRequestRedirected(){
		return (request.getAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED) != null)? true : false
	}

	private ArrayList processDocErrorCodes(HashSet error){
		List errors = error as List
		ArrayList err = []
		errors.each{ v ->
			def code = ['code':v.code,'description':v.description]
			err.add(code)
		}
		return err
	}

	/*
	 * TODO: Need to compare multiple authorities
	 */
	def apiRoles(List list) {
		if(springSecurityService.principal.authorities*.authority.any { list.contains(it) }){
			return true
		}
		return ['validation.customRuntimeMessage', 'ApiCommandObject does not validate. Check that your data validates or that requesting user has access to api method and all fields in api command object.']
	}

	/*
    public ResponseEntity<LinkedHashMap> respond(LinkedHashMap model){
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);
        responseHeaders.set("MyResponseHeader", "MyValue");
        return new ResponseEntity<String>("Hello World", responseHeaders, HttpStatus.CREATED);
        return ResponseEntity(model,HttpStatus.BAD_REQUEST);
    }
*/

	Map convertModel(Map map){
		//println("#### [ApiResponseService : convertModel ] ####")

		try{
			Map newMap = [:]
			String k = map.entrySet().toList().first().key
			if(map && (!map?.response && !map?.metaClass && !map?.params)){
				if(grailsApplication.isDomainClass(map[k].getClass())){
					newMap = formatDomainObject(map[k])
					return newMap
				}else if(['class java.util.LinkedList','class java.util.ArrayList'].contains(map[k].getClass())) {
					newMap = formatList(map[k])
					return newMap
				}else if(['class java.util.Map','class java.util.LinkedHashMap'].contains(map[k].getClass())) {
					newMap = formatMap(map[k])
					return newMap
				}
			}
			return newMap
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: convertModel] : Exception - full stack trace follows:",e)
		}
	}

	Map formatDomainObject(Object data){
		//println("#### [ApiResponseService : formatDomainObject ] ####")
		try{
			List nonPersistent = ['log', 'class', 'constraints', 'properties', 'errors', 'mapping', 'metaClass','maps']
			Map newMap = [:]

			newMap.put('id',data?.id)
			newMap.put('version',data?.version)


			data.properties.each { key, val ->
				if (!nonPersistent.contains(key)) {
					// no lazy mapping
					newMap[key] = (grailsApplication.isDomainClass(val.getClass()))? val.id:val
				}
			}

			return newMap
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: formatDomainObject] : Exception - full stack trace follows:",e)
		}
	}



	Map formatList(List list){
		//println("#### [ApiResponseService : formatList ] ####")
		Map newMap = [:]
		list.eachWithIndex(){ val, key ->
			if(val){
				if(grailsApplication.isDomainClass(val.getClass())){
					newMap[key]=formatDomainObject(val)
				}else{
					newMap[key] = ((val in java.util.ArrayList || val in java.util.List) || val in java.util.Map)?val:val.toString()
				}
			}
		}
		return newMap
	}

	Map formatMap(Map map) {
		//println("#### [ApiResponseService : formatMap ] ####")
		Map newMap = [:]
		map.each(){ key, val ->
			if(val){
				if(grailsApplication.isDomainClass(val.getClass())){
					newMap[key]=formatDomainObject(val)
				}else{
					newMap[key] = ((val in java.util.ArrayList || val in java.util.List) || val in java.util.Map)?val:val.toString()
				}
			}
		}
		return newMap
	}

}
