package net.nosegrind.apiframework

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import org.grails.web.json.JSONObject

import java.lang.reflect.Method

import org.grails.core.DefaultGrailsControllerClass
import grails.core.GrailsApplication

//import grails.util.Holders
import grails.converters.JSON
import grails.converters.XML
import groovy.util.ConfigObject
import grails.util.Environment
import grails.util.BuildSettings
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.nosegrind.apiframework.ApiDescriptor
import net.nosegrind.apiframework.ApiStatuses

import grails.plugins.GrailsPluginInfo
import grails.plugins.GrailsPluginManager
import grails.plugins.GrailsPlugin

//import grails.plugin.cache.GrailsCacheManager
//import org.springframework.cache.CacheManager
//import grails.plugin.springsecurity.SpringSecurityService

import org.springframework.cache.Cache
import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.springframework.web.context.request.RequestContextHolder as RCH

import net.nosegrind.apiframework.*

class ApiObjectService{

	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService
	ApiLayerService apiLayerService
	//ApiToolkitService apiToolkitService
	//GrailsCacheManager grailsCacheManager
	//CacheManager cacheManager
	ApiCacheService apiCacheService
	def grailsResourceLocator
	
	
	static transactional = false
	
	public initialize(){
		//Object version = 0.1

		try {
			if(grailsApplication.config.apitoolkit.serverType=='master'){
				String ioPath
				if(grailsApplication.isWarDeployed()){
					//ioPath = Holders.servletContext.getRealPath('/')
					ioPath = grailsApplication.mainContext.servletContext.getRealPath('/')
					if(Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST){
						ioPath += 'WEB-INF/classes/iostate'
					}else{
						// test in Environment.PRODUCTION
						ioPath += 'WEB-INF/classes/iostate'
					}
				}else{
					//ioPath = (String) BuildSettings.BASE_DIR?.path
					//ioPath = System.getProperty(BuildSettings.PROJECT_RESOURCES_DIR)
					//GrailsPluginManager pluginMngr = grailsApplication.getMainContext().getBean('pluginManager')
					//ioPath = pluginMngr.getGrailsPlugin("grailsApiFramework").getPluginPath()
					if(Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST){
						//ioPath = grailsApplication.mainContext.getResources("conf/iostate").path
						ioPath = 'src/iostate'
					}else{
						// test in Environment.PRODUCTION
						//ioPath = grailsApplication.mainContext.getResources("conf/iostate").path
						ioPath += 'src/iostate'
					}
					
				}
				parseFiles(ioPath)
			}
			
			String apiObjectSrc = grailsApplication.config.apitoolkit.iostate.preloadDir
			parseFiles(apiObjectSrc.toString())
			
			
		} catch (Exception e) {
			throw new Exception("[ApiObjectService :: initialize] : Exception - full stack trace follows:",e)
		}
	}
	
	private Map parseFile(JSONObject json){
		String apiObjectName = json.NAME.toString()
		parseJson(json.NAME.toString(),json)
	}
	
	private parseFiles(String path){
		new File(path).eachFile() { file ->
			def tmp = file.name.toString().split('\\.')
			if(tmp[1]=='json'){
				try{
					JSONObject json = JSON.parse(file.text)
					parseJson(json.NAME.toString(),json)
					//def cache = apiCacheService.getApiCache(apiName)
				}catch(Exception e){
					throw new Exception("[ApiObjectService :: initialize] : Unacceptable file '${file.name}' - full stack trace follows:",e)
				}
			}
		}
	}
	
	String getKeyType(String reference, String type){
		String keyType = (reference.toLowerCase()=='self')?((type.toLowerCase()=='long')?'PKEY':'INDEX'):((type.toLowerCase()=='long')?'FKEY':'INDEX')
		return keyType
	}
	
	private LinkedHashMap getIOSet(JSONObject io,LinkedHashMap apiObject){
		LinkedHashMap<String,ParamsDescriptor> ioSet = [:]

		io.each{ k, v ->
			// init
			if(!ioSet[k]){ ioSet[k] = [] }
			
			def roleVars=v.toList()
			roleVars.each{ val ->
				if(v.contains(val)){
					if(!ioSet[k].contains(apiObject[val])){
						ioSet[k].add(apiObject[val])
					}
				}
			}
		}
		
		// add permitAll vars to other roles after processing
		ioSet.each(){ key, val ->
			if(key!='permitAll'){
				ioSet['permitAll'].each(){ it ->
						ioSet[key].add(it)
				}
			}
		}
		
		return ioSet
	}
	
