package grails.api.framework

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.filter.GenericFilterBean

import net.nosegrind.apiframework.CorsService
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class SpringSecurityCORSFilter extends GenericFilterBean {

    @Autowired
    CorsService crsService

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if( !crsService.processPreflight(request, response) ) {
            chain.doFilter(request, response)
        }
    }
}