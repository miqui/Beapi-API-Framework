/*
 * The MIT License (MIT)
 * Copyright 2013 Owen Rubel
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

package net.nosegrind.apiframework

import grails.util.Metadata

class ApiFrameworkUrlMappings {

    static mappings = {

        String apiVersion = Metadata.current.getApplicationVersion()
        String api = "v${apiVersion}"
        String batchEntryPoint = "b${apiVersion}"
        String chainEntryPoint = "c${apiVersion}"
        String metricsEntryPoint = "m${apiVersion}"


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

        // REGULAR API ENDPOINTS
        "/$api/$controller/$action/$id?**"{
            entryPoint = api
            parseRequest = true
        }

        "/$api/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = api
            parseRequest = true
        }


        "/${api}-$apiObjectVersion/$controller/$action/$id?**" {
            entryPoint = api
            apiObject = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
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
        "/$batchEntryPoint/$controller/$action"{
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/$batchEntryPoint/$controller/$action/$id?**"{
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = batchEntryPoint
            apiObjectVersion = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }


        "/${batchEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = batchEntryPoint
            apiObjectVersion = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }


        "200"{
            id = '200'
            controller = "error"
            parseRequest = true
        }
        "302"{
            id = '302'
            controller = "error"
            parseRequest = true
        }
        "304"{
            id = '304'
            controller = "error"
            parseRequest = true
        }
        "400"{
            id = '400'
            controller = "error"
            parseRequest = true
        }
        "401"{
            id = '401'
            controller = "error"
            parseRequest = true
        }
        "403" {
            id = '403'
            controller = "error"
            parseRequest = true
        }
        "404" {
            id = '404'
            controller = "error"
            parseRequest = true
        }
        "405" {
            id = '405'
            controller = "error"
            parseRequest = true
        }
        "409"{
            id = '409'
            controller = "error"
            parseRequest = true
        }
        "412"{
            id = '412'
            controller = "error"
            parseRequest = true
        }
        "413"{
            id = '413'
            controller = "error"
            parseRequest = true
        }
        "416"{
            id = '416'
            controller = "error"
            parseRequest = true
        }
        "500" {
            id = '500'
            controller = "error"
            parseRequest = true
        }
        "500" {
            id = '500'
            exception = NullPointerException
            controller = "error"
            parseRequest = true
        }

        "503"{
            id = '503'
            controller = "error"
            parseRequest = true
        }
    }

}