	private ApiDescriptor createApiDescriptor(String apiname,String apiMethod, String apiDescription, List apiRoles, List batchRoles, String uri, JSONObject values, JSONObject json){
		LinkedHashMap<String,ParamsDescriptor> apiObject = [:]
		ApiParams param = new ApiParams()
		LinkedHashMap mocks = [
			"STRING":'Mock String',
			"DATE":'Mock Date',
			"LONG":999,
			"BOOLEAN":true,
			"FLOAT":0.01,
			"BIGDECIMAL":123456789,
			"EMAIL":'test@mockdata.com',
			"URL":'www.mockdata.com',
			"ARRAY":['this','is','mock','data']
		]
		
		List fkeys = []
		values.each{ k,v ->
			v.type = (v.references)?getKeyType(v.references, v.type):v.type
			if(v.type=='FKEY'){
				fkeys.add(k)	
			}
			
			String references = ''
			String hasDescription = ''
			String hasMockData = mocks[v.type]?mocks[v.type]:''

			param.setParam(v.type,k)
			
			def configType = grailsApplication.config.apitoolkit.apiobject.type."${v.type}"
			
			hasDescription = (configType?.description)?configType.description:hasDescription
			hasDescription = (v?.description)?v.description:hasDescription
			if(hasDescription){ param.hasDescription(hasDescription) }
			
			references = (configType?.references)?configType.references:""
			references = (v?.references)?v.references:references
			if(references){ param.referencedBy(references) }
			
			hasMockData = (v?.mockData)?v.mockData:hasMockData
			if(hasMockData){ param.hasMockData(hasMockData) }

			// collect api vars into list to use in apiDescriptor
			apiObject[param.param.name] = param.toObject()
		}
		
		LinkedHashMap receives = getIOSet(json.URI[uri]?.REQUEST,apiObject)
		LinkedHashMap returns = getIOSet(json.URI[uri]?.RESPONSE,apiObject)

		ApiDescriptor service = new ApiDescriptor(
			'method':"$apiMethod",
			'fkeys':fkeys,
			'description':"$apiDescription",
			'roles':[],
			'batchRoles':[],
			'doc':[:],
			'receives':receives,
			'returns':returns
		)
		
		service['roles'] = apiRoles
		service['batchRoles'] = batchRoles

		return service
	}
	
	Boolean parseJson(String apiName,JSONObject json){
		LinkedHashMap methods = [:]
		json.VERSION.each() { vers ->
			def versKey = vers.key
			String defaultAction = (vers.value.DEFAULTACTION)?vers.value.DEFAULTACTION:'index'
			List deprecated = (vers.value.DEPRECATED)?vers.value.DEPRECATED:[]
			String domainPackage = (vers.value.DOMAINPACKAGE!=null || vers.value.DOMAINPACKAGE?.size()>0)?vers.value.DOMAINPACKAGE:null

			vers.value.URI.each() { it ->
				def cache = (apiCacheService?.getApiCache(apiName))?apiCacheService.getApiCache(apiName):[:]
				methods['cacheversion'] = (!cache?.cacheversion)? 1 : cache['cacheversion']+1
				
				JSONObject apiVersion = json.VERSION[vers.key]
				
				//List temp = it.key.split('/')
				//String actionname = temp[1]
				String actionname = it.key
				
				ApiStatuses error = new ApiStatuses()
				
				ApiDescriptor apiDescriptor
				Map apiParams
				
				String apiMethod = it.value.METHOD
				String apiDescription = it.value.DESCRIPTION
				List apiRoles = it.value.ROLES
				List batchRoles = it.value.BATCH
				
				String uri = it.key
				apiDescriptor = createApiDescriptor(apiName, apiMethod, apiDescription, apiRoles, batchRoles, uri, json.get('VALUES'), apiVersion)
				if(!methods[vers.key]){
					methods[vers.key] = [:]
				}
				
				if(!methods['currentStable']){
					methods['currentStable'] = [:]
					methods['currentStable']['value'] = json.CURRENTSTABLE
				}
				if(!methods[vers.key]['deprecated']){
					methods[vers.key]['deprecated'] = []
					methods[vers.key]['deprecated'] = deprecated
				}
				
				if(!methods[vers.key]['defaultAction']){
					methods[vers.key]['defaultAction'] = defaultAction
				}

				if(!methods[vers.key]['domainPackage']){
					methods[vers.key]['domainPackage'] = domainPackage
				}

				methods[vers.key][actionname] = apiDescriptor

			}
			
			if(methods){
				apiCacheService.setApiCache(apiName,methods)

				def cache = apiCacheService.getApiCache(apiName)

				cache[vers.key].each(){ key1,val1 ->
					if(!['deprecated','defaultAction','domainPackage'].contains(key1)){
						apiCacheService.setApiCache(apiName,key1, val1, vers.key)
						//val.doc = apiCacheService.generateApiDoc(apiName, key, vers.key)
						//apiCacheService.resetApiCache(apiName,key,val)
					}
				}

			}
			
		}
		

	}

}
