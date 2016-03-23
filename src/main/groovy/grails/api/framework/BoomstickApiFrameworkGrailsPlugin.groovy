/*
 * The MIT License (MIT)
 * Copyright 2014 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright/trademark notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package grails.api.framework

import javax.servlet.ServletRegistration

import org.grails.web.servlet.mvc.GrailsDispatcherServlet
import grails.plugins.*
import grails.util.GrailsNameUtils
import grails.util.Metadata
import grails.util.BuildSettings

import java.util.jar.JarFile
import java.util.jar.JarException
import java.util.jar.JarEntry

class BoomstickApiFrameworkGrailsPlugin extends Plugin{
	def version = "0.1"
    def grailsVersion = "3.1.1 > *"
    def title = "Boomstick Api Framework" // Headline display name of the plugin
	def author = "Owen Rubel"
	def authorEmail = "orubel@gmail.com"
	def description = 'Boomstick API Framework is a fully reactive plug-n-play API Framework for Distributed Architectures providing api abstraction, cached IO state, automated batching and more. It is meant to autmoate alot of the issues behind setting up and maintaining API\'s in distributed architectures as well as handling and simplifying automation.'
	def documentation = "https://github.com/orubel/grails-api-toolkit-docs"
	def license = "MIT"
	def issueManagement = [system: 'GitHub', url: 'https://github.com/orubel/grails-api-toolkit-docs/issues']
	def scm = [url: 'https://github.com/orubel/api-framework']
	
	def dependsOn = [cache: "* > 3.0"]
	def loadAfter = ['cache']
    //def loadBefore = ['spring-boot-starter-tomcat']

    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]
    def profiles = ['web']
    def organization = [ name: "Nosegrind", url: "http://www.nosegrind.net/" ]

    def developers = [ [ name: "Owen Rubel", email: "orubel@gmail.com" ]]


    Closure doWithSpring() {
        { ->
            // TODO Implement runtime spring config (optional)
        }
    }

    def doWithDynamicMethods = { applicationContext ->
        // Configure servlets
        def config = getBean("grailsApplication").config
        def servletContext =  applicationContext.servletContext
        def serverInfo = servletContext.getServerInfo()



        config?.servlets?.each { name, parameters ->
            ServletRegistration servletRegistration = servletContext.addServlet(name, parameters.className)
            servletRegistration.addMapping(parameters.mapping)
            servletRegistration.setAsyncSupported(Boolean.TRUE)
            servletRegistration.setLoadOnStartup(1)

            servletRegistration.setInitParameter("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.JettyServlet30AsyncSupportWithWebSocket")

            def initParams = parameters.initParams
            if (initParams != "none") {
                initParams?.each { param, value ->
                    servletRegistration.setInitParameter(param, value)
                }
            }
        }
    }
	
    void doWithApplicationContext() {

        // Delegate OPTIONS requests to controllers
        applicationContext.dispatcherServlet.setDispatchOptionsRequest(true)

		String basedir = BuildSettings.BASE_DIR
		def ant = new AntBuilder()
		ant.mkdir(dir: "${basedir}/src/iostate")
		ant.mkdir(dir: "${System.properties.'user.home'}/.iostate")

        //def ctx = applicationContext.getServletContext()
        //ctx.setInitParameter("dispatchOptionsRequest", "true");


		doInitApiFrameworkInstall(applicationContext)
    }


	void doInitApiFrameworkInstall(applicationContext) {
		//String basedir = applicationContext.getResource("../../..").getFile().path
		String basedir = BuildSettings.BASE_DIR
        def ant = new AntBuilder()
		//basedir = basedir.substring(0,basedir.length())

        println "### Installing API Framework ..."

        def iostateDir = "${basedir}/src/iostate/"
        def iofile = new File(iostateDir)
        if(!iofile.exists()) {
            writeFile("templates/iostate/Apidoc.json.template", "${iostateDir}Apidoc.json")
            writeFile("templates/iostate/Hook.json.template", "${iostateDir}Hook.json")
            writeFile("templates/iostate/IOState.json.template", "${iostateDir}IOState.json")
            println " ... installing IOstate dir/files ..."
        }

        def contDir = "${basedir}/grails-app/controllers/net/nosegrind/apiframework/"
        def cfile = new File(contDir)
        if(!cfile.exists()) {
            ant.mkdir(dir: contDir)
            writeFile("templates/controllers/ApidocController.groovy.template", "${contDir}ApidocController.groovy")
            writeFile("templates/controllers/HookController.groovy.template", "${contDir}HookController.groovy")
            writeFile("templates/controllers/IostateController.groovy.template", "${contDir}IostateController.groovy")
            println " ... installing Controller dir/files ..."
        }

        def domainDir = "${basedir}/grails-app/domain/net/nosegrind/apiframework/"
        def dfile = new File(domainDir)
        if(!dfile.exists()) {
            writeFile("templates/domains/Hook.groovy.template", "${domainDir}Hook.groovy")
            writeFile("templates/domains/HookRole.groovy.template", "${domainDir}HookRole.groovy")
            writeFile("templates/domains/Role.groovy.template", "${domainDir}Role.groovy")
            println " ... installing Domain dir/files ..."
        }



        if(!grailsApplication.config.apitoolkit){
            println " ... updating config ..."
            file('grails-app/conf/application.groovy').withWriterAppend { BufferedWriter writer ->
                writer.newLine()
                writer.newLine()
                writer.writeLine '// Added by the Reactive API Framework plugin:'

                writer.writeLine "apitoolkit.attempts= 5"
                writer.writeLine "apitoolkit.roles= ['ROLE_USER','ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']"
                writer.writeLine "apitoolkit.chaining.enabled= true"
                writer.writeLine "apitoolkit.batching.enabled= true"
                writer.writeLine "apitoolkit.encoding= 'UTF-8'"
                writer.writeLine "apitoolkit.user.roles= ['ROLE_USER']"
                writer.writeLine "apitoolkit.admin.roles= ['ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']"
                writer.writeLine "apitoolkit.serverType= 'master'"
                writer.writeLine "apitoolkit.webhook.services= ['iostate']"
                // set this per environment
                //writer.writeLine "apitoolkit.iostate.preloadDir= '/user/home/.iostate'"
                writer.writeLine "apitoolkit.corsInterceptor.includeEnvironments= ['development','test']"
                writer.writeLine "apitoolkit.corsInterceptor.excludeEnvironments= ['production']"
                writer.writeLine "apitoolkit.corsInterceptor.allowedOrigins= ['localhost:3000']"

                writer.newLine()
            }
        }

        final String isBatchServer = grailsApplication.config.apitoolkit.batching.enabled
        final String isChainServer = grailsApplication.config.apitoolkit.chaining.enabled
        //final String isLocalAuth = (String)grailsApplication.config.apitoolkit.localauth.enabled

        System.setProperty('isBatchServer', isBatchServer)
        System.setProperty('isChainServer', isChainServer)
        //System.setProperty('isLocalAuth', isLocalAuth)

        println  "... API Framework installed. ###"
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
