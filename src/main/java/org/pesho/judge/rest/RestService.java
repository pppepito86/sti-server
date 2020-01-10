package org.pesho.judge.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.pesho.grader.step.Verdict;
import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class RestService {
	
	@Value("${work.dir}")
	private String workDir;
	
	@Autowired
	protected Repository repository;
	
	protected ObjectMapper mapper = new ObjectMapper();
	
    @GetMapping("user")
    public Map<String, Object> getUser() {
    	return repository.getUserDetails(getUsername()).orElse(null);
    }
    
    @GetMapping("tasks/info")
    public List<Map<String, Object>> getTasksInfo() {
		Optional<String> contest = getContest();
		Optional<String> contestId = repository.getContestId(getUsername());
		List<Map<String, Object>> tasks = contest.map(c -> repository.listContestTasks(c)).orElse(new ArrayList<>());

		int totalPublicScore = 0;
		int totalMaxPublicScore = 0;
		for (Map<String, Object> task: tasks) {
    		TaskDetails details = getTaskDetails(contestId.get(), task.get("number").toString());
    		task.put("time", details.getTime());
    		task.put("memory", details.getMemory());
			TreeSet<Integer> feedback = feedback(details.getFeedback());
			int totalTests = details.getTestGroups().stream().mapToInt(tg -> tg.getTestCases().size()).sum();
			int publicTests = feedback.size() == 0 ? totalTests : feedback.size();
			int publicScore = (int) (details.getPoints()*publicTests/totalTests+0.5);
			task.put("feedback", publicScore+"/100");			
			int currScore = taskSubmissions(Integer.parseInt(task.get("number").toString())).stream()
					.map(s -> s.get("points"))
					.filter(Objects::nonNull)
					.mapToInt(p -> (int) p)
					.max().orElse(0);
			task.put("publicScore", currScore);
			task.put("maxPublicScore", publicScore);
			totalPublicScore += currScore;
			totalMaxPublicScore += publicScore;
		}
		
		Map<String, Object> common = new HashMap<>();
		common.put("name", "общо");
		common.put("time", "");
		common.put("memory", "");
		common.put("submissions", "50");
		common.put("publicScore", totalPublicScore);
		common.put("maxPublicScore", totalMaxPublicScore);
		tasks.add(common);
    	
    	return tasks;
    }

    @GetMapping("tasks/{taskId}/full")
    public Map<String, Object> getTaskFull(@PathVariable int taskId) {
    	if (!contestHasStarted()) {
    		return new HashMap<>();
    	}
    	
    	List<Map<String, Object>> submissions = taskSubmissions(taskId);
    	
		return repository.getContestTask(getUsername(), taskId).map(task -> {
			TaskDetails details = getTaskDetails(task.get("contestId").toString(), task.get("number").toString());
			task.put("time", details.getTime());
			task.put("memory", details.getMemory());
			task.put("submissions", submissions);
			return task;
		}).orElse(null);
    }
    
    @GetMapping("tasks/{taskId}")
    public Map<String, Object> getTask(@PathVariable int taskId) {
		return repository.getContestTask(getUsername(), taskId).map(task -> {
			TaskDetails details = getTaskDetails(task.get("contestId").toString(), task.get("number").toString());
			task.put("time", details.getTime());
			task.put("memory", details.getMemory());
	    	try {
				TreeSet<Integer> feedback = feedback(Integer.valueOf(task.get("id").toString()));
				int totalTests = details.getTestGroups().stream().mapToInt(tg -> tg.getTestCases().size()).sum();
				int publicTests = feedback.size() == 0 ? totalTests : feedback.size();
				int publicScore = (int) (details.getPoints()*publicTests/totalTests+0.5);
				task.put("maxPublicScore", publicScore);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return task;
		}).orElse(null);
    }
    
    @GetMapping("tasks/{taskId}/submissions")
    public List<Map<String, Object>> getTaskSubmissions(@PathVariable int taskId) {
    	if (!contestHasStarted()) {
    		return new ArrayList<>();
    	}
    	
		return repository.getContestTask(getUsername(), taskId).map(task -> {
			return taskSubmissions(taskId);
		}).orElse(null);
    }

    @GetMapping("timeToSubmit")
    public Map<String, Object> getTimeToNextSubmit() {
    	HashMap<String, Object> ans = new HashMap<>();
    	
		Map<String, Object> contest = repository.getContest(getUsername()).get();
		Timestamp endTime = (Timestamp) contest.get("end_time");
		long timeLeft = endTime.getTime() - System.currentTimeMillis();
		ans.put("timeLeft", timeLeft);

		List<Map<String,Object>> submission = repository.getUserSubmissions(getUsername());
		if (submission.isEmpty()) {
			ans.put("timeToSubmit", 0);
		} else {
			Timestamp lastSubmissionTime = (Timestamp) submission.get(0).get("upload_time");
			long diff = lastSubmissionTime.getTime() + 60 * 1000 - System.currentTimeMillis();
			if (diff > 0) ans.put("timeToSubmit", diff);
			else ans.put("timeToSubmit", 0);
		}
		return ans;
	}
    
	@RequestMapping("/tasks")
	public List<Map<String, Object>> tasks() {
		Optional<String> contest = getContest();
		return contest.map(c -> repository.listContestTasks(c))
				.orElse(null);
	}

    @GetMapping("/tasks/{taskNumber}/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable("taskNumber") int number,
			@RequestParam(value = "download", defaultValue = "false") boolean download) throws Exception {
    	if (!contestHasStarted()) {
    		throw new RuntimeException("Състезанието не е започнало");
    	}
    	
    	String contestId = repository.getContestId(getUsername()).get();
	    return downloadPdf(Integer.valueOf(contestId), number, download);
    }
    
    public ResponseEntity<?> downloadPdf(int contestId, int number, boolean download) throws Exception {
    	HttpHeaders respHeaders = new HttpHeaders();
    	if (download) {
    		respHeaders.setContentDispositionFormData("attachment", "problem" + number + ".pdf");
    	} else {
    		respHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    	}
	    
	    File problemDir = getFile("problem", String.valueOf(contestId), String.valueOf(number));
	    File pdf = findPdf(problemDir);
	    if (pdf == null) return ResponseEntity.noContent().build();
	    
	    InputStream is = new FileInputStream(pdf);
		InputStreamResource inputStreamResource = new InputStreamResource(is);
	    
		return new ResponseEntity<InputStreamResource>(inputStreamResource, 
	    		respHeaders, HttpStatus.OK);
    }

	protected File findPdf(File problemDir) {
		File[] files = problemDir.listFiles();
		if (files == null) return null;
		
		for (File file: files) {
			if (file.isFile() && file.getName().endsWith(".pdf")) {
				return file;
			}
			if (file.isDirectory()) {
				File pdf = findPdf(file);
				if (pdf != null) return pdf;
			}
		}
		return null;
	}
    
	@RequestMapping("/tasks/{problemNumber}/solutions")
	public ResponseEntity<?> taskSolutions(
			@PathVariable int problemNumber) {
    	if (!contestHasStarted()) {
    		return ResponseEntity.ok(new ArrayList<>());
    	}
		
		List<Map<String, Object>> submissions = taskSubmissions(problemNumber);
		return ResponseEntity.ok(submissions);
	}

    @RequestMapping("/tasks/{problemNumber}/solutions/{solutionNumber}")
    public ResponseEntity<?> taskSolution(
    		@PathVariable int problemNumber,
    		@PathVariable int solutionNumber) throws Exception {
    	if (!contestHasStarted()) {
    		return ResponseEntity.ok(new ArrayList<>());
    	}

    	List<Map<String,Object>> submissions = taskSubmissions(problemNumber);
    	Map<String, Object> submission = submissions.get(submissions.size()-solutionNumber);
    	
		List<List<Integer>> groups = groups(Integer.valueOf(submission.get("problem_id").toString()));
		submission.put("groups", groups);
    	
    	TreeSet<Integer> feedback = feedback(Integer.valueOf(submission.get("problem_id").toString()));
		submission.put("verdict", fixVerdict(submission.get("verdict").toString(), feedback));
		
    	String details = repository.getSubmissionDetails(Integer.parseInt(submission.get("id").toString())).orElse(null);
		if (details != null && !details.isEmpty()) {
			SubmissionScore score = mapper.readValue(details, SubmissionScore.class);
			for(String key: score.getScoreSteps().keySet()) {
				StepResult stepResult = score.getScoreSteps().get(key);
				if (!key.startsWith("Test")) continue;
				
				int testId = Integer.valueOf(key.split("Test")[1]);
				if (feedback.size() == 0 || feedback.contains(testId)) continue;
						
				stepResult.setExpectedOutput("");
				stepResult.setOutput("");
				stepResult.setReason("");
				stepResult.setTime(null);
				stepResult.setVerdict(Verdict.HIDDEN);
			}

			if (feedback.size() == 0) {
				submission.put("score", Math.round(score.getScore()));
			} else {
				submission.put("score", "?");
			}
			submission.put("compile", score.getScoreSteps().get("Compile"));
			score.getScoreSteps().remove("Compile");
			
			LinkedHashMap<String,StepResult> scoreSteps = score.getScoreSteps();
			ArrayList<HashMap<String, Object>> newScoreSteps = new ArrayList<>();
			for (Map.Entry<String, StepResult> entry: scoreSteps.entrySet()) {
				HashMap<String, Object> m = new HashMap<>();
				m.put("name", entry.getKey());
				m.put("verdict", entry.getValue().getVerdict());
				m.put("reason", entry.getValue().getReason());
				m.put("time", entry.getValue().getTime());
				newScoreSteps.add(m);
			}
			
			submission.put("tests", newScoreSteps);
			
			LinkedHashMap<String, StepResult> steps = new LinkedHashMap<>();
			if (groups.size() <= score.getScoreSteps().size()) {
				for (int i = 0; i < groups.size(); i++) {
					StepResult step = new StepResult();
					boolean hidden = false;
					boolean ok = true;
					for (int t: groups.get(i)) {
						Verdict v = score.getScoreSteps().get("Test"+t).getVerdict();
						if (v == Verdict.HIDDEN) hidden = true;
						if (v != Verdict.OK) ok = false;
					}
					if (hidden) step.setVerdict(Verdict.HIDDEN);
					else if (ok) step.setVerdict(Verdict.OK);
					steps.put("Group"+(i+1), step);
					
					for (int j = 0; j < groups.get(i).size(); j++) {
						steps.put("Test"+groups.get(i).get(j), score.getScoreSteps().get("Test"+groups.get(i).get(j)));
					}
				}
			}
			submission.put("steps", steps);
		}
		
		File sourceFile = getFile("submissions", String.valueOf(submission.get("id")), submission.get("file").toString());
		String source = FileUtils.readFileToString(sourceFile, Charset.forName("UTF-8"));
		submission.put("source", source);
    	
		return ResponseEntity.ok(submission);
    }
	
	private String getUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	return authentication.getName();
	}
	
	private TaskDetails getTaskDetails(String contestId, String problemNumber) {
    	File problemDir = Paths.get(workDir, "problem", contestId, problemNumber).toFile();
		TaskParser parser = new TaskParser(problemDir);
		TaskDetails details = TaskDetails.create(parser);
		return details;
    }
	
	private Optional<String> getContest() {
		return repository.getUserDetails(getUsername())
				.map(user -> user.get("contest"))
				.map(Object::toString);
	}
    
	protected String fixVerdict(String verdict, TreeSet<Integer> feedback) {
		if (feedback.size() == 0) return verdict;
		
		String[] split = verdict.split(",");
		if (split.length < feedback.last()) return verdict;
		
		for (int i = 1; i <= split.length; i++) {
			if (!feedback.contains(i)) split[i-1] = "?";
		}
		return String.join(",", split);
	}
	
	protected int visibleScore(String verdict, TreeSet<Integer> feedback) {
		if (feedback.size() == 0) return 0;
		
		String[] split = verdict.split(",");
		if (split.length < feedback.last()) return 0;
		
		int okTests = 0;
		for (int i = 1; i <= split.length; i++) {
			if (feedback.contains(i) && split[i-1].equalsIgnoreCase("OK")) okTests++;
		}
		int ans = (int) (okTests*100/split.length+0.5);
		return ans;
	}
	
	private List<List<Integer>> groups(int problemId) throws Exception {
		Map<String, Object> problem = repository.getProblem(problemId).get();
		File problemDir = getFile("problem", problem.get("contest_id").toString(), problem.get("number").toString());

		TaskParser parser = new TaskParser(problemDir);
		TaskDetails details = TaskDetails.create(parser);
		String groups = details.getGroups();
		
		List<List<Integer>> result = new ArrayList<>();
		if (groups.isEmpty()) return result;
		
		String[] split = groups.split(",");
		for (int i = 0; i < split.length; i++) {
			String[] split2 = split[i].split("-");
			int start = Integer.valueOf(split2[0]);
			int end = Integer.valueOf(split2[1]);

			List<Integer> list = new ArrayList<>();
			for (int j = start; j <= end; j++) list.add(j);
			
			result.add(list);
		}
		
		return result;
	}
	
	private TreeSet<Integer> feedback(int problemId) throws Exception {
		Map<String, Object> problem = repository.getProblem(problemId).get();
		File problemDir = getFile("problem", problem.get("contest_id").toString(), problem.get("number").toString());

		TaskParser parser = new TaskParser(problemDir);
		TaskDetails details = TaskDetails.create(parser);
		return feedback(details.getFeedback());
	}
	
	protected TreeSet<Integer> feedback(String feedback) {
		TreeSet<Integer> set = new TreeSet<>();
		if (feedback.trim().equalsIgnoreCase("full")) return set;
		
		String[] split = feedback.split(",");
		for (String s: split) set.add(Integer.valueOf(s.trim()));
		return set;
	}

	protected File getFile(String type, String city, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + fileName;
		return new File(path).getAbsoluteFile();
	}
	
	protected File getFile(String type, String city, String group, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + group + File.separator + fileName;
		return new File(path).getAbsoluteFile();
	}
	
	private List<Map<String, Object>> taskSubmissions(int problemNumber) {
		List<Map<String,Object>> submissions = repository.listSubmissions(getUsername(), problemNumber);
    	for (int i = 0; i < submissions.size(); i++) {
    		submissions.get(i).put("number", submissions.size()-i);
    	}
    	String contestId = repository.getContestId(getUsername()).orElse(null);
    	TaskDetails details = getTaskDetails(contestId, String.valueOf(problemNumber));
    	submissions.forEach(submission -> {
    		TreeSet<Integer> feedback = feedback(details.getFeedback());
			submission.put("verdict", fixVerdict(submission.get("verdict").toString(), feedback));
			if (feedback.size() != 0) submission.put("points", visibleScore(submission.get("verdict").toString(), feedback));
    	});
		return submissions;
	}

	@RequestMapping("/time")
	public ResponseEntity<?> timeLeft() {
		return ResponseEntity.ok(getTimes());
	}
	
	boolean contestHasStarted() {
		Timestamp startTime = (Timestamp) repository.getContest(getUsername()).get().get("start_time");
		return startTime.getTime() <= System.currentTimeMillis();
	}
	
	private HashMap<String, Object> getTimes() {
		long currentTime = System.currentTimeMillis();
		Timestamp startTime = (Timestamp) repository.getContest(getUsername()).get().get("start_time");
		Timestamp endTime = (Timestamp) repository.getContest(getUsername()).get().get("end_time");
		long timeTillStart = startTime.getTime() - currentTime;
		long timeTillEnd = endTime.getTime() - currentTime;

		HashMap<String, Object> map = new HashMap<>();
		map.put("timeTillStart", timeTillStart);
		map.put("timeTillEnd", timeTillEnd);
		
		return map;
	}
		
	@PostMapping("/tasks/{problemNumber}/solutions")
	public Map<String, Object> submitCode(
			@PathVariable("problemNumber") Integer problemNumber,
			@RequestParam("ip") String localIp,
			@RequestParam("code") Optional<String> maybeCode,
			@RequestPart("file") Optional<MultipartFile> maybeFile) throws Exception {
    	if (!contestHasStarted()) {
    		return new HashMap<>();
    	}

		
		if (maybeFile.isPresent()) {
			MultipartFile file = maybeFile.get();
			if (file.getSize() > 64 * 1024) {
				throw new RuntimeException("{\"error\":6}");
			}
			
			if (file.isEmpty()) {
				throw new RuntimeException("{\"error\":4}");
			}
			
			if (!file.getOriginalFilename().toLowerCase().endsWith(".cpp") && !file.getOriginalFilename().toLowerCase().endsWith(".c")) {
				throw new RuntimeException("{\"error\":5}");
			}
			
			return addSubmission(file, null, problemNumber, localIp);
		} else if (maybeCode.isPresent()) {
			String code = maybeCode.get();
			if (code.trim().isEmpty()) {
				throw new RuntimeException("{\"error\":7}");
			}
			return addSubmission(null, code, problemNumber, localIp);
		} else {
			throw new RuntimeException("{\"error\":\"no code\"}");
		}
	}
	
	public Map<String, Object> addSubmission(MultipartFile file, String code, Integer problemNumber, String localIp) throws IOException {
		long submissionTime = System.currentTimeMillis();
		
		Map<String, Object> user = repository.getUserDetails(getUsername()).orElse(null);
		String username = user.get("name").toString();
		String city = user.get("city").toString();
		
		String contest = user.get("contest").toString();
		String problemName = repository.getProblem(contest, problemNumber).get().get("name").toString();
		
		String fileName = problemName + ".cpp";
		if (file != null && file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".c")) {
			fileName = problemName + ".c";
		}
		
		List<Map<String,Object>> submission = repository.getUserSubmissions(SecurityContextHolder.getContext().getAuthentication().getName());
		if (!submission.isEmpty()) {
			Timestamp lastSubmissionTime = (Timestamp) submission.get(0).get("upload_time");
			if (lastSubmissionTime.getTime() + 60 * 1000 > submissionTime) {
				throw new RuntimeException("{\"error\":1}");
			}
		}
		if (submission.size() >= 50) {
			throw new RuntimeException("{\"error\":7}");
		}
				
		Timestamp endTime = (Timestamp) repository.getContest(getUsername()).get().get("end_time");
		if (submissionTime > endTime.getTime()) {
			throw new RuntimeException("{\"error\":\"contest is over\"}");
		}
		
		int submissionId = repository.addSubmission(city, username, contest, problemName, fileName);
		if (localIp == null) localIp = "";
		repository.addIpLog(username, "SUBMISSION " + submissionId, localIp, getPublicIp());
		
		if (submissionId == 0) {
			String details = String.format("%s_%s_%s_%s", city, contest, username, problemName);
			repository.addLog("submission", "problem not found for " + details, "");
			throw new RuntimeException("{\"error\":\"problem not found for "+details+"\"}");
		}
		
		File sourceFile = getFile("submissions", String.valueOf(submissionId), fileName);
		if (file != null) {
			FileUtils.copyInputStreamToFile(file.getInputStream(), sourceFile);
		} else {
			FileUtils.writeStringToFile(sourceFile, code);
		}
		int submissionNumber = repository.userSubmissionsNumberForProblem(username, problemNumber, submissionId);
		HashMap<String, Object> response = new HashMap<>();
		response.put("sid", submissionNumber);
		return response;
	}
    
	public String getPublicIp() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
		        .getRequest();
		String publicIp = request.getRemoteAddr();
		if (publicIp == null) publicIp = "";
		return publicIp;
	}

	@GetMapping("/questions")
	public List<Map<String, Object>> listQuestions() throws Exception {
		return repository.listQuestions(getUsername());
	}
	
	@GetMapping("/announcements")
	public List<Map<String, Object>> listAnnouncements() throws Exception {
		List<Map<String, Object>> announcements = repository.listAnnouncements(getUsername());
		List<Map<String, Object>> seenAnnouncements = repository.listSeenAnnouncements(getUsername());
		Set<Integer> seen = seenAnnouncements.stream().map(a -> (Integer) a.get("announcement_id")).collect(Collectors.toSet());
		
		for (Map<String,Object> a : announcements) {
			int id = (int) a.get("id");
			boolean isSeen = seen.contains(id);
			a.put("seen", isSeen);
		}
		
		return announcements;
	}
	
	@PostMapping("/questions")
	public Map<String, Object> submitQuestion(
			@RequestParam("topic") String topic,
			@RequestParam("question") String question,
			@RequestParam("ip") String localIp) throws Exception {
		int id = repository.addQuestion(getUsername(), topic, question);
		if (id == 0) throw new RuntimeException("bad id");
		
		HashMap<String, Object> ans = new HashMap<>();
		ans.put("id", id);
		return ans;
	}

	@GetMapping("/unread")
	public Map<String, Object> unreedQuestions() throws Exception {
		HashMap<String, Object> ans = new HashMap<>();
		ans.put("questions", repository.unreadQuestions(getUsername()));
		return ans;
	}
	
	@PostMapping("/questions/seen")
	public Map<String, Object> seenQuestion(
			@RequestParam("id") int id) throws Exception {
		repository.setAnswerSeen(id);
		HashMap<String, Object> ans = new HashMap<>();
		ans.put("id", id);
		return ans;
	}
	
	@PostMapping("/announcements/seen")
	public Map<String, Object> seenAnnouncement(
			@RequestParam("id") int id) throws Exception {
		repository.setAnnouncementSeen(id, getUsername());
		HashMap<String, Object> ans = new HashMap<>();
		ans.put("id", id);
		return ans;
	}
}
