package net.nosegrind.apiframework


import grails.dev.commands.*
import grails.core.GrailsApplication
import grails.util.Environment
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import org.hibernate.metadata.ClassMetadata

import java.util.regex.Matcher
import java.util.regex.Pattern

class ImportDomainCommand implements ApplicationCommand {


	@Autowired
	GrailsApplication grailsApplication
    String iostateDir = ""


    boolean handle(ExecutionContext ctx) {
        // SET IOSTATE FILES PATH
        switch(Environment.current){
            case Environment.DEVELOPMENT:
                iostateDir = grailsApplication.config.environments.development.iostate.preloadDir
                break
            case Environment.TEST:
                iostateDir = grailsApplication.config.environments.test.iostate.preloadDir
                break
            case Environment.PRODUCTION:
                iostateDir = grailsApplication.config.environments.production.iostate.preloadDir
                break
        }

        // GET BINDING VARIABLES
        def domains = grailsApplication.getArtefacts("Domain")
        domains.each(){ it ->
            String logicalName = it.getLogicalPropertyName()
            String packageName = it.getPackageName()
            String realName = it.getName()


            def sessionFactory = grailsApplication.mainContext.sessionFactory
            ClassMetadata hibernateMetaClass = sessionFactory.getClassMetadata(it.clazz)

            String[] keys = hibernateMetaClass.getKeyColumnNames()
            String values = """
\t\t\"id\": {
\t\t\t\"type\": \"PKEY\",
\t\t\t\"description\": \"Primary Key\"
\t\t},
"""
            String uris = "\r"
            def controller = grailsApplication.getArtefactByLogicalPropertyName('Controller', logicalName)

            if(controller){
                List actions = controller.actions as List



                def domainProperties = hibernateMetaClass.getPropertyNames()

                List variables = []
                variables.add("\"id\"")
                domainProperties.each() { it2 ->
                    List ignoreList = ['constrainedProperties','gormPersistentEntity','properties','async','gormDynamicFinders','all','attached','class','constraints','reports','dirtyPropertyNames','errors','dirty','transients','count']

                    String type = ""
                    if(!ignoreList.contains(it2)) {
                        String thisType = hibernateMetaClass.getPropertyType(it2).class as String
                        if (keys.contains(it2) || thisType=='class org.hibernate.type.ManyToOneType') {
                            type = 'FKEY'
                        } else {
                            type = getValueType(thisType)
                        }
                        String name = (type=='FKEY')?"${it2}Id".toString():it2
                        variables.add("\"${name}\"")
                        String value = """\t\t\"${name}\": {
\t\t\t\"type\": \"${type}\",
\t\t\t\"description\": \"Description for ${it2}\"
\t\t},
"""
                        values <<= value
                    }
                }
                //println values



                actions.each() { it4 ->
                    String method = ""
                    List req = []
                    List resp = []
                    Pattern getPattern = Pattern.compile("get|show|list|listBy")
                    Pattern postPattern = Pattern.compile("create|make|generate|build")
                    Pattern putPattern = Pattern.compile("edit|update")
                    Pattern deletePattern = Pattern.compile("delete|destroy|kill")


                    Matcher getm = getPattern.matcher(it4)
                    if (getm.find()) {
                        method = 'GET'
                        if (getm.group() == 'list') {
                            // request is empty
                            resp = variables
                        } else {
                            req.add('\"id\"')
                            resp = variables
                        }
                    }

                    if (method.isEmpty()) {
                        Matcher postm = postPattern.matcher(it4)
                        if (postm.find()) {
                            method = 'POST'
                            req = variables
                            resp.add('\"id\"')
                        }
                    }

                    if (method.isEmpty()) {
                        Matcher putm = postPattern.matcher(it4);
                        if (putm.find()) {
                            method = 'PUT'
                            req = variables
                            resp.add("id")
                        }
                    }

                    if (method.isEmpty()) {
                        Matcher delm = postPattern.matcher(it4);
                        if (delm.find()) {
                            method = 'DELETE'
                            req.add('\"id\"')
                            resp.add('\"id\"')
                        }
                    }
                    //response.collect{ '"' + it + '"'}
                    //request.collect{ '"' + it + '"'}

                    String uri = """
\t\t\t\t\"${it4}\": {
\t\t\t\t\t\"METHOD\": "${method}",
\t\t\t\t\t\"DESCRIPTION\": \"Description for ${it4}\",
\t\t\t\t    \t\"ROLES\": {
\t\t\t\t\t    \"DEFAULT\": [\"permitAll\"],
\t\t\t\t\t    \"BATCH\": [\"ROLE_ADMIN\"]
\t\t\t\t\t},
\t\t\t\t\t\"REQUEST\": {
\t\t\t\t\t\t\"permitAll\": ${req}
\t\t\t\t\t},
\t\t\t\t\t\"RESPONSE\": {
\t\t\t\t\t\t\"permitAll\": ${resp}
\t\t\t\t\t}
\t\t\t\t},
"""
                    uris <<= uri

                }
                //println(uris)
            }
            if(logicalName.length()>0 && values.length()>0 && uris.length()>1) {
                createTemplate(iostateDir, realName, logicalName, values, uris)
            }
        }
        return true
    }

