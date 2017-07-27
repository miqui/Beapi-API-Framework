package net.nosegrind.apiframework

class Account implements Serializable {

	String acctName
	boolean enabled=true

	static constraints = {
		acctName blank: false, unique: true
	}

	static mapping = {
		cache true
	}

}
