/*
 * Academic Free License ("AFL") v. 3.0
 * Copyright 2014-2017 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/AFL-3.0
 */

package net.nosegrind.apiframework

import grails.util.Metadata


class ApiFrameworkUrlMappings {

    static mappings = {

        String apiVersion = Metadata.current.getApplicationVersion()
        String api = "v${apiVersion}"
        String batchEntryPoint = "b${apiVersion}"
        String chainEntryPoint = "c${apiVersion}"
        String profilerEntryPoint = "p${apiVersion}"


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
        "/$batchEntryPoint/$controller/$action/$id?**"{
            entryPoint = batchEntryPoint
            parseRequest = true
        }

        "/$batchEntryPoint/$controller/$action"{
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
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


        // CHAIN API ENDPOINTS
        "/$chainEntryPoint/$controller/$action/$id?**"{
            entryPoint = chainEntryPoint
            parseRequest = true
        }

        "/$chainEntryPoint/$controller/$action"{
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = chainEntryPoint
            parseRequest = true
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = chainEntryPoint
            apiObjectVersion = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/${chainEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = chainEntryPoint
            apiObjectVersion = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        // PROFILER API ENDPOINTS
        "/$profilerEntryPoint/$controller/$action/$id?**"{
            entryPoint = profilerEntryPoint
            parseRequest = true
        }

        "/$profilerEntryPoint/$controller/$action"{
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = profilerEntryPoint
            parseRequest = true
        }

        "/${profilerEntryPoint}-$apiObjectVersion/$controller/$action/$id**" {
            entryPoint = profilerEntryPoint
            apiObjectVersion = apiObjectVersion
            parseRequest = true
            constraints {
                apiObjectVersion(matches:/^[0-9]?[0-9]?(\\.[0-9][0-9]?)?/)
            }
        }

        "/${profilerEntryPoint}-$apiObjectVersion/$controller/$action" {
            if(action?.toInteger()==action && action!=null){
                id=action
                action = null
            }
            entryPoint = profilerEntryPoint
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
