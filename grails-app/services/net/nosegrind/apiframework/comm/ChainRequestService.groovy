package net.nosegrind.apiframework.comm

import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.groovy.grails.commons.*

import javax.servlet.forward.*

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/
import javax.servlet.http.HttpServletRequest
import net.nosegrind.apiframework.*

class ChainRequestService extends ApiLayerService{

	static transactional = false
	
	boolean handleApiRequest(LinkedHashMap cache, HttpServletRequest request, GrailsParameterMap params, String entryPoint){
		try{
			setEnv()
			
			ApiStatuses error = new ApiStatuses()
			setApiParams(request, params)
			// CHECK IF URI HAS CACHE
			if(cache[params.apiObject][params.action]){

				// CHECK ACCESS TO METHOD
				List roles = cache[params.apiObject][params.action]['roles']?.toList()
				
				/*
				if(!checkAuth(request,roles)){
					return false
				}
				*/

				// CHECK VERSION DEPRECATION DATE
				if(cache[params.apiObject][params.action]['deprecated']?.get(0)){
					String depdate = cache[params.apiObject][params.action]['deprecated'][0]
					
					if(checkDeprecationDate(depdate)){
						String depMsg = cache[params.apiObject][params.action]['deprecated'][1]
						// replace msg with config deprecation message
						String msg = "[ERROR] ${depMsg}"
						error._400_BAD_REQUEST(msg)?.send()

						return false
					}
				}

				// CHECK METHOD FOR API CHAINING. DOES METHOD MATCH?
				def method = cache[params.apiObject][params.action]['method']?.trim()

				// TEST FOR CHAIN PATHS
				if(chain && params?.chain){
					List uri = [params.controller,params.action,params.id]
					int pos = checkChainedMethodPosition(cache,request, params,uri,params?.chain?.order as Map)
					if(pos==3){
						String msg = "[ERROR] Bad combination of unsafe METHODS in api chain."
						error._400_BAD_REQUEST(msg)?.send()
						return false
					}else{
						return true
					}
				}else{
					return true
				}

			}
			return false
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:",e)
		}
	}
	
	protected void setApiParams(HttpServletRequest request, GrailsParameterMap params){
		try{
            String contentType = params.format

            if(request?."${contentType}"){
                request?."${contentType}".each{ k,v ->
                    if(chain && k=='chain'){
                        params.apiChain = [:]
                        params.apiChain = request."${contentType}".chain
                    }else if(batch && k=='batch'){
                        params.apiBatch = []
                        v.each { it ->
                            params.apiBatch.add(it)
                        }
                        params.apiBatch = params.apiBatch
                        //request.JSON.remove("batch")
                    }else{
                        params[k]=v
                    }
                }
                if(request?."${contentType}"?.chain){
                    request."${contentType}".remove('chain')
                }
                if(request?."${contentType}"?.batch){
                    request."${contentType}".remove('batch')
                }
            }
			
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: setApiParams] : Exception - full stack trace follows:"+ e);
		}
	}
	
	boolean isRequestMatch(String protocol,String method){
		if(['TRACERT','OPTIONS','TRACE','HEAD'].contains(method)){
			return true
		}else{
			if(protocol == method){
				return true
			}else{
				return false
			}
		}
		return false
	}
}
