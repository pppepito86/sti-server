package org.pesho.judge.rest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@PreAuthorize("hasRole('USER')")
@Controller
public class UserHtmlService extends HtmlService {

	@GetMapping("/user/problem/{problem-number}")
    public String userProblem(Model model, 
    		@PathVariable("problem-number") Integer problemNumber) {

		int contestId = getCurrentUserContestId();

		String problemNumStr = Integer.toString(problemNumber);
		File problemDir = getFile("problem", String.valueOf(contestId), problemNumStr);
		TaskParser parser = new TaskParser(problemDir);
		TaskDetails details = TaskDetails.create(parser);
		model.addAttribute("problemDetails", details);
		
		Map<String,Object> problem = repository.getProblem(contestId, problemNumber).get();
		
		model.addAttribute("problem", problem);
		model.addAttribute("problemNumber", problemNumber);
		
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		List<Map<String, Object>> submissions = 
				repository.listUserSubmissionsForProblem(username, problemNumber);
		
		int number = submissions.size();
		for (Map<String, Object> submission: submissions) {
			submission.put("number", number--);
			
			TreeSet<Integer> feedback = feedback(details.getFeedback());
			submission.put("verdict", fixVerdict(submission.get("verdict").toString(), feedback));
			if (feedback.size() != 0) submission.put("points", "?");
		}

		model.addAttribute("submissions", submissions);
		
		addContestProblemsToModel(model, contestId);
		model.addAttribute("problemNumber", problemNumber);
		
		addIsStarted(model, contestId);
		addTimeLeft(model, contestId);
		
    	return "user/problem";
    }


    @GetMapping("/user/problem/{number}/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable("number") int number,
			@RequestParam(value = "download", defaultValue = "false") boolean download) throws Exception {
	    int contestId = getCurrentUserContestId();
		if (!isStarted(contestId)) {
			return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
		}
	    
	    return downloadPdf(contestId, number, download);
    }
	
	@PostMapping("/user/submit-code")
	public String submitFile(@RequestParam("code") String code, 
			@RequestParam("problemNumber") Integer problemNumber,
			Model model) throws Exception {
		if (code.trim().isEmpty()) {
			return "redirect:/user/error?msg=7";
		}
		
		return addSubmission(null, code, problemNumber);
	}
	
	@PostMapping("/user/submit-file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String submitFile(@RequestPart("file") MultipartFile file, 
			@RequestParam("problemNumber") Integer problemNumber,
			Model model) throws Exception {
		
		if (file.getSize() > 64 * 1024) {
			return "redirect:/user/error?msg=6";
		}
		
		if (file.isEmpty()) {
			return "redirect:/user/error?msg=4";
		}
		
		if (!file.getOriginalFilename().toLowerCase().endsWith(".cpp")) {
			return "redirect:/user/error?msg=5";
		}
		
		return addSubmission(file, null, problemNumber);
	}
	
	@GetMapping("/user/problem/{problem_number}/submissions/{submission_number}")
	public String userSubmissionPage(@PathVariable("problem_number") int problemNumber,
			@PathVariable("submission_number") int submissionNumber,
			Model model) throws Exception {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		List<Map<String, Object>> submissions = 
				repository.listUserSubmissionsForProblem(username, problemNumber);
		
		if (submissionNumber < 1 || submissionNumber > submissions.size()) {
			return "redirect:/user/error?msg=2";
		}
		
		Map<String, Object> submission = submissions.get(submissions.size() - submissionNumber);
		return showUserSubmissionPage((int) submission.get("id"), model);
	}
	
