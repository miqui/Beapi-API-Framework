package net.nosegrind.apitoolkit


import java.io.Serializable;
import java.util.Date;


class Role implements Serializable {

	static transactional = true
	

	String authority

	static constraints = {
		authority blank: false, unique: true
	}
	

	static mapping = {
		version false
		datasource 'user'
	}

	
	/*
	static mapping = {
		version false
		table 'role'
		id column:'id'
	}
	*/


}
