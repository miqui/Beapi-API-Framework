// Place your Spring DSL code here
//import net.nosegrind.apiframework.GormUserDetailsService
//import net.nosegrind.apiframework.SecurityConfiguration

beans = {
    webSecurityConfiguration(SecurityConfiguration)
    userDetailsService(GormUserDetailsService)
}
