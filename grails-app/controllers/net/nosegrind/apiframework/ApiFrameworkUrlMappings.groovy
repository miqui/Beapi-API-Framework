package net.nosegrind.apiframework

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

import grails.util.Metadata



class ApiFrameworkUrlMappings {

	static mappings = {

        //String apiVersion = getGrailsApplication().config.getProperty('info.app.version')
        String apiVersion = getGrailsApplication().metadata['info.app.version']
        String api = "v${apiVersion}"
        String batch = "b${apiVersion}"
        String domain = "c${apiVersion}"
        String tracert = "t${apiVersion}"
        String apidomain = "d${apiVersion}"

/*
		"/apidoc/show" {
            parseRequest: true
        }
*/

        // REGULAR API ENDPOINTS
        "/$api/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$api/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${api}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$api/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${api}-$apiObjectVersion/$controller/$action" {
            controller = controller
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // BATCH API ENDPOINTS
        "/$batch/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$batch/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${batch}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$batch/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${batch}-$apiObjectVersion/$controller/$action" {
            controller = controller
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // DOMAIN API ENDPOINTS
        "/$apidomain/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$apidomain/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${apidomain}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$apidomain/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${apidomain}-$apiObjectVersion/$controller/$action" {
            controller = controller
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // TRACERT API ENDPOINTS
        "/$tracert/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$tracert/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${tracert}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$tracert/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${tracert}-$apiObjectVersion/$controller/$action" {
            controller = controller
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // CHAIN API ENDPOINTS
        "/$chain/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$chain/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${chain}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$chain/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${chain}-$apiObjectVersion/$controller/$action" {
            controller = controller
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "403" {
            controller = "errors"
            parseRequest = true
        }
        "404" {
            controller = "errors"
            parseRequest = true
        }
        "405" {
            controller = "errors"
            parseRequest = true
        }
        "500" {
            controller = "errors"
            parseRequest = true
        }
    }

}