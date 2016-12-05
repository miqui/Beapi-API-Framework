package grails.api.framework


import grails.dev.commands.*
import grails.core.GrailsApplication
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext

import grails.util.Environment

class ImportDomainCommand implements ApplicationCommand {

    boolean handle(ExecutionContext ctx) {
        getDbType(ctx)
        return true
    }

    protected void getDbType(ExecutionContext ctx) {

        GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication)
        def config = grailsApplication.config

        switch(Environment.current){
            case Environment.DEVELOPMENT:
                println(config.environments.development.grails)
                break;
            case Environment.TEST:
                println(config.environments.test.grails)
                break;
            case Environment.PRODUCTION:
                println(config.environments.production.grails)
                break;
        }
        return
    }
}
