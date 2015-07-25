description( "Initialize API Framework" ) {
	//flag name:'force', description:"Whether to overwrite existing files"
}

// create IO State Objects
render  template:"/templates/iostate/Hook.json",destination: file("grails-app/src/iostate/Hook.json"),model: model
render  template:"/templates/iostate/IOState.json",destination: file("grails-app/src/iostate/IOState.json"),model: model

// create domains
render  template:"/templates/domains/Hook.groovy",destination: file("grails-app/domain/Hook.groovy"),model: model
render  template:"/templates/domains/HookRole",destination: file("grails-app/domain/HookRole.groovy"),model: model
render  template:"/templates/domains/Role",destination: file("grails-app/domain/Role.groovy"),model: model

// create controllers
render  template:"/templates/controllers/HookController.groovy",destination: file("grails-app/controllers/HookController.groovy"),model: model
render  template:"/templates/controllers/IostateController.groovy",destination: file("grails-app/controllers/IostateController.groovy"),model: model



printMessage """
*************************************************************
* SUCCESS! API Framework is now installed. Please see           *
* documentation page on implementation details.                 *
*************************************************************
"""

