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
	String name
	String idReferences
	String description = ""
	String mockData
	ParamsDescriptor[] values = []

	static constraints = { 
		paramType(nullable:false,maxSize:100,inList: ["PKEY","FKEY","INDEX","STRING","DATE","LONG","BOOLEAN","FLOAT","BIGDECIMAL","ARRAY","COMPOSITE"])
		name(nullable:false,maxSize:100)
		idReferences(maxSize:100, validator: { val, obj ->
			if (paramType!="PKEY" && paramType!="FKEY") {
			  return ['nullable']
			}else {
			  return true
			}
		})
		description(nullable:false,maxSize:1000)
		mockData(nullable:true)
		values(nullable:true)
	} 
}
