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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.filter.OncePerRequestFilter

import net.nosegrind.apiframework.CorsService
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

import javax.servlet.http.HttpServletRequest
//import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse

import com.google.common.io.CharStreams


class CorsSecurityFilter extends OncePerRequestFilter {

    @Autowired
    CorsService crsService

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    //void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = request as HttpServletRequest
        HttpServletResponse httpResponse = response as HttpServletResponse

        if( !crsService.processPreflight(httpRequest, httpResponse) ) {
            chain.doFilter(request, response)
        }
    }
}
