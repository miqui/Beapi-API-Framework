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

import net.nosegrind.apiframework.ApiDescriptor
import javax.servlet.forward.*
import net.nosegrind.apiframework.comm.ApiLayer
import org.grails.groovy.grails.commons.*
import grails.web.servlet.mvc.GrailsParameterMap
//import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.*
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletResponse

@CompileStatic
class ApiRequestService extends ApiLayer{

	def grailsApplication

	static transactional = false


	boolean handleApiRequest(ApiDescriptor cache, HttpServletRequest request, HttpServletResponse response, GrailsParameterMap params){
		//println("#### [ApiRequestService : handleApiRequest ] ####")
		try{
			// CHECK ACCESS TO METHOD
			List roles = cache['roles'] as List
			if(!checkAuth(request,roles)){
				response.status = 401
				response.setHeader('ERROR','Unauthorized Access attempted')
				return false
			}

			// CHECK VERSION DEPRECATION DATE
			List deprecated = cache['deprecated'] as List

			if(deprecated?.get(0)){
				if(checkDeprecationDate(deprecated[0].toString())){
					String depMsg = deprecated[1].toString()
					response.status = 400
					response.setHeader('ERROR',depMsg)
					return false
				}
			}

			def method = cache['method']?.toString().trim()

			// DOES api.methods.contains(request.method)
			if(!isRequestMatch(method,request.method.toString())){
				response.status = 400
				response.setHeader('ERROR',"Request method doesn't match expected method.")
				return false
			}
			return true
		}catch(Exception e){
			//throw new Exception("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:",e)
			println("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:"+e)
		}
	}
}
