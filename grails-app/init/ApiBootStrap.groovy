//package net.nosegrind.apiframework

import net.nosegrind.apiframework.*

class ApiBootStrap {

	def grailsApplication
	def apiObjectService
	ApiCacheService apiCacheService

	def init = { servletContext ->
		//apiObjectService.initialize()
	}
	
	def destroy = {}

}
