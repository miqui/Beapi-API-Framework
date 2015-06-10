package grails.api.framework

import grails.plugins.*
import grails.util.GrailsNameUtils
import grails.util.Metadata

class GrailsApiFrameworkGrailsPlugin extends Plugin {
	def version = "0.1"
    def grailsVersion = "3.0.1 > *"
    def title = "Grails Api Framework" // Headline display name of the plugin
	def author = "Owen Rubel"
	def authorEmail = "orubel@gmail.com"
	def description = 'API Framework for Distributed Architectures providing api abstraction and cached IO state'
	def documentation = "https://github.com/orubel/grails-api-toolkit-docs"
	def license = "Apache"
	def issueManagement = [system: 'GitHub', url: 'https://github.com/orubel/grails-api-toolkit-docs/issues']
	def scm = [url: 'https://github.com/orubel/grails-api-toolkit']
	

    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]
    def profiles = ['web']
    def organization = [ name: "Nosegrind", url: "http://www.nosegrind.net/" ]

    def developers = [ [ name: "Owen Rubel", email: "orubel@gmail.com" ]]


    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        } 
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
		ant.mkdir(dir: "${basedir}/src/apiObject")
		ant.mkdir(dir: "${System.properties.'user.home'}/.iostate")
		
		def configFile = new File("${basedir}/grails-app", 'conf/Config.groovy')
		if (configFile.exists()) {
			configFile.withWriterAppend {
				it.writeLine """
		###Added by the Api Toolkit plugin
		apitoolkit.apiName = 'api'
		apitoolkit.apichain.limit=3
		apitoolkit.rest.postcrement=false
		apitoolkit.attempts = 5
		apitoolkit.chaining.enabled=true
		apitoolkit.batching.enabled=true
		apitoolkit.roles = ['ROLE_USER','ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']
		apitoolkit.user.roles = ['ROLE_USER']
		apitoolkit.admin.roles = ['ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']
		
		apitoolkit.apiobject.type = [
			"PKEY":["type":"Long","references":"self","description":"Primary Key","mockData":"1"],
			"FKEY":["type":"Long","description":"","mockData":"1"],
			"INDEX":["type":"String","references":"self","description":"Foreign Key","mockData":"1"],
			"String":["type":"String","description":"String","mockData":"mockString"],
			"Date":["type":"String","description":"String","mockData":"1970-01-01 00:00:01"],
			"Long":["type":"Long","description":"Long","mockData":"1234"],
			"Boolean":["type":"Boolean","description":"Boolean","mockData":"true"],
			"Float":["type":"Float","description":"Floating Point","mockData":"0.01"],
			"BigDecimal":["type":"BigDecimal","description":"Big Decimal","mockData":"1234567890"],
			"URL":["type":"URL","description":"URL","mockData":"www.mockdata.com"],
			"Email":["type":"Email","description":"Email","mockData":"test@mockdata.com"],
			"Array":["type":"Array","description":"Array","mockData":["this","is","mockdata"]],
			"Composite":["type":"Composite","description":"Composite","mockData":["type":"Composite","description":"this is a composite","List":[1,2,3,4,5]]]
		]
				"""
				
			}
			
			println """
			************************************************************
			* SUCCESS! You have successfully installed the API Toolkit *
			************************************************************
		"""
		}
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
