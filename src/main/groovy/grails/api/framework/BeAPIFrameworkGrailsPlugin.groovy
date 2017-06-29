/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

package grails.api.framework

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.SecurityFilterPosition
import net.nosegrind.apiframework.ApiDescriptor
import net.nosegrind.apiframework.ApiParams
import net.nosegrind.apiframework.ParamsDescriptor

import javax.servlet.ServletRegistration

import org.grails.web.servlet.mvc.GrailsDispatcherServlet

import grails.plugins.*

import grails.util.GrailsNameUtils
import grails.util.Metadata
import grails.util.BuildSettings
import grails.util.Holders

import grails.converters.JSON
import org.grails.web.json.JSONObject

import org.springframework.context.ApplicationContext

import java.net.URL

import java.util.jar.JarFile
import java.util.jar.JarException
import java.util.jar.JarEntry

class BoomstickApiFrameworkGrailsPlugin extends Plugin{
	def version = "0.9"
    def grailsVersion = "3.2.9 > *"
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

    Closure doWithSpring() { { ->
        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.active) {
            return
        }

        SpringSecurityUtils.loadSecondaryConfig 'DefaultRestSecurityConfig'
        conf = SpringSecurityUtils.securityConfig

            /* restTokenValidationFilter */
            SpringSecurityUtils.registerFilter 'tokenCacheValidationFilter', SecurityFilterPosition.PRE_AUTH_FILTER.order + 1
            SpringSecurityUtils.registerFilter 'corsSecurityFilter', SecurityFilterPosition.PRE_AUTH_FILTER.order + 2
            SpringSecurityUtils.registerFilter 'contentTypeMarshallerFilter', SecurityFilterPosition.PRE_AUTH_FILTER.order + 3




            corsSecurityFilter(grails.api.framework.CorsSecurityFilter){}

            tokenCacheValidationFilter(grails.api.framework.TokenCacheValidationFilter) {
                headerName = conf.rest.token.validation.headerName
                validationEndpointUrl = conf.rest.token.validation.endpointUrl
                active = conf.rest.token.validation.active
                tokenReader = ref('tokenReader')
                enableAnonymousAccess = conf.rest.token.validation.enableAnonymousAccess
                authenticationSuccessHandler = ref('restAuthenticationSuccessHandler')
                authenticationFailureHandler = ref('restAuthenticationFailureHandler')
                restAuthenticationProvider = ref('restAuthenticationProvider')
                authenticationEventPublisher = ref('authenticationEventPublisher')
            }
            contentTypeMarshallerFilter(grails.api.framework.ContentTypeMarshallerFilter){}
    } }

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

            // servletRegistration.setInitParameter("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.JettyServlet30AsyncSupportWithWebSocket")

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
        String apiObjectSrc = grails.util.Holders.grailsApplication.config.iostate.preloadDir
        parseFiles(apiObjectSrc.toString(), applicationContext)
    }

    private parseFiles(String path, ApplicationContext applicationContext){
        LinkedHashMap methods = [:]

        println "### Loading IO State Files ..."

        try {
            new File(path).eachFile() { file ->
                String fileName = file.name.toString()
                //println(fileName+" ...")
                def tmp = fileName.split('\\.')
                String fileChar = fileName.charAt(fileName.length() - 1)

                if (tmp[1] == 'json' && fileChar == "n") {
                    //try{
                    JSONObject json = JSON.parse(file.text)
                    methods[json.NAME.toString()] = parseJson(json.NAME.toString(), json, applicationContext)
                    //}catch(Exception e){
                    //    throw new Exception("[ApiObjectService :: initialize] : Unacceptable file '${file.name}' - full stack trace follows:",e)
                    //}
                }
            }
        }catch(Exception e){
            throw new Exception("[BeAPIFramework] : No IO State Files found for initialization :",e)
        }
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
            println " ... installing IO state dir/files ..."
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
            String groovyConf = "${basedir}/grails-app/conf/application.groovy"
            def confFile = new File(groovyConf)
            confFile.withWriterAppend { BufferedWriter writer ->
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
               	writer.writeLine "apitoolkit.iostate.preloadDir= '"+System.getProperty('user.home')+"/.iostate'"
                writer.writeLine "apitoolkit.corsInterceptor.includeEnvironments= ['development','test']"
                writer.writeLine "apitoolkit.corsInterceptor.excludeEnvironments= ['production']"
                writer.writeLine "apitoolkit.corsInterceptor.allowedOrigins= ['localhost:3000']"

                writer.newLine()
            }
        }

        String isBatchServer = grailsApplication.config.apitoolkit.batching.enabled
        String isChainServer = grailsApplication.config.apitoolkit.chaining.enabled
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
            println("Exception :"+e)
        }
	}

    LinkedHashMap parseJson(String apiName,JSONObject json, ApplicationContext applicationContext){
        def apiCacheService = applicationContext.getBean("apiCacheService")
        LinkedHashMap methods = [:]
        json.VERSION.each() { vers ->
            def versKey = vers.key
            String defaultAction = (vers.value['DEFAULTACTION'])?vers.value.DEFAULTACTION:'index'
            List deprecated = (vers.value.DEPRECATED)?vers.value.DEPRECATED:[]
            String domainPackage = (vers.value.DOMAINPACKAGE!=null || vers.value.DOMAINPACKAGE?.size()>0)?vers.value.DOMAINPACKAGE:null

            /*
            URL url = this.getClass().getClassLoader().getResource("ehcache.xml")
            System.out.println(this.getClass().getResource("ehcache.xml"));
            System.out.println(this.getClass().getClassLoader().getResource("ehcache.xml"));
            CacheManager manager = new CacheManager(url);
            */

            //CacheManager ehcacheManager = new CacheManager(new ClassPathResource("ehcache.xml").getInputStream());
            //EhCacheCacheManager manager = new EhCacheCacheManager();
            //manager.setCacheManager(ehcacheManager);
            //Cache cache = manager.getCache("ApiCache");

            String actionname
            vers.value.URI.each() { it ->

                def cache = apiCacheService.getApiCache(apiName.toString())
                //def cache = (temp?.get(apiName))?temp?.get(apiName):[:]

                methods['cacheversion'] = 1

                JSONObject apiVersion = json.VERSION[vers.key]

                actionname = it.key

                ApiDescriptor apiDescriptor
                //Map apiParams

                String apiMethod = it.value.METHOD
                String apiDescription = it.value.DESCRIPTION
                //List apiRoles = it.value.ROLES
                //List batchRoles = it.value.BATCH

                List apiRoles = it.value.ROLES.DEFAULT
                List batchRoles = it.value.ROLES.BATCH
                List hookRoles = it.value.ROLES.HOOK

                String uri = it.key
                apiDescriptor = createApiDescriptor(apiName, apiMethod, apiDescription, apiRoles, batchRoles, hookRoles, uri, json.get('VALUES'), apiVersion)
                if(!methods[vers.key]){
                    methods[vers.key] = [:]
                }

                if(!methods['currentStable']){
                    methods['currentStable'] = [:]
                    methods['currentStable']['value'] = json.CURRENTSTABLE
                }
                if(!methods[vers.key]['deprecated']){
                    methods[vers.key]['deprecated'] = []
                    methods[vers.key]['deprecated'] = deprecated
                }

                if(!methods[vers.key]['defaultAction']){
                    methods[vers.key]['defaultAction'] = defaultAction
                }


                methods[vers.key][actionname] = apiDescriptor

            }

            if(methods){
                def cache = apiCacheService.setApiCache(apiName,methods)

                cache[vers.key].each(){ key1,val1 ->

                    if(!['deprecated','defaultAction'].contains(key1)){
                        apiCacheService.setApiCache(apiName,key1, val1, vers.key)
                    }
                }

            }

        }
        return methods
    }

    LinkedHashMap generateApiDoc(String controllername, String actionname, String apiversion){
        try{
            LinkedHashMap doc = [:]

            //URL url = this.getClass().getClassLoader().getResource("ehcache.xml")
            //System.out.println(this.getClass().getResource("ehcache.xml"));
            // System.out.println(this.getClass().getClassLoader().getResource("ehcache.xml"));
            //CacheManager manager = new CacheManager(url);
            //def temp = manager.getCache("ApiCache");
            //def cache = (temp?.get(controllername))?temp?.get(controllername):[:]

            def cache = apiCacheService.getApiCache(controllername.toString())

            String apiPrefix = "v${Metadata.current.getApplicationVersion()}"

            if(cache){
                String path = "/${apiPrefix}-${apiversion}/${controllername}/${actionname}"
                doc = ['path':path,'method':cache[apiversion][actionname]['method'],'description':cache[apiversion][actionname]['description']]
                if(cache[apiversion][actionname]['receives']){

                    doc['receives'] = [:]
                    for(receiveVal in cache[apiversion][actionname]['receives']){
                        if(receiveVal?.key) {
                            doc['receives']["$receiveVal.key"] = receiveVal.value
                        }
                    }
                }

                if(cache[apiversion][actionname]['returns']){
                    doc['returns'] = [:]
                    for(returnVal in cache[apiversion][actionname]['returns']){
                        if(returnVal?.key) {
                            doc['returns']["$returnVal.key"] = returnVal.value
                        }
                    }

                    doc['json'] = [:]
                    doc['json'] = processJson(doc["returns"])
                }

            }
            return doc
        }catch(Exception e){
            throw new Exception("[ApiCacheService :: generateApiDoc] : Exception - full stack trace follows:",e)
        }
    }

    private ApiDescriptor createApiDescriptor(String apiname,String apiMethod, String apiDescription, List apiRoles, List batchRoles, List hookRoles, String uri, JSONObject values, JSONObject json){
        LinkedHashMap<String,ParamsDescriptor> apiObject = [:]
        ApiParams param = new ApiParams()
        LinkedHashMap mocks = [
                "STRING":'Mock String',
                "DATE":'Mock Date',
                "LONG":999,
                "BOOLEAN":true,
                "FLOAT":0.01,
                "BIGDECIMAL":123456789,
                "EMAIL":'test@mockdata.com',
                "URL":'www.mockdata.com',
                "ARRAY":['this','is','mock','data']
        ]

        List fkeys = []
        values.each{ k,v ->
            v.type = (v.references)?getKeyType(v.references, v.type):v.type
            if(v.type=='FKEY'){
                fkeys.add(k)
            }

            String references = ''
            String hasDescription = ''
            String hasMockData = mocks[v.type]?mocks[v.type]:''

            param.setParam(v.type,k)

            def configType = grailsApplication.config.apitoolkit.apiobject.type."${v.type}"

            hasDescription = (configType?.description)?configType.description:hasDescription
            hasDescription = (v?.description)?v.description:hasDescription
            if(hasDescription){ param.hasDescription(hasDescription) }

            references = (configType?.references)?configType.references:""
            references = (v?.references)?v.references:references
            if(references){ param.referencedBy(references) }

            hasMockData = (v?.mockData)?v.mockData:hasMockData
            if(hasMockData){ param.hasMockData(hasMockData) }

            // collect api vars into list to use in apiDescriptor
            apiObject[param.param.name] = param.toObject()
        }

        LinkedHashMap receives = getIOSet(json.URI[uri]?.REQUEST,apiObject)
        LinkedHashMap returns = getIOSet(json.URI[uri]?.RESPONSE,apiObject)

        ApiDescriptor service = new ApiDescriptor(
                'empty':false,
                'method':"$apiMethod",
                'fkeys':fkeys,
                'description':"$apiDescription",
                'roles':[],
                'batchRoles':[],
                'hookRoles':[],
                'doc':[:],
                'receives':receives,
                'returns':returns
        )

        service['roles'] = apiRoles
        service['batchRoles'] = batchRoles
        service['hookRoles'] = hookRoles

        return service
    }

    private LinkedHashMap getIOSet(JSONObject io,LinkedHashMap apiObject){
        LinkedHashMap<String,ParamsDescriptor> ioSet = [:]

        io.each{ k, v ->
            // init
            if(!ioSet[k]){ ioSet[k] = [] }

            def roleVars=v.toList()
            roleVars.each{ val ->
                if(v.contains(val)){
                    if(!ioSet[k].contains(apiObject[val])){
                        ioSet[k].add(apiObject[val])
                    }
                }
            }
        }

        // add permitAll vars to other roles after processing
        def permitAll = ioSet['permitAll']
        ioSet.each(){ key, val ->
            if(key!='permitAll'){
                permitAll.each(){ it ->
                    ioSet[key].add(it)
                }
            }
        }
        return ioSet
    }

    public List getApiParams(LinkedHashMap definitions){
        try{
            traceService.startTrace('ProfilerCommProcess','getApiParams')
            List apiList = []
            definitions.each(){ key, val ->
                if (request.isUserInRole(key) || key == 'permitAll') {
                    val.each(){ it2 ->
                        apiList.add(it2.name)
                    }
                }
            }
            traceService.endTrace('ProfilerCommProcess','getApiParams')
            return apiList
        }catch(Exception e){
            throw new Exception("[ParamsService :: getApiParams] : Exception - full stack trace follows:",e)
        }
    }
}
