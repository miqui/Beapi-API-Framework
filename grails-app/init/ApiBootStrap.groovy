//package net.nosegrind.apiframework

import net.nosegrind.apiframework.*

class ApiBootStrap {
	
	def grailsApplication
	def apiObjectService
	def apiCacheService

	def init = { servletContext ->
		Person user = Person.findByUsername("${grailsApplication.config.root.login}")
		boolean hasAuthority = false
		Set authorities = user.getAuthorities()
		String userRole = ''
		
		grailsApplication.config.apitoolkit.roles.each(){
			Authority rootRole = Authority.findByAuthority(it)?: new Authority(authority:it).save(faileOnError:true)
			if(authorities.contains(it)){
				hasAuthority = true
				userRole = it
			}
		}

		PersonAuthority.withTransaction(){ status ->

			Authority adminRole = Authority.findByAuthority(userRole)
			if(!user?.id){
				user = new Person(username:"${grailsApplication.config.root.login}",password:"${grailsApplication.config.root.password}",email:"${grailsApplication.config.root.email}")
				user.save(flush:true,failOnError:true)
			}else{
				user.password = "${grailsApplication.config.root.password}"
				user.save(flush:true,failOnError:true)
			}
		
			if(!user?.authorities?.contains(adminRole)){
				PersonRole.create user,adminRole
			}
			
			status.isCompleted()
		}
		
		apiObjectService.initialize()
	}
	
	def destroy = {}

}
