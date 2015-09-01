package net.nosegrind.apiframework

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/

import net.nosegrind.apiframework.comm.ApiLayerService
import org.grails.groovy.grails.commons.*
import javax.servlet.forward.*

class TimerService extends ApiLayerService{

	List currentTimer = []

	void clearTimer(){
		currentTimer = []
	}

	List getTimer(){
		return currentTimer
	}

	void startTime(String classname, String methodname) {
		String key = "${classname}/${methodname}".toString()
		Long time = System.currentTimeMillis()
		Map log = [:]
		log."${key}" = time
		currentTimer.add(log)
	}

	void endTime(String classname, String methodname){
		String key = "${classname}/${methodname}"
		Long end = System.currentTimeMillis()

		def lastIndex = currentTimer.indexOf(currentTimer.get(currentTimer.size()-1))
		LinkedHashMap newMap = currentTimer[lastIndex]

		if(newMap.keySet()[0] == key){
			String index = newMap.keySet()[0]
			Long finalTime = newMap["${index}"]
			newMap[key] = end-finalTime
			currentTimer[lastIndex] = newMap
		} else{
			println("#### FAIL - tried to end parent time in secondary loop ####")
		}

	}
}