	@GetMapping("/user/error")
	public String userError(
			@RequestParam("msg") String code,
			Model model) {
		int contestId = getCurrentUserContestId();
		addContestProblemsToModel(model, contestId);
		
		addTimeLeft(model, contestId);
		model.addAttribute("code", code);
		
		return "user/error";
	}

//	@GetMapping("/user/submissions/{submission_id}")
//	public String userSubmissionPage(@PathVariable("submission_id") int id,
//			Model model) throws Exception {
//		return showUserSubmissionPage(id, model);
//	}

//	@GetMapping("/user")
//    public String userDashboard(Model model) {
//		List<Map<String,Object>> workers = repository.listWorkers();
//		List<Map<String,Object>> contests = repository.listContests();
//		List<Map<String,Object>> submissions = repository.listDetailedSubmissions();
//		List<Map<String,Object>> submissionsQueue = submissions.stream()
//				.filter(s -> s.get("verdict").equals("waiting") 
//						|| s.get("verdict").equals("judging"))
//				.collect(Collectors.toList());
//		submissionsQueue.addAll(submissions.stream().filter(s -> s.get("verdict").equals("system error")).collect(Collectors.toList()));
//		
//		Long submissionsCE = submissions.stream().filter(x -> x.get("verdict").equals("CE")).count();
//		Long submissionsEvaluating = submissions.stream().filter(x -> x.get("verdict").equals("judging")).count();
//		Long submissionsWaiting = submissions.stream().filter(x -> x.get("verdict").equals("waiting")).count();
//		Long submissionsErrors = submissions.stream().filter(x -> x.get("verdict").equals("system error")).count();
//		Long submissionsScored = submissions.size() - submissionsCE - submissionsEvaluating - submissionsWaiting - submissionsErrors;
//		model.addAttribute("workers", workers);
//		model.addAttribute("contests", contests);
//		model.addAttribute("submissions", submissions);
//		model.addAttribute("queue", submissionsQueue);
//		model.addAttribute("submissionsCE", submissionsCE);
//		model.addAttribute("submissionsEvaluating", submissionsEvaluating);
//		model.addAttribute("submissionsWaiting", submissionsWaiting);
//		model.addAttribute("submissionsErrors", submissionsErrors);
//		model.addAttribute("submissionsScored", submissionsScored);
//		
//		int contestId = getCurrentUserContestId();
//		addContestProblemsToModel(model, contestId);
//		
//    	return "user/dashboard";
//    }

//	@GetMapping("/user")
//    public String userDashboard(Model model) {
//		List<Map<String,Object>> workers = repository.listWorkers();
//		List<Map<String,Object>> contests = repository.listContests();
//		List<Map<String,Object>> submissions = repository.listDetailedSubmissions();
//		List<Map<String,Object>> submissionsQueue = submissions.stream()
//				.filter(s -> s.get("verdict").equals("waiting") 
//						|| s.get("verdict").equals("judging"))
//				.collect(Collectors.toList());
//		submissionsQueue.addAll(submissions.stream().filter(s -> s.get("verdict").equals("system error")).collect(Collectors.toList()));
//		
//		Long submissionsCE = submissions.stream().filter(x -> x.get("verdict").equals("CE")).count();
//		Long submissionsEvaluating = submissions.stream().filter(x -> x.get("verdict").equals("judging")).count();
//		Long submissionsWaiting = submissions.stream().filter(x -> x.get("verdict").equals("waiting")).count();
//		Long submissionsErrors = submissions.stream().filter(x -> x.get("verdict").equals("system error")).count();
//		Long submissionsScored = submissions.size() - submissionsCE - submissionsEvaluating - submissionsWaiting - submissionsErrors;
//		model.addAttribute("workers", workers);
//		model.addAttribute("contests", contests);
//		model.addAttribute("submissions", submissions);
//		model.addAttribute("queue", submissionsQueue);
//		model.addAttribute("submissionsCE", submissionsCE);
//		model.addAttribute("submissionsEvaluating", submissionsEvaluating);
//		model.addAttribute("submissionsWaiting", submissionsWaiting);
//		model.addAttribute("submissionsErrors", submissionsErrors);
//		model.addAttribute("submissionsScored", submissionsScored);
//		
//		int contestId = getCurrentUserContestId();
//		addContestProblemsToModel(model, contestId);
//		
//    	return "user/dashboard";
//    }
	
//	@PostMapping("/user/send-message")
//    public String userSendMessage(@RequestParam("message") String message) {
//		
//    	return "redirect:/user/communication";
//    }

}
