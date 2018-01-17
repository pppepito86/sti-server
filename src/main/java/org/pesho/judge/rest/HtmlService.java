package org.pesho.judge.rest;

import java.io.File;
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
import org.pesho.grader.SubmissionScore;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.pesho.workermanager.Configuration;
import org.pesho.workermanager.RunTerminateListener;
import org.pesho.workermanager.WorkerManager;
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

import com.amazonaws.services.ec2.model.InstanceType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class HtmlService implements RunTerminateListener {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	private WorkersQueue workersQueue;
	
	@Autowired
	private Repository repository;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void instanceCreated(String url) {
		repository.addWorker(url + ":8089");
	}
	
	@Override
	public void instanceTerminated(String url) {
		repository.deleteWorker(url + ":8089");
	}

	@GetMapping("/admin")
    public String adminPage(Model model) {
		List<Map<String,Object>> workers = repository.listWorkers();
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions();
		List<Map<String,Object>> submissionsQueue = submissions.stream()
				.filter(s -> s.get("verdict").equals("waiting") 
						|| s.get("verdict").equals("judging"))
				.collect(Collectors.toList());
		submissionsQueue.addAll(submissions.stream().filter(s -> s.get("verdict").equals("system error")).collect(Collectors.toList()));
		
		Long submissionsCE = submissions.stream().filter(x -> x.get("verdict").equals("CE")).count();
		Long submissionsEvaluating = submissions.stream().filter(x -> x.get("verdict").equals("judging")).count();
		Long submissionsWaiting = submissions.stream().filter(x -> x.get("verdict").equals("waiting")).count();
		Long submissionsErrors = submissions.stream().filter(x -> x.get("verdict").equals("system error")).count();
		Long submissionsScored = submissions.size() - submissionsCE - submissionsEvaluating - submissionsWaiting - submissionsErrors;
		model.addAttribute("workers", workers);
		model.addAttribute("contests", contests);
		model.addAttribute("submissions", submissions);
		model.addAttribute("queue", submissionsQueue);
		model.addAttribute("submissionsCE", submissionsCE);
		model.addAttribute("submissionsEvaluating", submissionsEvaluating);
		model.addAttribute("submissionsWaiting", submissionsWaiting);
		model.addAttribute("submissionsErrors", submissionsErrors);
		model.addAttribute("submissionsScored", submissionsScored);
    	return "dashboard";
    }
	
	@GetMapping("/admin/contests")
	public String adminContestsPage(Model model) {
		List<Map<String, Object>> contests = repository.listContests();
		model.addAttribute("contests", contests);
		return "contests";
	}
	
	@GetMapping("/admin/workers")
	public String adminWorkersPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> workers = repository.listActiveWorkers();
		model.addAttribute("contests", contests);
		model.addAttribute("workers", workers);
		return "workers";
	}
	
	@GetMapping("/admin/submissions")
	public String adminSubmissionsPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions();
		model.addAttribute("contests", contests);
		model.addAttribute("submissions", submissions);
		return "submissions";
	}
	
	@GetMapping("/admin/contests/{contest_id}")
	public String adminContestPage(@PathVariable("contest_id") int contestId, Model model) {
		Map<String, Object> contest = repository.getContest(contestId).get();
		
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> problems = repository.listContestProblems(contestId);
		List<Map<String,Object>> submissions = repository.listContestSubmissions(contestId);
		model.addAttribute("contests", contests);
		model.addAttribute("problems", problems);
		model.addAttribute("submissions", submissions);
		model.addAttribute("contest", contest);
		return "contest";
	}

	@GetMapping("/admin/submissions/{submission_id}")
	public String adminSubmissionPage(@PathVariable("submission_id") int id,
			Model model) throws Exception {
		List<Map<String,Object>> contests = repository.listContests();
		model.addAttribute("contests", contests);
		Optional<Map<String,Object>> submission = repository.getSubmission(id);
		if (submission.isPresent()) {
			String details = submission.get().get("details").toString();
			if (details != null && !details.isEmpty()) {
				SubmissionScore score = mapper.readValue(details, SubmissionScore.class);
				model.addAttribute("submissionId", String.valueOf(id));
				model.addAttribute("score", Math.round(score.getScore()));
				model.addAttribute("compile", score.getScoreSteps().get("Compile"));
				score.getScoreSteps().remove("Compile");
				model.addAttribute("tests", score.getScoreSteps());
			}
			model.addAttribute("submission", submission.get());
		}
		return "submission";
	}
	
	@GetMapping("/admin/submissions2")
	public String adminSubmissions(Model model) {
		List<Map<String,Object>> submissions = repository.listSubmissions();
		model.addAttribute("submissions", submissions);
		return "submissions2";
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

//	@GetMapping("/admin/problem/{contest}/{number}")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
//	public String showProblem(@PathVariable("contest") String contest,
//			@PathVariable int number,
//			Model model) throws Exception {
//		contest = contest.toLowerCase();
//		
//		Optional<Map<String, Object>> maybeProblem = repository.getProblem(contest, number);
//		if (maybeProblem.isPresent()) {
//			System.out.println(maybeProblem.get());
//			File problemDir = getFile("problem", contest, number+"");
//			TaskParser parser = new TaskParser(problemDir);
//			TaskDetails details = TaskDetails.create(parser);
//			model.addAttribute("problem", maybeProblem.get());
//			model.addAttribute("details", details);
//		}
//
//		return "problem";
//	}

	@PostMapping("/admin/contests")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addContest(@RequestParam("name") String name,
			Model model) throws Exception {
		int id = repository.addContest(name);
		return "redirect:/admin/contests/"+id;
	}
	
	@PostMapping("/admin/workers/ensure")
	public String ensureWorkers(@RequestParam("count") int count,
			Model model) throws Exception {
		count = Math.min(30, count);
		InstanceType type = InstanceType.valueOf("C5Large");
		Configuration configuration = new Configuration()
				.setImageId("ami-099a0966")
				.setInstanceType(type)
				.setSecurityGroup("All")
				.setSecurityKeyName("pesho")
				.setListener(this);

		WorkerManager manager = new WorkerManager(configuration);
		manager.ensureNumberOfInstances(count);
		
		return "redirect:/admin/workers";
	}
	
	@PostMapping("/admin/workers/create")
	public String createWorker(@RequestParam("url") String url,
			Model model) throws Exception {
		repository.addWorker(url);
		return "redirect:/admin/workers";
	}
	
	@PostMapping("/admin/workers/delete")
	public String deleteWorker(@RequestParam("url") String url,
			@RequestParam("destroy") Optional<Boolean> destroy ,
			Model model) throws Exception {
		if (destroy.orElse(false)) {
			url = url.split(":")[0];
			InstanceType type = InstanceType.valueOf("T2Micro");
			Configuration configuration = new Configuration()
					.setImageId("ami-099a0966")
					.setInstanceType(type)
					.setSecurityGroup("All")
					.setSecurityKeyName("pesho")
					.setListener(this);
			
			WorkerManager manager = new WorkerManager(configuration);
			manager.killInstance(url);
		} else {
			repository.deleteWorker(url);
		}
		
		return "redirect:/admin/workers";
	}
	
	@PostMapping("/admin/problem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String addProblem(@RequestPart("file") MultipartFile file, @RequestParam("number") Integer number,
			@RequestParam("name") String name, @RequestParam("contest_id") int contestId,
			Model model) throws Exception {
		
		Optional<Map<String, Object>> maybeProblem = repository.getProblem(contestId, number);

		File zipFile = getFile("problem", String.valueOf(contestId), String.valueOf(number), name + ".zip");
		if (maybeProblem.isPresent()) {
			FileUtils.deleteQuietly(getFile("problem", String.valueOf(contestId), String.valueOf(number)));
		}
		
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = getFile("problem", String.valueOf(contestId), String.valueOf(number), name);
		unzip(zipFile, zipFolder);

		int problemId = 0;
		if (maybeProblem.isPresent()) {
			problemId = (int) maybeProblem.get().get("id");
			repository.updateProblem(problemId, name, zipFile.getName());
		} else {
			problemId = repository.addProblem(name, contestId, number, zipFile.getName());
		}

		final int problemIdFinal = problemId;
		workersQueue.getAll().stream().parallel().forEach(worker -> {
			worker.sendProblemToWorker(problemIdFinal, zipFile.getAbsolutePath());
		});

		return "redirect:/admin/contests/"+contestId;
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
    	if (repository.hasCitySubmissions(city)) {
    		return "redirect:/admin/submissions";
    	}
    	
		File zipFile = getFile("submission", city, city + ".zip");
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = getFile("submission", city, city);
		unzip(zipFile, zipFolder);

		List<File> listSourceFiles = listSourceFiles(zipFolder);
		for (File sourceFile: listSourceFiles) {
			String username = sourceFile.getParentFile().getName();
			String contest = sourceFile.getParentFile().getParentFile().getName();
			String sourceFilePath = sourceFile.getCanonicalPath().replace(new File(workDir).getCanonicalPath(), "");
			if (sourceFilePath.startsWith(File.separator)) {
				sourceFilePath = sourceFilePath.substring(1);
			}
			String problemName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
			repository.addSubmission(city, username, contest, problemName, sourceFilePath);
		}
		
		return "redirect:/admin/submissions";
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