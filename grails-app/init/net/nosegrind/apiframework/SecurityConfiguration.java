package net.nosegrind.apiframework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser("user").password("pwd").roles("USER");
    }
    
    @Autowired
    UserDetailsService userDetailsService;
    
    @Override  
    protected void configure(HttpSecurity http) throws Exception {  
        http  
            .authorizeRequests()
            	.antMatchers("/**").authenticated()
            	.antMatchers("/hook/**").fullyAuthenticated()
				.antMatchers("/").permitAll() 
				.antMatchers("/index").permitAll() 
				.antMatchers("/index.gsp").permitAll() 
				.antMatchers("/**/js/**").permitAll() 
				.antMatchers("/**/css/**").permitAll() 
				.antMatchers("/**/images/**").permitAll() 
				.antMatchers("/**/favicon.ico").permitAll() 
				.antMatchers("/login/**").permitAll() 
				.antMatchers("/logout/**").permitAll();
    }
    
    @Autowired
    protected void globalConfigure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }
    
}
