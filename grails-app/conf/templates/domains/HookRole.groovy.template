package net.nosegrind.apiframework

import java.util.Date

class HookRole {
	
	Hook hook
	Role role
	Date dateCreated
	Date lastModified = new Date()

	static mapping = {
		datasource 'user'
	}
	
	static constraints = {
		hook(nullable:false)
		role(nullable:false)
	}
}