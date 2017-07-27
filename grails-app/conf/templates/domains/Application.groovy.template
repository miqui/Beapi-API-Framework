package net.nosegrind.apiframework

class Application implements Serializable {

	String appName
	Account acct
	boolean enabled=true

	static constraints = {
		appName blank: false, unique: true
		acct blank: false
	}

	static mapping = {
		cache true
	}

}
