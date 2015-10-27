package net.nosegrind.apiframework

import org.grails.web.json.JSONObject

class IostateController {

	def springSecurityService
	def apiObjectService
	def apiCacheService
	
	/*
	* list name of collections of iostate
	*/
	def list() {
		if(isSuperuser()){
			List temp = apiCacheService.getCacheNames()
			List cacheNames = []
			temp.each(){
				if(!['hook','iostate'].contains(it)){
					 cacheNames.add(it)
				}
			}
			LinkedHashMap model = [name:cacheNames]
			respond model as Object
			return null
		}
	}

	def show() {
		if(isSuperuser()){
	
		}
	}

	def update() {
		if(isSuperuser()){
		    def file = request.getFile('iostate')
		    
		    if (file.empty) {
		        render(status:HttpServletResponse.SC_BAD_REQUEST)
		        return null
		    }
		    
			JSONObject json = JSON.parse(file.text)
			if(!apiObjectService.parseFile(json)){
				render(status:HttpServletResponse.SC_BAD_REQUEST)
				return null
			}
			
			def cache = apiCacheService.getApiCache(json.NAME)
			
			LinkedHashMap model = [name:cache.name,cacheversion:cache.cacheversion]
			webhookService.postData('Iostate', model,'update')
			respond model as Object
			return null
		}
	}


	protected boolean isSuperuser() {
		springSecurityService.principal.authorities*.authority.any { grailsApplication.config.apitoolkit.admin.roles.contains(it) }
	}
}
