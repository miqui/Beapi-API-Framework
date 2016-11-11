package net.nosegrind.apiframework;

public enum RequestMethod {
	 GET("get"),
	 PUT("put"),
	 POST("post"),
	 DELETE("delete"),
	 HEAD("head"),
	 TRACE("trace"),
	 OPTIONS("options")
	
	 private final String value
	
	 RequestMethod(String value){
	  this.value = value;
	 }
	
	 String toString() {
	  value
	 }
	
	 String getKey() {
	  name()
	 }
	
	 static list(){
		 [GET,PUT,POST,DELETE,HEAD,TRACE,OPTIONS]
	 }

	 static restAltList(){
		 [OPTIONS,TRACE,HEAD]
	 }

	 public static Boolean isRestAlt(String keyValue){
		 for (wd in restAltList()) {
			 if (wd.getKey().equals(keyValue)){
				 return true
			 }
		 }
		 return false
	 }

	 
	 public static RequestMethod fromString(String keyValue) {
		 for (wd in list()) {
			 if (wd.getKey().equals(keyValue)){
			 	return wd
			}
		 }
		 throw new IllegalArgumentException("There's no RequestMethod value with key " + keyValue)
	 }
}
