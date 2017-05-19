package net.nosegrind.apiframework


import grails.dev.commands.*
import grails.core.GrailsApplication
import grails.util.Environment
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import org.hibernate.metadata.ClassMetadata

class ImportDomainCommand implements ApplicationCommand {


	@Autowired
	GrailsApplication grailsApplication
    String iostateDir = ""


    String jsonMethod = """
"<%= methodName %>":{"METHOD":"<%= requestMethod %>","DESCRIPTION":"<%= methodDesc %>",
    "ROLES":{"DEFAULT":["ROLE_ADMIN"],"BATCH":["ROLE_ADMIN"]},
    "REQUEST": {"permitAll":[]},
    "RESPONSE": {"permitAll":[]}
},
"""
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
            String name = it.name
            String logicalName = it.getLogicalPropertyName()
            String packageName = it.getPackageName()


            def sessionFactory = grailsApplication.mainContext.sessionFactory
            ClassMetadata hibernateMetaClass = sessionFactory.getClassMetadata(it.clazz)

            String[] keys = hibernateMetaClass.getKeyColumnNames()

            def controller = grailsApplication.getArtefactByLogicalPropertyName('Controller', logicalName)
            if(controller){

                def domainProperties = hibernateMetaClass.getPropertyNames()
                String values = """      "id": {
            "type": "PKEY",
            "description":"Primary Key"
        },
"""
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
                        String value = """      "${name}": {
            "type": "${type}",
            "description":"Description for ${it2}"
        },
"""
                        values += value
                    }
                }
                println values
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
                println("#### getValueType > "+type)
                return 'COMPOSITE'
                break
        }
    }

    private void createTemplate(String logicalPropertyName, String values, String uris){
        String jsonTemplate = ""


        // 2. use base JSON template
        // 3. fill-in base JSON template
        // 4. write to file

        // MAKE SURE DIRECTORY EXISTS
        File ioFile = new File(iostateDir);
        if (f.exists() && f.isDirectory()) {
            jsonTemplate = """
{
    "NAME":"${logicalPropertyName}",
    "VALUES": {
        ${values}
    },
    "CURRENTSTABLE": "1",
    "VERSION": {
        "1": {
            "DEFAULTACTION":"list",
            "URI": {
                ${uris}
            }
        }
    }
}
"""
        }



        String basedir = BuildSettings.BASE_DIR
        def ant = new AntBuilder()
        //basedir = basedir.substring(0,basedir.length())

        try {
            //Whatever the file path is.
            File statText = new File("E:/Java/Reference/bin/images/statsTest.txt");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.write("POTATO!!!");
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
    }


}
