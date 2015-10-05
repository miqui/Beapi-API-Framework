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
        String batchEntryPoint = "b${apiVersion}"
        String chainEntryPoint = "c${apiVersion}"
        String tracertEntryPoint = "t${apiVersion}"
        String domainEntryPoint = "d${apiVersion}"

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
        "/$batchEntryPoint/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$batchEntryPoint/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$batchEntryPoint/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action" {
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
        "/$domainEntryPoint/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$domainEntryPoint/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${domainEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$domainEntryPoint/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${domainEntryPoint}-$apiObjectVersion/$controller/$action" {
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
        "/$tracertEntryPoint/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$tracertEntryPoint/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${tracertEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$tracertEntryPoint/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${tracertEntryPoint}-$apiObjectVersion/$controller/$action" {
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
        "/$chainEntryPoint/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

        "/$chainEntryPoint/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$chainEntryPoint/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action" {
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