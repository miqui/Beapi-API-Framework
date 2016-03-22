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

import grails.util.Holders


import javax.servlet.forward.*
import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.groovy.grails.commons.*
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.ApiDescriptor
import grails.core.GrailsApplication
import net.nosegrind.apiframework.*
import groovy.transform.CompileStatic

@CompileStatic
class ApiResponseService extends ApiLayer{

	GrailsApplication grailsApplication
	ApiCacheService apiCacheService


	def handleApiResponse(ApiDescriptor cache, HttpServletRequest request, HttpServletResponse response, LinkedHashMap model, GrailsParameterMap params){
		//println("#### [ApiResponseService : handleApiResponse ] ####")
		try{
			response.setHeader('Authorization', cache['roles'].toString().join(', '))
			List responseList = getApiParams((LinkedHashMap)cache['returns'])
			LinkedHashMap result = parseURIDefinitions(model,responseList)

			// will parse empty map the same as map with content
			LinkedHashMap content = parseResponseMethod(request, params, result)
			return content

		}catch(Exception e){
			throw new Exception("[ApiResponseService :: handleApiResponse] : Exception - full stack trace follows:",e)
		}
	}
}