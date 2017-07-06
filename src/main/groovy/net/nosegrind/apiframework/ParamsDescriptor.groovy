/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

package net.nosegrind.apiframework;

import grails.validation.Validateable
//import grails.compiler.GrailsCompileStatic

//@GrailsCompileStatic
class ParamsDescriptor implements Validateable {

	String paramType
	String keyType
	String name
	String idReferences
	String description = ""
	String mockData
	ParamsDescriptor[] values = []

	static constraints = { 
		paramType(nullable:false,maxSize:100,inList: ["STRING","DATE","LONG","BOOLEAN","FLOAT","BIGDECIMAL","MAP","COMPOSITE"])
		keyType(nullable:true,maxSize:100,inList: ["PRIMARY","FOREIGN","INDEX"])
		name(nullable:false,maxSize:100)
		idReferences(maxSize:100, validator: { val, obj ->
			if(keyType['FOREIGN','PRIMARY','INDEX'].contains(keyType)) {
			  return true
			}else {
			  return ['nullable']
			}
		})
		description(nullable:false,maxSize:1000)
		mockData(nullable:false)
		values(nullable:true)
	} 
}
