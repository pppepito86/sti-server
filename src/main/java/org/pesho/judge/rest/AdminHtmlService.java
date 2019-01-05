package org.pesho.judge.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.pesho.judge.Worker;
import org.pesho.judge.util.HomographTranslator;
import org.pesho.workermanager.Configuration;
import org.pesho.workermanager.WorkerManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Tag;

@PreAuthorize("hasRole('ADMIN')")
@Controller
public class AdminHtmlService extends HtmlService {

	@GetMapping("/admin/communication")
    public String adminCommunication(Model model) {
		
    	return "communication";
    }
	
	@PostMapping("/admin/send-message")
    public String adminSendMessage(@RequestParam("message") String message,
    		@RequestParam("msg-type") String messageType,
    		@RequestParam("username") Optional<String> username,
    		@RequestParam("contest-name") Optional<String> contestname) {
		
		System.out.println(message + " " + messageType);
    	return "redirect:/admin/communication";
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
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Calendar startTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar endTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		endTime.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY) + 4);
		model.addAttribute("startTime", sdf.format(startTime.getTime()));
		model.addAttribute("endTime", sdf.format(endTime.getTime()));
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
	
	@GetMapping("/admin/users")
	public String adminUsersPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> users = repository.listUsers();
		model.addAttribute("contests", contests);
		model.addAttribute("users", users);
		return "users";
	}
	
	@GetMapping("/admin/submissions")
	public String adminSubmissionsPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions();
		model.addAttribute("contests", contests);
		model.addAttribute("submissions", submissions);
		return "submissions";	
	}

	@GetMapping("/admin/results")
	public String adminResultsPage(Model model) {
		List<Map<String,Object>> users = repository.listUsers();
		HashMap<String, String> names = new HashMap<>();
		for (Map<String, Object> user: users) {
			names.put(user.get("name").toString(), user.get("display_name").toString());
		}
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			info.put("display_name", names.getOrDefault(submission.get("username").toString().toUpperCase(), "N/A"));
			totals.put(key, info);
		}
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, false);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "results";
	}
	
	@GetMapping("/admin/resultsfull")
	public String adminResultsFullPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			totals.put(key, info);
		}
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, true);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "resultsfull";
	}
	
	@GetMapping("/admin/results/{contest}")
	public String adminContestResultsPage(@PathVariable("contest") String contest, Model model) {
		List<Map<String,Object>> users = repository.listUsers();
		HashMap<String, String> names = new HashMap<>();
		for (Map<String, Object> user: users) {
			names.put(user.get("name").toString(), user.get("display_name").toString());
		}
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> contest.equalsIgnoreCase(x.get("contest_name").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			info.put("display_name", names.getOrDefault(submission.get("username").toString().toUpperCase(), "N/A"));
			totals.put(key, info);
		}
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, false);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "results";
	}
	
	@GetMapping("/admin/logs")
	public String adminLogsPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> logs = repository.listLogs();
		model.addAttribute("contests", contests);
		model.addAttribute("logs", logs);

		return "logs";
	}
	
	@PostMapping("/admin/contests/{contest_id}/time")
	public String updateContestTime(
			@PathVariable("contest_id") int contestId, 
			@RequestParam("start_time") String startTime,
			@RequestParam("end_time") String endTime,
			Model model) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		long start = sdf.parse(startTime).getTime();
		long end = sdf.parse(endTime).getTime();
		repository.updateContest(contestId, new Timestamp(start), new Timestamp(end));
		
		return "redirect:/admin/contests/" + contestId;
	}

	@GetMapping("/admin/contests/{contest_id}")
	public String adminContestPage(@PathVariable("contest_id") int contestId, Model model) {
		Map<String, Object> contest = repository.getContest(contestId).get();
		
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> problems = repository.listContestProblems(contestId);
		for (Map<String,Object> problem: problems) {
			File problemDir = getFile("problem", String.valueOf(contestId), problem.get("number").toString());
			TaskParser parser = new TaskParser(problemDir);
			TaskDetails details = TaskDetails.create(parser);
			problem.put("details", details);
		}
		List<Map<String,Object>> submissions = repository.listContestSubmissions(contestId);
		model.addAttribute("contests", contests);
		model.addAttribute("problems", problems);
		model.addAttribute("submissions", submissions);
		model.addAttribute("contest", contest);
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Timestamp startTime = (Timestamp) contest.get("start_time");
		Timestamp endTime = (Timestamp) contest.get("end_time");

		model.addAttribute("startTime", sdf.format(startTime.getTime()));
		model.addAttribute("endTime", sdf.format(endTime.getTime()));
		return "contest";
	}
	
	@GetMapping("/admin/contests/{contest_id}/problems/{problem_number}")
	public String adminContestProblemPage(
			@PathVariable("contest_id") int contestId, 
			@PathVariable("problem_number") int number,
			Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		model.addAttribute("contests", contests);
		Optional<Map<String,Object>> contest = repository.getContest(contestId);
		model.addAttribute("contest", contest.get());
		
		Optional<Map<String,Object>> problem = repository.getProblem(contestId, number);
		if (problem.isPresent()) {
			File problemDir = getFile("problem", String.valueOf(contestId), String.valueOf(number));
			TaskParser parser = new TaskParser(problemDir);
			TaskDetails details = TaskDetails.create(parser);
			model.addAttribute("problem", problem.get());
			model.addAttribute("details", details);
		}
		
		return "problem";
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
				model.addAttribute("score", Math.round(score.getScore()));
				model.addAttribute("compile", score.getScoreSteps().get("Compile"));
				score.getScoreSteps().remove("Compile");
				model.addAttribute("tests", score.getScoreSteps());
			}
			File sourceFile = getFile("submissions", String.valueOf(submission.get().get("id")), submission.get().get("file").toString());
			String source = FileUtils.readFileToString(sourceFile, Charset.forName("UTF-8"));
			model.addAttribute("submissionId", String.valueOf(id));
			model.addAttribute("source", source);
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
	public String addContest(
			@RequestParam("name") String name,
			@RequestParam("start_time") String startTime,
			@RequestParam("end_time") String endTime,
			Model model) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		long start = sdf.parse(startTime).getTime();
		long end = sdf.parse(endTime).getTime();
		int id = repository.addContest(name, new Timestamp(start), new Timestamp(end));
		return "redirect:/admin/contests/"+id;
	}
	
	@PostMapping("/admin/workers/ensure")
	public String ensureWorkers(@RequestParam("count") int count,
			Model model) throws Exception {
		count = Math.min(20, count);
		int manualCount = (int) repository.listWorkers().stream().filter(w -> "manual".equals(w.get("type"))).count();
		int automaticCount = count-manualCount; // Math.max((count - manualCount + 1) / 2, 0);
		InstanceType type = InstanceType.valueOf("C4Large");
		Configuration configuration = new Configuration()
				.setImageId("ami-0653cb104a8c7e20c")
				.setInstanceType(type)
				.setSecurityGroup("All")
				.setSecurityKeyName("noi")
				.setWorkerTag(new Tag("type", "noi2_worker"))
				.setListener(this);

		WorkerManager manager = new WorkerManager(configuration);
		manager.ensureNumberOfInstances(automaticCount);
		
		return "redirect:/admin";
	}
	
	@PostMapping("/admin/workers/create")
	public String createWorker(@RequestParam("url") String url,
			Model model) throws Exception {
		repository.addWorker(url, "manual");
		workersQueue.put(new Worker(url));
		
		return "redirect:/admin";
	}
	
	@PostMapping("/admin/workers/delete")
	public String deleteWorker(@RequestParam("url") String url,
			Model model) throws Exception {
		workersQueue.remove(url);
		repository.deleteWorker(url);
		
		return "redirect:/admin";
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
		
		String checksum = getChecksum(zipFile);
		File zipFolder = getFile("problem", String.valueOf(contestId), String.valueOf(number), name);
		unzip(zipFile, zipFolder);
		
		sanitize(zipFile, zipFolder);
		zip(zipFile, zipFolder);

		int problemId = 0;
		if (maybeProblem.isPresent()) {
			problemId = (int) maybeProblem.get().get("id");
			repository.updateProblem(problemId, name, zipFile.getName(), checksum);
		} else {
			problemId = repository.addProblem(name, contestId, number, zipFile.getName(), checksum);
		}

		return "redirect:/admin/contests/"+contestId;
	}

	private void sanitize(File zipFile, File zipFolder) throws IOException {
		TaskParser parser = new TaskParser(zipFolder);
		for (File input: parser.getInput()) {
			sanitize(input);
		}
		for (File output: parser.getOutput()) {
			sanitize(output);
		}
	}

	private void sanitize(File file) throws IOException {
		String content = FileUtils.readFileToString(file);
		content = content.replace("\r\n", "\n");
		content = content.replace("\r", "\n");
		FileUtils.writeStringToFile(file, content);
	}

	@GetMapping("/admin/download-users")
	public ResponseEntity<InputStreamResource> downloadUsers() {
		HttpHeaders respHeaders = new HttpHeaders();
	    respHeaders.setContentDispositionFormData("attachment", "users.csv");
	    
	    StringBuffer buffer = new StringBuffer();
	    
	    
	    final String[] displayColumnNames = new String[] {"ID", "Name", 
	    		"Username", "Password", "School"};
	    final String[] dbColumns = new String[] {"id", "display_name", 
	    		"name", "password", "school"};
	    
	    try (
            CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT
                    .withHeader(displayColumnNames));
        ) {
	    	List<Map<String, Object>> users = repository.listUsers();
	    	for (Map<String, Object> user : users) {
	    		Object[] values = Arrays
	    				.stream(dbColumns)
	    				.map(column -> user.get(column))
	    				.toArray();
	    		csvPrinter.printRecord(values);
	    	}
            csvPrinter.flush();  
        } catch (IOException e) {
			e.printStackTrace();
		}
	    
	    InputStream is = new ByteArrayInputStream(buffer.toString().getBytes());
		InputStreamResource inputStreamResource = new InputStreamResource(is);
	    
		return new ResponseEntity<InputStreamResource>(inputStreamResource, 
	    		respHeaders, HttpStatus.OK);
	}
			
			
	@PostMapping("/admin/upload-users")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public synchronized String uploadUsers(@RequestPart("file") MultipartFile file, 
			@RequestParam("name-column-index") int nameColumnIndex,
			@RequestParam("group-column-index") int groupColumnIndex, 
			@RequestParam("school-column-index") Optional<Integer> schoolColumnIndex,
			@RequestParam("grade-column-index") Optional<Integer> gradeColumnIndex,
			Model model)
			throws Exception {
		
		
		InputStreamReader reader = new InputStreamReader(file.getInputStream());
		CSVParser csv = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
		Map<String, Integer> groupNumUsersMap = new HashMap<String, Integer>();
		
		try {
		    for (final CSVRecord record : csv) {
		        String name = record.get(nameColumnIndex);
		        String group = record.get(groupColumnIndex);
		        group = group.substring(0, 1);
		        group = new HomographTranslator().translate(group);
		        
		        String password = Long.toHexString(Double.doubleToLongBits(Math.random()));
		        
		        
		        if (!groupNumUsersMap.containsKey(group)) {
		        	groupNumUsersMap.put(group, 0);
		        }
		        int id = groupNumUsersMap.get(group);
		        groupNumUsersMap.put(group, id+1);
		        String username = group + String.format("%03d", id);
		        
		        String school = null;
		        if (schoolColumnIndex.isPresent()) {
		        	school = record.get(schoolColumnIndex.get());
		        }
		        
		        String grade = null;
		        if (gradeColumnIndex.isPresent()) {
		        	grade = record.get(gradeColumnIndex.get());
		        }
		        
		        repository.addUser(username, password, name, group, school, grade);
		    }
		} finally {
		    csv.close();
		    reader.close();
		}
		
		return "redirect:/admin/users";
	}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/grade")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public synchronized String addAdminSubmission(@RequestPart("file") MultipartFile file, 
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
