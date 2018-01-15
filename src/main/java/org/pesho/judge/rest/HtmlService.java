package org.pesho.judge.rest;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.zip.ZipUtil;

@Controller
public class HtmlService {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	private WorkersQueue workersQueue;
	
	@Autowired
	private Repository repository;

	@GetMapping("/admin")
    public String adminPage() {
    	return "redirect:/admin/problems";
    }

	@GetMapping("/admin/submissions")
	public String adminSubmissions(Model model) {
		List<Map<String,Object>> submissions = repository.listSubmissions();
		model.addAttribute("submissions", submissions);
		return "submissions";
	}
	
    @GetMapping("/admin/problem")
    public String adminProblem() {
    	return "addproblem";
    }
    
    @GetMapping("/admin/problems")
    public String adminProblems(Model model) {
    	List<Map<String,Object>> listProblems = repository.listProblems();
    	Map<String, List<Map<String, Object>>> contestProblems = listProblems.stream().collect(Collectors.groupingBy(x->x.get("contest").toString()));
    	contestProblems.values().stream().forEach(x -> Collections.sort(x, 
    			(Map<String, Object> o1, Map<String, Object> o2) -> {
    	    		return (int) o1.get("number") - (int) o2.get("number");
    	    	}));
    	Map<String, Object> emptyMap = new HashMap<>();
    	contestProblems.values().stream().forEach(x -> {
    		for (int i = 0; i < 3; i++) {
    			if (i >= x.size() || (int) x.get(i).get("number") > i+1) {
    				x.add(i, emptyMap);
    			}
    		}
    	});

    	model.addAttribute("contests", contestProblems);
    	return "problems";
    }

	@GetMapping("/admin/problem/{contest}/{number}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String showProblem(@PathVariable("contest") String contest,
			@PathVariable int number,
			Model model) throws Exception {
		contest = contest.toLowerCase();
		
		Optional<Map<String, Object>> maybeProblem = repository.getProblem(contest, number);
		if (maybeProblem.isPresent()) {
			System.out.println(maybeProblem.get());
			File problemDir = getFile("problem", contest, number+"");
			TaskParser parser = new TaskParser(problemDir);
			TaskDetails details = TaskDetails.create(parser);
			model.addAttribute("problem", maybeProblem.get());
			model.addAttribute("details", details);
		}

		return "problem";
	}
    
	@PostMapping("/admin/problem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addProblem(@RequestPart("file") MultipartFile file, @RequestParam("number") Integer number,
			@RequestParam("name") String name, @RequestParam("contest") String contest,
			Model model) throws Exception {
		name = name.toLowerCase();
		contest = contest.toLowerCase();
		
		Optional<Map<String, Object>> maybeProblem = repository.getProblem(contest, number);

		File zipFile = getFile("problem", contest, String.valueOf(number), name + ".zip");
		if (maybeProblem.isPresent()) {
			FileUtils.deleteQuietly(getFile("problem", contest, String.valueOf(number)));
		}
		
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = getFile("problem", contest, String.valueOf(number), name);
		unzip(zipFile, zipFolder);

		int problemId = 0;
		if (maybeProblem.isPresent()) {
			problemId = (int) maybeProblem.get().get("id");
			repository.updateProblem(problemId, name, zipFile.getName());
		} else {
			problemId = repository.addProblem(name, contest, number, zipFile.getName());
		}

		final int problemIdFinal = problemId;
		workersQueue.getAll().stream().parallel().forEach(worker -> {
			worker.sendProblemToWorker(problemIdFinal, zipFile.getAbsolutePath());
		});

		return "redirect:/admin/problems";
	}

	@GetMapping("/home")
    public String table(@RequestParam(value = "city") String city, Model model) {
    	city = city.toLowerCase();
    	List<Map<String,Object>> submissions = repository.listCitySubmissions(city);
    	if (submissions.size() > 0) {
    		Map<String, Map<String, List<Map<String, Object>>>> result = new HashMap<>();
    		Map<String, List<Map<String, Object>>> contestsSubmissions = submissions.stream().collect(Collectors.groupingBy(s->s.get("contest").toString()));
    		for (String contest: contestsSubmissions.keySet()) {
    			List<Map<String, Object>> contestSubmissions = contestsSubmissions.get(contest);
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
    
    @PostMapping("/grade")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addSubmission(@RequestPart("file") MultipartFile file, 
			@RequestParam("city") String city, Model model)
			throws Exception {
    	city = city.toLowerCase();
    	String cityEncoded = URLEncoder.encode(city, "UTF-8");
    	
    	if (repository.hasCitySubmissions(city)) {
    		return "redirect:/home?city="+cityEncoded;
    	}
    	
		File zipFile = getFile("submission", city, city + ".zip");
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = getFile("submission", city, city);
		unzip(zipFile, zipFolder);

		List<File> listSourceFiles = listSourceFiles(zipFolder);
		for (File sourceFile: listSourceFiles) {
			String username = sourceFile.getParentFile().getName();
			String contest = sourceFile.getParentFile().getParentFile().getName().toLowerCase();
			String sourceFilePath = sourceFile.getCanonicalPath().replace(new File(workDir).getCanonicalPath(), "");
			if (sourceFilePath.startsWith(File.separator)) {
				sourceFilePath = sourceFilePath.substring(1);
			}
			String problemName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
			repository.addSubmission(city, username, contest, problemName, sourceFilePath);
		}
		return "redirect:/home?city="+cityEncoded;
	}
    
	private List<File> listSourceFiles(File zipFolder) {
		File[] allFiles = zipFolder.listFiles();
		List<File> files = new ArrayList<>();
		if (allFiles == null) return files;
		
		for (File file: allFiles) {
			if (file.isFile() && isSourceFile(file)) files.add(file);
			if (file.isDirectory()) files.addAll(listSourceFiles(file));
		}
		return files;
	}

	private boolean isSourceFile(File file) {
		String[] split = file.getName().split("\\.");
		String extension = split[split.length-1];
		if (extension.equalsIgnoreCase("cpp")) return true;
		if (extension.equalsIgnoreCase("c")) return true;
		return false;
	}

	private File getFile(String type, String city, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + fileName;
		return new File(path).getAbsoluteFile();
	}
	
	private File getFile(String type, String city, String group, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + group + File.separator + fileName;
		return new File(path).getAbsoluteFile();
	}

	public void unzip(File file, File folder) {
		try {
			ZipUtil.unpack(file, folder);
		} catch (Exception e) {
			try {
				new ProcessExecutor().command("unzip", file.getCanonicalPath(), "-d", folder.getCanonicalPath()).execute();
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new IllegalStateException(e);
			}
		}
	}

}