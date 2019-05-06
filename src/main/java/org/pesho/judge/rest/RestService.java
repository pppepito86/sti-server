package org.pesho.judge.rest;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestService {
	
	@Value("${work.dir}")
	private String workDir;
	
	@Autowired
	protected Repository repository;
	
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

    @RequestMapping("/me")
    public ResponseEntity<?> me() {
    	return repository.getUserDetails(getUsername())
    			.map(ResponseEntity::ok)
    	       .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping("/tasks")
    public ResponseEntity<?> tasks() {
    	Optional<String> contest = getContest();
    	return contest.map(c -> repository.listContestTasks(c))
    		.map(ResponseEntity::ok)
	       .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping("/tasks/{problemNumber}")
    public ResponseEntity<?> taskDetails(@PathVariable int problemNumber) {
    	return repository.getContestTask(getUsername(), problemNumber).map(task -> {
    		TaskDetails details = getTaskDetails(task.get("contestId").toString(), task.get("number").toString());
    		task.put("time", details.getTime());
    		task.put("memory", details.getMemory());
    		return task;
    	}).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        
    }
    
    @RequestMapping("/tasks/{problemNumber}/solutions")
    public ResponseEntity<?> taskSolutions(
    		@PathVariable int problemNumber) {
    	List<Map<String, Object>> submissions = taskSubmissions(problemNumber);
    	return ResponseEntity.ok(submissions);
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
    	for (int i = submissions.size()-1; i >= 0; i--) {
    		submissions.get(i).put("number", i+1);
    	}
    	String contestId = repository.getContestId(getUsername()).orElse(null);
    	TaskDetails details = getTaskDetails(contestId, String.valueOf(problemNumber));
    	submissions.forEach(submission -> {
    	TreeSet<Integer> feedback = feedback(details.getFeedback());
			submission.put("verdict", fixVerdict(submission.get("verdict").toString(), feedback));
			if (feedback.size() != 0) submission.put("points", "?");
    	});
		return submissions;
	}

    @RequestMapping("/tasks/{problemNumber}/solutions/{solutionNumber}")
    public ResponseEntity<?> taskSolution(
    		@PathVariable int problemNumber,
    		@PathVariable int solutionNumber) {
    	List<Map<String,Object>> submissions = taskSubmissions(problemNumber);
    	//TODO validate range
    	return ResponseEntity.ok(submissions.get(solutionNumber-1));
    }
    
}
