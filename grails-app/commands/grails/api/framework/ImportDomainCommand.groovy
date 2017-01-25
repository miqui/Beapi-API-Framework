package grails.api.framework


import grails.dev.commands.*
import grails.core.GrailsApplication
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import org.grails.config.PropertySourcesConfig
import grails.util.Environment
import com.mongodb.*;

class ImportDomainCommand implements ApplicationCommand {

    private MongoClient mongo;
    private MongoClientOptions mongoOptions;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String database;
    private List<ServerAddress> replicaSetSeeds;
    private List<ServerAddress> replicaPair;
    private ConnectionString connectionString;
    private MongoClientURI clientURI;

    boolean handle(ExecutionContext ctx) {
        getDbType(ctx)
        return true
    }

    protected void getDbType(ExecutionContext ctx) {

        GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication)
        PropertySourcesConfig config = grailsApplication.config

        switch(Environment.current){
            case Environment.DEVELOPMENT:
                if(config.environments.development?.grails){
                    LinkedHashMap temp = config.environments.production?.grails
                    createDomains(temp)
                }else{
                    //RDBMS OPTION
                }
                // detect if NOSQL or RDBMS
                break;
            case Environment.TEST:
                if(config.environments.test?.grails){
                    LinkedHashMap temp = config.environments.production?.grails
                    createDomains(temp)
                }else{
                    //RDBMS OPTION
                }
                break;
            case Environment.PRODUCTION:
                if(config.environments.production?.grails){
                    LinkedHashMap temp = config.environments.production?.grails
                    createDomains(temp)
                }else{
                    //RDBMS OPTION
                }
                break;
        }
        return
    }

    protected void createDomains(LinkedHashMap config){
        switch(config.getKey()){
            case 'mongodb':
            default:
                println(config)
                String connectionString = config.mongodb.connectionString
            //println(connectionString)


                // regex the mongodb.connectionString
                //
                // mongodb://myUserName:myPassword@ipOfServer:portOfServer/dbName
                // mongodb://127.0.0.1/test

                //if(connectionString =~ /mongodb\:\/\/(?=.+\:.+\@)(.+)\:(.+)\@(.+)[\:*](.+)\/(.+)|mongodb\:\/\/(?=.+\:{1})(.+)[\:*](.+)\/(.+)/ ){
                //    println true
                //}

                //MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
                // DB db = mongoClient.getDB("database name");
                // boolean auth = db.authenticate("username", "password".toCharArray());

                break;
            //case 'redis':
             //   break;
            //case 'cassandra':
              //  break;
            //case 'dynamodb':
            //   break;
        }
    }

    protected void createRdbmsDomains(){

    }
}
