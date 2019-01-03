package org.pesho.judge.rest;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommonHtmlService extends HtmlService {

    @GetMapping("/")
    public String index() {
    	Object adminRole = new SimpleGrantedAuthority("ROLE_ADMIN");
		boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(adminRole);
    	if (isAdmin) {
    		return "redirect:/admin";
    	}
    	return "redirect:/user/problem/1";
    }

    @GetMapping("/login")
    public String login() {
    	return "/login";
    }

//	@GetMapping("/home")
//    public String table(@RequestParam(value = "city") String city, Model model) {
//    	city = city.toLowerCase();
//    	List<Map<String,Object>> submissions = repository.listCitySubmissions(city);
//    	if (submissions.size() > 0) {
//    		Map<String, Map<String, List<Map<String, Object>>>> result = new HashMap<>();
//    		Map<String, List<Map<String, Object>>> contestsSubmissions = submissions.stream().collect(Collectors.groupingBy(s->s.get("contest").toString()));
//    		for (String contest: contestsSubmissions.keySet()) {
//    			List<Map<String, Object>> contestSubmissions = contestsSubmissions.get(contest);
//        		Map<String, List<Map<String, Object>>> usersSubmissions = contestSubmissions.stream().collect(Collectors.groupingBy(s->s.get("username").toString()));
//        		for (List<Map<String, Object>> list: usersSubmissions.values()) {
//        			while (list.size() < 3) list.add(new HashMap<>());
//        		}
//        		result.put(contest, usersSubmissions);
//    		}
//    		
//    		System.out.println(result);
//    		model.addAttribute("contests", result);
//        	return "result";
//        } else {
//        	model.addAttribute("city", city);
//        	return "upload";
//        }
//    }

    
    
}
