package net.nosegrind.apiframework

class AppToken implements Serializable {

	Application app
	String tokenValue

	static constraints = {
		app blank: false, unique: true
		tokenValue blank: false, unique: true
	}

	static mapping = {
		cache true
	}

}
