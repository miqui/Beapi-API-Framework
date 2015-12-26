package grails.api.framework

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration

import org.springframework.context.annotation.ComponentScan

@ComponentScan
@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
class Application extends GrailsAutoConfiguration {
	
    static void main(String[] args) {
        GrailsApp.run(Application)
    }
}