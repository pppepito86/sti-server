package org.pesho.judge.rest;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.pesho.sandbox.SandboxExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.exec.ProcessExecutor;

@Controller
public class HtmlService {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	private WorkersQueue workersQueue;
	
	@Autowired
	private Repository repository;


    @GetMapping("/admin/problem")
    public String adminProblem() {
    	return "addproblem";
    }
    
    @GetMapping("/admin/problems")
    public String adminProblems(Model model) {
    	List<Map<String,Object>> listProblems = repository.listProblems();
    	List<Map<String,String>> groupProblems = new ArrayList<>(5);
    	for (int i = 0; i < 5; i++) {
    		groupProblems.add(new HashMap<>());
    		char group = (char)('A'+i);
    		groupProblems.get(i).put("id", String.valueOf(group));
    		for (int j = 1; j <= 3; j++) {
    			groupProblems.get(i).put(String.valueOf(j), "");
    		}
    	}
    	for (Map<String, Object> problem: listProblems) {
    		String group = problem.get("group").toString();
    		if (group.length() != 1) continue;
    		char c = group.charAt(0);
    		if (c < 'A' || c > 'E') continue;
    		groupProblems.get(c-'A').put(problem.get("number").toString(), problem.get("name").toString());
    	}
    	model.addAttribute("groups", groupProblems);
    	System.out.println(groupProblems);
    	return "problems";
    }

	@PostMapping("/admin/problem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addProblem(@RequestPart("file") MultipartFile file, @RequestParam("number") Integer number,
			@RequestParam("name") String name, @RequestParam("group") String group,
			@RequestParam("points") Integer points,
			Model model) throws Exception {
		group = group.toUpperCase();
		
		Optional<Map<String, Object>> maybeProblem = repository.getProblem(group, number);

		File zipFile = getFile("problem", group, String.valueOf(number), file.getOriginalFilename().toLowerCase());
		if (maybeProblem.isPresent()) {
			FileUtils.deleteQuietly(getFile("problem", group, String.valueOf(number)));
		}
		
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = new File(zipFile.getAbsolutePath().replace(".zip", ""));
		unzip(zipFile, zipFolder);

		int problemId = 0;
		if (maybeProblem.isPresent()) {
			problemId = (int) maybeProblem.get().get("id");
			repository.updateProblem(problemId, name, points, zipFile.getName());
		} else {
			problemId = repository.addProblem(name, group, number, points, zipFile.getName());
		}

		final int problemIdFinal = problemId;
		workersQueue.getAll().stream().parallel().forEach(worker -> {
			worker.sendProblemToWorker(problemIdFinal, zipFile.getAbsolutePath());
		});

		return "redirect:/admin/problems";
	}

	@GetMapping("/home")
    public String table(@RequestParam(value = "city") String city, Model model) {
    	city = city.toUpperCase();
    	
    	List<Map<String,Object>> submissionsA = repository.listGroupSubmissions(city, "A");
    	List<Map<String,Object>> submissionsB = repository.listGroupSubmissions(city, "B");
    	List<Map<String,Object>> submissionsC = repository.listGroupSubmissions(city, "C");
    	List<Map<String,Object>> submissionsD = repository.listGroupSubmissions(city, "D");
    	List<Map<String,Object>> submissionsE = repository.listGroupSubmissions(city, "E");
        if (submissionsA.size() + submissionsB.size() + submissionsC.size() + submissionsD.size() + submissionsE.size() > 0) {
        	List<Map<String, Object>> problems = repository.listProblems();
        	for (Map<String, Object> problem: problems) {
        		model.addAttribute(problem.get("group").toString()+problem.get("number"), problem.get("name"));
        	}
        	
        	model.addAttribute("submissionsA", submissionsA);
        	model.addAttribute("submissionsB", submissionsB);
        	model.addAttribute("submissionsC", submissionsC);
        	model.addAttribute("submissionsD", submissionsD);
        	model.addAttribute("submissionsE", submissionsE);
        	return "result";
        } else {
        	model.addAttribute("city", city);
        	return "upload";
        }
    }
    
    @PostMapping("/grade")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addSubmission(@RequestPart("file") MultipartFile file, 
			@RequestParam("city") String city, Model model)
			throws Exception {
    	city = city.toUpperCase();

    	String cityEncoded = URLEncoder.encode(city, "UTF-8");
    	
    	if (repository.hasCitySubmissions(city)) {
    		return "redirect:/home?city="+cityEncoded;
    	}
    	
		File zipFile = getFile("submission", city, file.getOriginalFilename().toLowerCase());
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = new File(zipFile.getAbsolutePath().replace(".zip", ""));
		unzip(zipFile, zipFolder);
		
		List<File> firstLevelFiles = Arrays.stream(zipFolder.listFiles()).filter(x -> x.isDirectory()).collect(Collectors.toList());
		if (firstLevelFiles.size() == 1) {
			String name = firstLevelFiles.get(0).getName();
			if (!name.equals("A") && !name.equals("B") && !name.equals("C") && !name.equals("D") && !name.equals("E")) {
				zipFolder = firstLevelFiles.get(0);
			}
		}
		
		
		for (File group: zipFolder.listFiles()) {
			if (!group.isDirectory()) continue;
			
			File upperCaseGroup = new File(zipFolder, group.getName().toUpperCase());
			group.renameTo(upperCaseGroup);
			if (upperCaseGroup.listFiles() == null) continue;
			for (File dir : upperCaseGroup.listFiles()) {
				if (!dir.isDirectory())
					continue;
				String dirName = dir.getCanonicalPath().replace(new File(workDir).getCanonicalPath(), "");
				if (dirName.startsWith(File.separator))
					dirName = dirName.substring(1);
				repository.addSubmission(city, group.getName(), dirName);
			}
		}
		return "redirect:/home?city="+cityEncoded;
	}
    
	private File getFile(String type, String city, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + fileName;
		return new File(path);
	}
	
	private File getFile(String type, String city, String group, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + group + File.separator + fileName;
		return new File(path);
	}

	public void unzip(File file, File folder) {
		try {
			new ProcessExecutor().command("unzip", file.getCanonicalPath(), "-d", folder.getCanonicalPath()).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}