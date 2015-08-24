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

        String apiVersion = getGrailsApplication().config.getProperty('info.app.version')
        String entrypoint = "v${apiVersion}"

        "/$entrypoint/$controller/$action?/$id?(.$format)?"{
            parseRequest = true
        }

/*
		"/apidoc/show" {
            parseRequest: true
        }
*/

        "/$entrypoint/$controller/$action/$id**" {
            parseRequest = true
        }

        "/${entrypoint}-$apiObjectVersion/$controller/$action/$id**" {
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/$entrypoint/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            parseRequest = true
        }

        "/${entrypoint}-$apiObjectVersion/$controller/$action" {
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

        "/$entrypoint/$controller?/$id" {
            parseRequest = true
        }

        "/${entrypoint}-$apiObjectVersion/$controller?/$id" {
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