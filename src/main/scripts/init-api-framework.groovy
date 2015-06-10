description( "Initialize API Framework" ) {
	usage "grails init-apitoolkit [SERVER TYPE] [PACKAGE NAME] [USER CLASS NAME] [ROLE CLASS NAME]"
	argument name:'Server Type', description:"Whether Server is Master/Slave; single instance api is always master"
	argument name:'Package Name', description:"User Domain Package"
	argument name:'User Class Name', description:"Class name for User domain"
	argument name:'Role Class Name', description:"Class name for Role domain"
	//flag name:'force', description:"Whether to overwrite existing files"
}

String templateDir = "$apiToolkitPluginDir/src/templates/"
String appDir = "$basedir/grails-app"

model = args[0]
copyControllersAndViews()
switch(args[0]){
	case 'master':
		createDomains()
		copyControllersAndViews()
		updateMasterConfig()
		break;
	case 'slave':
		updateSlaveConfig()
		break;
}


printMessage """
*************************************************************
* SUCCESS! API Slave is now installed. Please see           *
* documentation page on implementation details.             *
*************************************************************
"""

// Update Config for API Slave Setup
void updateSlaveConfig() {
	ant.mkdir dir: "${userHome}/.iostate"
	def configFile = new File(appDir, 'conf/Config.groovy')
	if (configFile.exists()) {
		configFile.withWriterAppend {
			it.writeLine '\n// Added by the Api Toolkit plugin:'
			it.writeLine ' '
			it.writeLine "apitoolkit.serverType='slave'"
			it.writeLine "apitoolkit.master='127.0.0.1:8080/hook'"
			it.writeLine "apitoolkit.master.hooks=[]"
		}
	}
}

// Update Config for API Master Setup
void updateMasterConfig() {
	ant.mkdir dir: "${userHome}/.iostate"
	def configFile = new File(appDir, 'conf/Config.groovy')
	if (configFile.exists()) {
		configFile.withWriterAppend {
			it.writeLine '\n// Added by the Api Toolkit plugin:'
			it.writeLine ' '
			it.writeLine "apitoolkit.webhook.domain = '${packageName}.Hook'"
			it.writeLine "apitoolkit.webhook.controller = '${packageName}.HookController'"
			it.writeLine " "
			it.writeLine "apitoolkit.serverType='master'"
			it.writeLine "apitoolkit.iostate.preloadDir=\"${userHome}/.iostate\""
		}
	}
}

// Create API WebHook Domains
void createDomains() {
	String dir = packageToDir(packageName)
	generateFile "$templateDir/webhook/Hook.groovy.template", "$appDir/domain/${dir}Hook.groovy"
	generateFile "$templateDir/webhook/HookRole.groovy.template", "$appDir/domain/${dir}HookRole.groovy"
	printMessage "Domains created..."
}

// Create API Controllers and Views for webhooks
void copyControllersAndViews() {
	ant.mkdir dir: "$appDir/views/hook"
	// add default views for webhooks administration
	copyFile "$templateDir/webhook/create.gsp.template", "$appDir/views/hook/create.gsp"
	copyFile "$templateDir/webhook/edit.gsp.template", "$appDir/views/hook/edit.gsp"
	copyFile "$templateDir/webhook/list.gsp.template", "$appDir/views/hook/list.gsp"
	copyFile "$templateDir/webhook/show.gsp.template", "$appDir/views/hook/show.gsp"

	String dir2 = packageToDir(packageName)
	generateFile "$templateDir/webhook/HookController.groovy.template", "$appDir/controllers/${dir2}HookController.groovy"
	generateFile "$templateDir/iostate/IostateController.groovy.template", "$appDir/controllers/${dir2}IostateController.groovy"
	printMessage "Controller / Views created..."
}