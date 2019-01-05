package org.pesho.judge.rest;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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

    @PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/home")
    public String table(@RequestParam(value = "city") String city, Model model) {
    	city = city.toLowerCase();
    	List<Map<String,Object>> submissions = repository.listCitySubmissions(city);
    	if (submissions.size() > 0) {
    		Map<String, Map<String, List<Map<String, Object>>>> result = new HashMap<>();
    		for (String contest: new String[] {"A", "B", "C", "D", "E"}) {
    			List<Map<String, Object>> contestSubmissions = repository.listContestSubmissions(city, contest);
        		Map<String, List<Map<String, Object>>> usersSubmissions = contestSubmissions.stream().collect(Collectors.groupingBy(s->s.get("username").toString()));
        		for (List<Map<String, Object>> list: usersSubmissions.values()) {
        			while (list.size() < 3) list.add(new HashMap<>());
        		}
        		result.put(contest, usersSubmissions);
    		}
    		
    		System.out.println(result);
    		model.addAttribute("contests", result);
        	return "result";
        } else {
        	model.addAttribute("city", city);
        	return "upload";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/grade")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public synchronized String addSubmission(@RequestPart("file") MultipartFile file, 
			@RequestParam("city") String city, Model model)
			throws Exception {
		File zipFile = getFile("temp", city, city + ".zip");
		zipFile.getParentFile().mkdirs();
		
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = getFile("temp", city, city);
		unzip(zipFile, zipFolder);

		List<File> listSourceFiles = listSourceFiles(zipFolder);
		for (File sourceFile: listSourceFiles) {
			String username = sourceFile.getParentFile().getName();
			String contest = sourceFile.getParentFile().getParentFile().getName();
			String problemName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
			String fileName = sourceFile.getName();
			int submissionId = repository.addSubmission(city, username, contest, problemName, fileName);
			if (submissionId != 0) {
				File newFile = getFile("submissions", String.valueOf(submissionId), fileName);
				newFile.getParentFile().mkdirs();
				FileUtils.copyFile(sourceFile, newFile);
			} else {
				String details = String.format("%s_%s_%s_%s", city, contest, username, problemName);
				repository.addLog("submission", "problem not found for " + details, "");
			}
		}
		
		FileUtils.deleteQuietly(zipFile);
		FileUtils.deleteQuietly(zipFolder);
		
		return "redirect:/admin/submissions";
	}

    
}