    private String getValueType(String type){
        switch(type){
            case 'class org.hibernate.type.TextType':
            case 'class org.hibernate.type.StringType':
            case 'java.lang.String':
                return 'STRING'
                break
            case 'class org.hibernate.type.IntegerType':
            case 'class org.hibernate.type.LongType':
            case 'java.lang.Integer':
            case 'java.lang.Long':
                return 'LONG'
                break
            case 'class org.hibernate.type.BooleanType':
            case 'java.lang.Boolean':
                return 'BOOLEAN'
                break
            case 'class org.hibernate.type.DoubleType':
            case 'class org.hibernate.type.FloatType':
            case 'java.lang.Double':
            case 'java.lang.Float':
                return 'FLOAT'
                break
            case 'class org.hibernate.type.TimestampType':
            case 'class org.hibernate.type.DateType':
            case 'java.util.Date':
                return 'DATE'
                break
            case 'class org.hibernate.type.BigDecimalType':
            case 'java.math.BigDecimal':
                return 'BIGDECIMAL'
                break
            case 'class org.hibernate.type.MapType':
            case 'class java.util.HashMap':
            case 'java.util.LinkedHashMap':
                return 'ARRAY'
                break
            default:
                return 'COMPOSITE'
                break
        }
    }


    private void createTemplate(String iostateDir, String realName, String logicalName, String values, String uris){
        // MAKE SURE DIRECTORY EXISTS
        File ioFile = new File(iostateDir)
        if (ioFile.exists() && ioFile.isDirectory()) {
            String template = """{
\t\"NAME\": \"${logicalName}\",
\t\"VALUES\": { ${values}
\t},
\t\"CURRENTSTABLE\": \"1\",
\t\"VERSION\": {
\t\t\"1\": {
\t\t\t\"DEFAULTACTION\":\"list\",
\t\t\t\"URI\": { ${uris}

\t\t\t}
\t\t}
\t}
}
"""

            //String basedir = BuildSettings.BASE_DIR
            //def ant = new AntBuilder()
            //basedir = basedir.substring(0,basedir.length())

        try {
            //Whatever the file path is.
            String path = iostateDir+"/${realName}.json" as String
            File iostateFile = new File(path)

            FileOutputStream is = new FileOutputStream(iostateFile)
            OutputStreamWriter osw = new OutputStreamWriter(is)
            Writer w = new BufferedWriter(osw)
            w.write(template)
            w.close()

        } catch (IOException e) {
            println("Problem writing to the file ${path}")
        }


        }
    }


}
