package net.nosegrind.apiframework.comm

import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.groovy.grails.commons.*
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils

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
			//setApiParams(request, params)

			def format = params.format

			// CHECK IF URI HAS CACHE
			if(cache[params.apiObject][params.action]){
println("uri has cache...")
				// CHECK ACCESS TO METHOD
				/*
				List roles = cache[params.apiObject][params.action]['roles']?.toList()
				if(!checkAuth(request,roles)){
					return false
				}
				*/


				if(!checkURIDefinitions(request,cache[params.apiObject][params.action]['receives'])){
					println("return bad status")
					String msg = 'Expected request variables do not match sent variables'
					error._400_BAD_REQUEST(msg)?.send()
					return false
				}

				println("################## after checkURIDefinitions: "+params)

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
				println("${chain}/${this.chain}/${params.apiChain}")
				if(this.chain && params?.apiChain){
					println("chain detected...")
					List uri = [params.controller,params.action,params.id]
					int pos = checkChainedMethodPosition(cache,request, params,uri,params?.apiChain?.order as Map)
					println("pso : ${pos}")
					if(pos==3){
						String msg = "[ERROR] Bad combination of unsafe METHODS in api chain."
						error._400_BAD_REQUEST(msg)?.send()
						return false
					}
				}
				return true
			}
			return false
		}catch(Exception e){
			throw new Exception("[ApiRequestService :: handleApiRequest] : Exception - full stack trace follows:",e)
		}
	}
	
	protected void setApiParams(HttpServletRequest request, GrailsParameterMap params){
		println("############# setApiParams")
		try{
            String contentType = request?."${params.format}"
            if(contentType){
				contentType.each{ k,v ->
                    if(chain && k=='chain'){
						params.apiChain = [:]
						v.each { k2,v2 ->
							params.apiChain[k2] = v2
						}
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
                if(contentType?.chain){
					contentType.remove('chain')
                }
                if(contentType?.batch){
					contentType.remove('batch')
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
