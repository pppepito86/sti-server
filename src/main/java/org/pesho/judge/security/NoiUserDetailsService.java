package org.pesho.judge.security;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
public class NoiUserDetailsService implements UserDetailsService {

	private static final String ROLE_PREFIX = "ROLE_";
	@Autowired
	private JdbcTemplate template;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username.equalsIgnoreCase("admin")) {
			return new User("admin", "admin", 
					Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + "ADMIN")));
		}
		
		Optional<Map<String, Object>> result = template.queryForList(
				"select user, password, role from users", 
				new Object[] {username}).stream().findFirst();
		
		return result.map(user -> new User(
				user.get("user").toString(), 
				user.get("password").toString(), 
				Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + user.get("role").toString())))).orElse(null);
	}
	
}