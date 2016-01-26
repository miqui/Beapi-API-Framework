/*
 * The MIT License (MIT)
 * Copyright 2014 Owen Rubel
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

import grails.core.GrailsApplication
import grails.web.servlet.mvc.GrailsParameterMap
import org.grails.groovy.grails.commons.*

//import grails.application.springsecurity.SpringSecurityService
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest

class MetricsService {

	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService

	static transactional = false

	Map currentTimer = [:]

	// top level parent
	String currentController = ""


	private getTime(){
		return currentTimer
	}

	private setTime(Long time){
		currentTimer = time
	}

	private void clearTimer() {
		currentTimer = 0
	}

	// TODO
	private void clearLog(){}

	void getTimer() {
		endTime()
		Long time = currentTimer
		clearTimer()
		println(time+"ms")
	}

	void startTime() {
		clearTimer()
		String callingClass = Thread.currentThread().getStackTrace()[2].getClassName()
		String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName()
		setTime(System.currentTimeMillis())
	}

	void setClassAndMethod(String classname, String methodname){
		// test whether 'class' is same as currentTimer.lastKey()
		// else start new class/method
	}

	void endTime() {
		Long start = getTime()
		Long end = System.currentTimeMillis()-start
		setTime(end)
		println(end+"ms")
	}

	public static void trace(StackTraceElement[] e) {
		boolean doNext = false;
		for (StackTraceElement s : e) {
			if (doNext) {
				System.out.println(s.getMethodName());
				return;
			}
			doNext = s.getMethodName().equals("getStackTrace");
		}
	}
}
