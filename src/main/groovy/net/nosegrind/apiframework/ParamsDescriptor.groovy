/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/
package net.nosegrind.apiframework;


import grails.validation.Validateable
//import grails.compiler.GrailsCompileStatic
//import groovy.transform.TypeCheckingMode



//@Validateable
//@GrailsCompileStatic
class ParamsDescriptor implements Validateable {

	String paramType
	String name
	String idReferences
	String description = ""
	String mockData
	ParamsDescriptor[] values = []

	static constraints = { 
		paramType(nullable:false,maxSize:100,inList: ["PKEY","FKEY","INDEX","STRING","DATE","LONG","BOOLEAN","FLOAT","BIGDECIMAL","EMAIL","URL","ARRAY","COMPOSITE"])
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