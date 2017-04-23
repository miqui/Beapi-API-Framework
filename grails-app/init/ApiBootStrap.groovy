/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils

class ApiBootStrap {


	def init = { servletContext ->
		//SpringSecurityUtils.registerFilter 'tokenCacheValidationFilter', SecurityFilterPosition.ANONYMOUS_FILTER.order + 2
		//SpringSecurityUtils.registerFilter 'corsSecurityFilter', SecurityFilterPosition.ANONYMOUS_FILTER.order + 3
	}
	
	def destroy = {}

}
