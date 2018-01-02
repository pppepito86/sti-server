package org.pesho.judge.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private JudgeUserDetailsService judgeUserDetailsService;

	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(judgeUserDetailsService);
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
	     http
         .authorizeRequests()
         	.antMatchers("/api/**").authenticated()
             .antMatchers("/**").permitAll()
             .and()
             .httpBasic();		
	     http.csrf().disable();
	}

}
