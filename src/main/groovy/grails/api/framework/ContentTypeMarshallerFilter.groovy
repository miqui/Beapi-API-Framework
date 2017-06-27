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

import grails.core.GrailsApplication

import grails.util.Metadata
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import groovy.json.JsonSlurper


@Slf4j
//@CompileStatic
class ContentTypeMarshallerFilter extends OncePerRequestFilter {

    String headerName

    GrailsApplication grailsApplication

    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest request = servletRequest as HttpServletRequest
        HttpServletResponse response = servletResponse as HttpServletResponse

        String format = (request?.format)?request.format.toUpperCase():'JSON';
        List formats = ['XML', 'JSON']

        try {
            // Init params

            if (formats.contains(format)) {
                LinkedHashMap dataParams = [:]
                switch (format) {
                    case 'XML':
                        String xml = request.XML.toString()
                        if(xml!='[:]') {
                            def xslurper = new XmlSlurper()
                            xslurper.parseText(xml).each() { k, v ->
                                dataParams[k] = v
                            }
                            request.setAttribute('XML', dataParams)
                        }
                        break
                    case 'JSON':
                    default:
                        def json = request.JSON.toString()
                        if(json!='[:]') {
                            def slurper = new JsonSlurper()
                            slurper.parseText(json).each() { k, v ->
                                dataParams[k] = v
                            }
                            request.setAttribute('JSON', dataParams)
                        }
                        break
                }

            }
        } catch (Exception e) {
            println("ContentTypeMarshallerFilter: Formatting exception "+e)
            log.error "marshalling failed: ${ae.message}"
            response.status = 401
            response.setHeader('ERROR', 'Failed')
            response.writer.flush()
            return
        }

        chain.doFilter(servletRequest, servletResponse)
    }

}
