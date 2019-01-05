package org.pesho.judge.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private NoiUserDetailsService userDetailsService;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
//	     http
//         .authorizeRequests()
//         	.antMatchers("/admin/**").authenticated()
//         	.antMatchers("/**").permitAll()
//         	.and()
//         	.httpBasic();		
//	     http.csrf().disable();
		
        http.csrf().disable()
        	.authorizeRequests()
//			.antMatchers("/").permitAll()
			.antMatchers("/bower_components/**").permitAll()
			.antMatchers("/build/**").permitAll()
			.antMatchers("/dist/**").permitAll()
			.antMatchers("/docs/**").permitAll()
			.antMatchers("/plugins/**").permitAll()
			.antMatchers("/pages/**").permitAll()
			.antMatchers("/login/**").permitAll()
			.antMatchers("/home/**").permitAll()
			.antMatchers("/admin/**").hasAnyRole("ADMIN")
			.antMatchers("/**").hasAnyRole("USER", "ADMIN")
			.anyRequest().authenticated()
        .and()
        .formLogin()
			.loginPage("/login")
			.permitAll()
			.and()
        .logout()
			.permitAll()
		.and()
		.exceptionHandling()
	      .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")); 
	}
	
//    @Autowired	
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth
//            .inMemoryAuthentication()
//                .withUser("admin").password("admin").roles("admin");
//    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    	auth.userDetailsService(userDetailsService);
    }

}
