package net.nosegrind.apiframework.comm

import grails.converters.JSON

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/
import grails.converters.XML
import grails.core.GrailsApplication
import grails.web.http.HttpHeaders

//import grails.plugin.springsecurity.SpringSecurityService
import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.http.ResponseEntity

import javax.servlet.forward.*

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import net.nosegrind.apiframework.*

class ChainResponseService extends ApiLayerService{
	
	GrailsApplication grailsApplication
	
	boolean handleApiChain(LinkedHashMap cache, HttpServletRequest request, HttpServletResponse response, Map model, GrailsParameterMap params){
		try{
			List uri = [params.controller,params.action,params.id]
			ApiStatuses errors = new ApiStatuses()
			List keys = []
			String controller
			String action
			List uri2 = []
			
			if(params?.apiChain?.order!='null'){
				keys = params?.apiChain?.order.keySet() as List
				uri2 = keys.last().split('/')
				controller = uri2[0]
				action = uri2[1]
			}
			
			Long id = model.id

			
			//if(keys.last() && (params?.apiChain?.order["${keys.last()}"]!='null' && params?.apiChain?.order["${keys.last()}"]!='return')){
			if(keys.last()){
				int pos = checkChainedMethodPosition(cache,request,params,uri,params?.apiChain?.order as Map)
				if(pos==3){
					String msg = '[ERROR] Bad combination of unsafe METHODS in api chain.'
					errors._403_FORBIDDEN(msg).send()
					return false
				}else{
					if(!uri2){
						String msg = 'Path was unable to be parsed. Check your path variables and try again.'
						errors._404_NOT_FOUND(msg).send()
						return false
					}
					
					def currentPath = "${controller}/${action}"
					List roles = cache[params.apiObject][params.action]['roles'].toArray() as List
					if(checkAuth(request,roles)){
						/*
						if(params?.apiChain.combine=='true'){
							params.apiCombine["${params.controller}/${params.action}"] = parseURIDefinitions(model,cache[params.action][params.apiObject]['returns'])
						}
						*/
						params.controller = controller
						params.action = action

						if(params.apiChain.key){
							params.id = model[params.apiChain.key]
							params.apiChain.remove('key')
						}else{
							params.id = model.id
						}

						if(params?.apiChain.combine=='true'){
							params.apiCombine[currentPath] = parseURIDefinitions(request,model,cache[params.apiObject][params.action]['returns'])
						}
						
						if(keys.last() && (params?.apiChain?.order["${keys.last()}"]=='null' && params?.apiChain?.order["${keys.last()}"]=='return')){
							params.remove('apiChain')
						}
						params?.apiChain?.order.remove("$currentPath")
						return true
					}else{
						String msg = "User does not have access."
						errors._403_FORBIDDEN(msg).send()
						return false
					}
				}
			}
			//}else{
			//	params.remove("apiChain")
			//}

			return false
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: handleApiChain] : Exception - full stack trace follows:",e)
		}
	}
	
	def handleApiResponse(LinkedHashMap cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		try{
			String type = ''
			if(cache){
				if(cache[params.apiObject][params.action]){
					// make 'application/json' default
					//def formats = ['text/html','text/json','application/json','text/xml','application/xml']
					//type = (params.contentType)?formats.findAll{ type.startsWith(it) }[0].toString():params.contentType
					//if(type){
							response.setHeader('Authorization', cache[params.apiObject][params.action]['roles'].join(', '))
							LinkedHashMap result = parseURIDefinitions(request,model,cache[params.apiObject][params.action]['returns'])
							if(params?.apiChain?.combine=='true'){
								if(!params.apiCombine){ params.apiCombine = [:] }
								String currentPath = "${params.controller}/${params.action}"
								params.apiCombine[currentPath] = result
							}
							Map content = parseResponseMethod(request, params, result,cache[params.apiObject][params.action]['returns'])
							return content
					//}else{
						//return true
						//render(view:params.action,model:model)
					//}
				}else{
					//return true
					//render(view:params.action,model:model)
				}
			}
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
		}
	}
	
	boolean isChain(HttpServletRequest request,GrailsParameterMap params){
		try{
			String type = params.format
			if(request."${type}"?.chain){
				return true
			}
			return false
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: isChain] : Exception - full stack trace follows:",e)
		}
	}
	
	LinkedHashMap parseURIDefinitions(HttpServletRequest request, LinkedHashMap model,LinkedHashMap responseDefinitions){
		try{
			ApiStatuses errors = new ApiStatuses()
			String msg = 'Error. Invalid variables being returned. Please see your administrator'
			List optionalParams = ['action','controller','apiName_v','contentType', 'encoding','apiChain', 'apiBatch', 'apiCombine', 'apiObject','apiObjectVersion', 'chain']
			List responseList = getApiParams(request,responseDefinitions)

			HashMap params = getMethodParams()
			//GrailsParameterMap params = RCH.currentRequestAttributes().params
			List paramsList = model.keySet() as List
			paramsList.removeAll(optionalParams)
			if(!responseList.containsAll(paramsList)){
				paramsList.removeAll(responseList)
				paramsList.each(){ it ->
					model.remove("${it}".toString())
				}
				if(!paramsList){
					errors._400_BAD_REQUEST(msg).send()
					return [:]
				}else{
					return model
				}
			}else{
				return model
			}
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: parseURIDefinitions] : Exception - full stack trace follows:",e)
		}
	}
	
	Integer getKey(String key){
		switch(key){
			case'FKEY':
				return 2
				break
			case 'PKEY':
				return 1
				break
			default:
				return 0
		}
	}
	
	boolean validateUrl(String url){
		String[] schemes = ["http","https"]
		UrlValidator urlValidator = new UrlValidator(schemes)
		return urlValidator.isValid(url)
	}
	
	boolean isRequestRedirected(){
		if(request.getAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED) != null){
			return true
		}else{
			return false
		}
	}
	
	List getRedirectParams(){
		def uri = grailsApplication.mainContext.servletContext.getControllerActionUri(request)
		//def uri = HOLDER.getServletContext().getControllerActionUri(request)
		return uri[1..(uri.size()-1)].split('/')
	}
	
	private ArrayList processDocValues(List<ParamsDescriptor> value){
		List val2 = []
		value.each{ v ->
			Map val = [:]
			val = [
				'paramType':v.paramType,
				'name':v.name,
				'description':v.description
			]
			
			if(v.paramType=='PKEY' || v.paramType=='FKEY'){
				val["idReferences"] = v.idReferences
			}
	
			if(v.required==false){
				val['required'] = false
			}
			if(v.mockData){
				val['mockData'] = value.mockData
			}
			if(v.values){
				val['values'] = processDocValues(v.values)
			}
			if(v.roles){
				val['roles'] = v.roles
			}
			val2.add(val)
		}
		return val2
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
	private String processJson(LinkedHashMap returns){
		def json = [:]
		returns.each{ p ->
				p.value.each{ it ->

					ParamsDescriptor paramDesc = it
				
					def j = [:]
					if(paramDesc?.values){
						j[paramDesc.name]=[]
					}else{
						String dataName=(['PKEY','FKEY','INDEX'].contains(paramDesc.paramType.toString()))?'ID':paramDesc.paramType
						j = (paramDesc?.mockData?.trim())?["${paramDesc.name}":paramDesc.mockData]:["${paramDesc.name}":dataName]
					}
					j.each(){ key,val ->
						if(val instanceof List){
							def child = [:]
							val.each(){ it2 ->
								it2.each(){ key2,val2 ->
									child[key2] = val2
								}
							}
							json[key] = child
						}else{
							json[key]=val
						}
					}
				}
		}

		if(json){
			json = json as JSON
		}
		return json
	}
	
	/*
	 * TODO: Need to compare multiple authorities
	 */
	LinkedHashMap getApiDoc(GrailsParameterMap params){
		LinkedHashMap newDoc = [:]
		List paramDescProps = ['paramType','idReferences','name','description']
		try{
			def controller = grailsApplication.getArtefactByLogicalPropertyName('Controller', params.controller)
			if(controller){
				def cache = (params.controller)?apiCacheService.getApiCache(params.controller):null
				if(cache){
					if(cache[params.apiObject][params.action]){
	
						def doc = cache[params.apiObject][params.action].doc
						def path = doc?.path
						def method = doc?.method
						def description = doc?.description
	
						def authority = springSecurityService.principal.authorities*.authority[0]
						newDoc[params.action] = ['path':path,'method':method,'description':description]
						if(doc.receives){
							newDoc[params.action].receives = [:]
							doc.receives.each{ it ->
								if(authority==it.key || it.key=='permitAll'){
									it.value.each(){ it2 ->
										it2.getProperties().each(){ it3 ->
											if(paramDescProps.contains(it3.key)){
												//println("receives > ${it3.key} : ${it3.value}")
												newDoc[params.action].receives[it3.key] = it3.value
											}
										}
									}
									//newDoc[params.action].receives[it.key] = it.value
								}
							}
						}

						if(doc.returns){
							newDoc[params.action].returns = [:]
							List jsonReturns = []
							doc.returns.each(){ v ->
								if(authority==v.key || v.key=='permitAll'){
									jsonReturns.add(['${v.key}':v.value])
									v.value.each(){ v2 ->
										v2.getProperties().each(){ v3 ->
											if(paramDescProps.contains(v3.key)){
												//println("receives > ${v3.key} : ${v3.value}")
												newDoc[params.action].returns[v3.key] = v3.value
											}
										}
									}
									//newDoc[params.action].returns[v.key] = v.value
								}
							}

							//newDoc[params.action].json = processJson(newDoc[params.action].returns)
							newDoc[params.action].json = processJson(jsonReturns)
						}
						
						if(doc.errorcodes){
							doc.errorcodes.each{ it ->
								newDoc[params.action].errorcodes.add(it)
							}
						}
						return newDoc
					}
				}
			}
			return [:]
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: getApiDoc] : Exception - full stack trace follows:",e)
		}
	}

	Map formatDomainObject(Object data){
		try{
			def nonPersistent = ["log", "class", "constraints", "properties", "errors", "mapping", "metaClass","maps"]
			def newMap = [:]

			if(data?.'id'){
				newMap['id'] = data.id
			}
			if(data?.'version'!=null){
				newMap['version'] = data.version
			}
			
			data.getProperties().each { key, val ->
				if (!nonPersistent.contains(key)) {
					if(grailsApplication.isDomainClass(val.getClass())){
						newMap.put key, val.id
					}else{
						newMap.put key, val
					}
				}
			}

			return newMap
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: formatDomainObject] : Exception - full stack trace follows:",e)
		}
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

    public ResponseEntity<LinkedHashMap> respond(LinkedHashMap model){
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);
        responseHeaders.set("MyResponseHeader", "MyValue");
        return new ResponseEntity<String>("Hello World", responseHeaders, HttpStatus.CREATED);
        return ResponseEntity(model,HttpStatus.BAD_REQUEST);
    }

	Map convertModel(Map map){
		try{
			Map newMap = [:]
			String k = map?.entrySet()?.toList()?.first()?.key
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

	Map formatList(List list){
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

	Map parseResponseMethod(HttpServletRequest request, GrailsParameterMap params, Map map, LinkedHashMap returns){
		Map data = [:]
		switch(request.method) {
			case 'PURGE':
				// cleans cache; disabled for now
				break;
			case 'TRACERT':
				break;
			case 'TRACE':
				break;
			case 'HEAD':
				break;
			case 'OPTIONS':
				String contentType = (params.contentType)?params.contentType:'application/json'
				String encoding = (params.encoding)?params.encoding:"UTF-8"
				LinkedHashMap doc = getApiDoc(params)
				data = ['content':doc,'contentType':contentType,'encoding':encoding]
				break;
			case 'GET':
				if(map?.isEmpty()==false){
					data = parseContentType(request,params, map, returns)
				}
				break;
			case 'PUT':
				if(!map.isEmpty()){
					data = parseContentType(request,params, map, returns)
				}
				break;
			case 'POST':
				if(!map.isEmpty()){
					data = parseContentType(request,params, map, returns)
				}
				break;
			case 'DELETE':
				if(!map.isEmpty()){
					data = parseContentType(request,params, map, returns)
				}
				break;
		}
		return ['apiToolkitContent':data.content,'apiToolkitType':data.contentType,'apiToolkitEncoding':data.encoding]
	}

	Map parseContentType(HttpServletRequest request, GrailsParameterMap params, Map map, LinkedHashMap returns){
		String content
		String type = params.format
		String contentType = "application/${params.format.toLowerCase()}"
		String encoding = (params.encoding)?params.encoding:"UTF-8"
		LinkedHashMap result = parseURIDefinitions(request,map,returns)
		switch(type){
			case 'XML':
				content = result as XML
				break
			case 'JSON':
			default:
				content = result as JSON
		}

		return ['content':content,'type':contentType,'encoding':encoding]
	}
}
