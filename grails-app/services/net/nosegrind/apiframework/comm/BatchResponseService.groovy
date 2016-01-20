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

package net.nosegrind.apiframework.comm

import grails.core.GrailsApplication

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/



//import grails.application.springsecurity.SpringSecurityService
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import net.nosegrind.apiframework.ApiCacheService
import org.grails.groovy.grails.commons.*

import javax.servlet.forward.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

//import org.grails.web.sitemesh.GrailsContentBufferingResponse
@CompileStatic
class BatchResponseService extends ApiLayer{

	GrailsApplication grailsApplication
	ApiCacheService apiCacheService

	def handleApiResponse(Object cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		//println("#### [ApiResponseService : handleApiResponse ] ####")

		try{
			response.setHeader('Authorization', cache['roles'].toString().join(', '))
			List responseList = getApiParams((LinkedHashMap)cache['returns'])
			LinkedHashMap result = parseURIDefinitions(model,responseList)

			// TODO : add combine functionality for batching
			//if(params?.apiBatch.combine=='true'){
			//	params.apiCombine["${params.uri}"] = result
			//}

			if(!result){
				response.status = 400
			}else{
				LinkedHashMap content = parseResponseMethod(request, params, result)
				return content
			}
		}catch(Exception e){
			throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
		}

	}
}