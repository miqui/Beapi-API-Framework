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
        //String apiVersion = getGrailsApplication().metadata['info.app.version']
        String apiVersion = Metadata.current.getApplicationVersion()
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

        "/login/$action" {
            controller = 'login'
            action = action
            parseRequest= true
        }
        "/logout?/$action" {
            controller = 'logout'
            action = action
            parseRequest= true
        }

        "/v01/$controller/$action?/$id?(.$format)?"{
            entryPoint = 'v01'
            parseRequest = true
        }

        "/v01/$controller/$action/$id**" {
            entryPoint = 'v01'
            parseRequest = true
        }


        // REGULAR API ENDPOINTS
        "/$api/$controller/$action?/$id?(.$format)?"{
            entryPoint = api
            parseRequest = true
        }

        "/$api/$controller/$action/$id**" {
            entryPoint = api
            parseRequest = true
        }

        "/${api}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = api
            apiObject = apiObjectVersion
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
            entryPoint = api
            parseRequest = true
        }

        "/${api}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = api
            apiObject = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // BATCH API ENDPOINTS
        "/$batchEntryPoint/$controller/$action?/$id?(.$format)?"{
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/$batchEntryPoint/$controller/$action/$id**" {
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = batchEntryPoint
            apiObject = apiObjectVersion
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
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = batchEntryPoint
            apiObject = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // DOMAIN API ENDPOINTS
        "/$domainEntryPoint/$controller/$action?/$id?(.$format)?"{
            entryPoint = domainEntryPoint
            parseRequest = true
        }

        "/$domainEntryPoint/$controller/$action/$id**" {
            entryPoint = domainEntryPoint
            parseRequest = true
        }

        "/${domainEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = domainEntryPoint
            apiObject = apiObjectVersion
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
            entryPoint = domainEntryPoint
            parseRequest = true
        }

        "/${domainEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = domainEntryPoint
            apiObject = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // TRACERT API ENDPOINTS
        "/$tracertEntryPoint/$controller/$action?/$id?(.$format)?"{
            entryPoint = tracertEntryPoint
            parseRequest = true
        }

        "/$tracertEntryPoint/$controller/$action/$id**" {
            entryPoint = tracertEntryPoint
            parseRequest = true
        }

        "/${tracertEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = tracertEntryPoint
            apiObject = apiObjectVersion
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
            entryPoint = tracertEntryPoint
            parseRequest = true
        }

        "/${tracertEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = tracertEntryPoint
            apiObject = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }



        // CHAIN API ENDPOINTS
        "/$chainEntryPoint/$controller/$action?/$id?(.$format)?"{
            entryPoint = chainEntryPoint
            parseRequest = true
        }

        "/$chainEntryPoint/$controller/$action/$id**" {
            entryPoint = chainEntryPoint
            parseRequest = true
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = chainEntryPoint
            apiObject = apiObjectVersion
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
            entryPoint = chainEntryPoint
            parseRequest = true
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = chainEntryPoint
            apiObject = apiObjectVersion
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