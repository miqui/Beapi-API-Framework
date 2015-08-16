package grails.api.framework

import grails.plugins.*
import grails.util.GrailsNameUtils
import grails.util.Metadata
import grails.util.BuildSettings

import java.util.jar.JarFile
import java.util.jar.JarException
import java.util.jar.JarEntry

class GrailsApiFrameworkGrailsPlugin extends Plugin{
	def version = "0.1"
    def grailsVersion = "3.0.1 > *"
    def title = "Grails Api Framework" // Headline display name of the plugin
	def author = "Owen Rubel"
	def authorEmail = "orubel@gmail.com"
	def description = 'API Framework for Distributed Architectures providing api abstraction and cached IO state'
	def documentation = "https://github.com/orubel/grails-api-toolkit-docs"
	def license = "Apache"
	def issueManagement = [system: 'GitHub', url: 'https://github.com/orubel/grails-api-toolkit-docs/issues']
	def scm = [url: 'https://github.com/orubel/grails-api-toolkit']
	
	def dependsOn = [cache: "* > 3.0"]
	def loadAfter = ['cache']
	
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]
    def profiles = ['web']
    def organization = [ name: "Nosegrind", url: "http://www.nosegrind.net/" ]

    def developers = [ [ name: "Owen Rubel", email: "orubel@gmail.com" ]]


    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        } 
    }

	def doWithDynamicMethods = {
	}
	
    void doWithApplicationContext() {
		String basedir = BuildSettings.BASE_DIR
		def ant = new AntBuilder()
		ant.mkdir(dir: "${basedir}/src/iostate")
		ant.mkdir(dir: "${System.properties.'user.home'}/.iostate")
		doInitApiFrameworkInstall(applicationContext)
    }

	void doInitApiFrameworkInstall(applicationContext) {
		//String basedir = applicationContext.getResource("../../..").getFile().path
		String basedir = BuildSettings.BASE_DIR
		//basedir = basedir.substring(0,basedir.length())

		writeFile("templates/iostate/Hook.json.template","${basedir}/src/iostate/Hook.json")
		writeFile("templates/iostate/IOState.json.template","${basedir}/src/iostate/IOState.json")
		
		writeFile("templates/controllers/HookController.groovy.template","${basedir}/grails-app/controllers/HookController.groovy")
		writeFile("templates/controllers/IostateController.groovy.template","${basedir}/grails-app/controllers/IostateController.groovy")
		
		writeFile("templates/domains/Hook.groovy.template","${basedir}/grails-app/domain/Hook.groovy")
		writeFile("templates/domains/HookRole.groovy.template","${basedir}/grails-app/domain/HookRole.groovy")
		writeFile("templates/domains/Role.groovy.template","${basedir}/grails-app/domain/Role.groovy")
	}
	
    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
	
	void writeFile(String inPath, String outPath){
		String pluginDir = new File(getClass().protectionDomain.codeSource.location.path).path
		def plugin = new File(pluginDir)
        try {
            if (plugin.isFile() && plugin.name.endsWith("jar")) {
                JarFile jar = new JarFile(plugin)

                JarEntry entry = jar.getEntry(inPath)
                InputStream inStream = jar.getInputStream(entry);
                OutputStream out = new FileOutputStream(outPath);
                int c;
                while ((c = inStream.read()) != -1) {
                    out.write(c);
                }
                inStream.close();
                out.close();

                jar.close();
            }
        }catch(Exception e){
            println(e)
        }
	}
}
