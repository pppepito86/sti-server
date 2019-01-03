package org.pesho.judge.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.pesho.grader.step.Verdict;
import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TaskParser;
import org.pesho.judge.Worker;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.pesho.workermanager.RunTerminateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.zip.ZipUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HtmlService implements RunTerminateListener {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	protected WorkersQueue workersQueue;
	
	@Autowired
	protected Repository repository;
	
	protected ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void instanceCreated(String host) {
		addWorker(host, 8089);
//		addWorker(host, 8090);
	}
	
	private void addWorker(String host, int port) {
		String url = host + ":" + port;
		repository.addWorker(url, "automatic");
		workersQueue.put(new Worker(url));
	}
	
	@Override
	public void instanceTerminated(String host) {
		removeWorker(host, 8089);
//		removeWorker(url, 8090);
	}
	
	private void removeWorker(String host, int port) {
		String url = host + ":" + port;
		repository.deleteWorker(url);
		workersQueue.remove(url);
	}
	
	protected void addIsStarted(Model model, int contestId) {
		Map<String, Object> contest = repository.getContest(contestId).get();
		Timestamp startTime = (Timestamp) contest.get("start_time");
		boolean isStarted = System.currentTimeMillis() >= startTime.getTime();
		model.addAttribute("isStarted", isStarted);
	}

	protected void addTimeLeft(Model model, int contestId) {
		Map<String, Object> contest = repository.getContest(contestId).get();
		Timestamp endTime = (Timestamp) contest.get("end_time");
		long timeLeft = endTime.getTime() - System.currentTimeMillis();
		model.addAttribute("timeLeft", timeLeft);

		List<Map<String,Object>> submission = repository.getUserSubmissions(SecurityContextHolder.getContext().getAuthentication().getName());
		if (submission.isEmpty()) {
			model.addAttribute("timeToSubmit", 0);
		} else {
			Timestamp lastSubmissionTime = (Timestamp) submission.get(0).get("upload_time");
			long diff = lastSubmissionTime.getTime() + 60 * 1000 - System.currentTimeMillis();
			if (diff > 0) model.addAttribute("timeToSubmit", diff);
			else model.addAttribute("timeToSubmit", 0);
		}
	}
	
	protected void addContestProblemsToModel(Model model, int contestId) {
		List<Map<String, Object>> contestProblems = 
				repository.listContestProblems(contestId);
		model.addAttribute("contestProblems", contestProblems);
		model.addAttribute("contestName", getCurrentUserContest());
		model.addAttribute("problemNumber", 0);
		
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		model.addAttribute("displayName", repository.getUserDisplayName(name).orElse(""));
		
		addTimeLeft(model, contestId);
	}
	
	public String addSubmission(MultipartFile file, String code, Integer problemNumber) throws IOException {
		long submissionTime = System.currentTimeMillis();
		
		String city = "Sofia";
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		String contest = getCurrentUserContest();
		int contestId = getCurrentUserContestId();
		String problemName = repository
				.getProblem(contestId, problemNumber).get().get("name").toString();
		
		String fileName = problemName + ".cpp";
		
		List<Map<String,Object>> submission = repository.getUserSubmissions(SecurityContextHolder.getContext().getAuthentication().getName());
		if (!submission.isEmpty()) {
			Timestamp lastSubmissionTime = (Timestamp) submission.get(0).get("upload_time");
			if (lastSubmissionTime.getTime() + 60 * 1000 > submissionTime) {
				return "redirect:/user/error?msg=1";
			}
		}
		if (submission.size() >= 50) {
			return "redirect:/user/error?msg=3";
		}
		
		Map<String, Object> contestMap = repository.getContest(contestId).get();
		Timestamp endTime = (Timestamp) contestMap.get("end_time");
		if (submissionTime > endTime.getTime()) {
			return "redirect:/user/error?msg=\"contest is over\"";
		}
		
		int submissionId = repository.addSubmission(city, username, contest, problemName, fileName);
		if (submissionId != 0) {
			File sourceFile = getFile("submissions", String.valueOf(submissionId), fileName);
			if (file != null) {
				FileUtils.copyInputStreamToFile(file.getInputStream(), sourceFile);
			} else {
				FileUtils.writeStringToFile(sourceFile, code);
			}
			int submissionNumber = repository.userSubmissionsNumberForProblem(username, problemNumber, submissionId);
			return "redirect:/user/problem/" + problemNumber + "/submissions/"+submissionNumber;
		} else {
			String details = String.format("%s_%s_%s_%s", city, contest, username, problemName);
			repository.addLog("submission", "problem not found for " + details, "");
			return "redirect:/user/problem/"+problemNumber;
		}
		
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
	
	public String showUserSubmissionPage(int id, Model model) throws Exception {
		List<Map<String,Object>> contests = repository.listContests();
		model.addAttribute("contests", contests);
		Optional<Map<String,Object>> submission = repository.getSubmission(id);
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		if (!submission.isPresent() || !username.equals(submission.get().get("username"))) {
			return "redirect:/user/error?msg=2";
		}
		
		if (submission.isPresent()) {
			TreeSet<Integer> feedback = feedback(Integer.valueOf(submission.get().get("problem_id").toString()));
			submission.get().put("verdict", fixVerdict(submission.get().get("verdict").toString(), feedback));
			
			String details = submission.get().get("details").toString();
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
					model.addAttribute("score", Math.round(score.getScore()));
				} else {
					model.addAttribute("score", "?");
				}
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
		
		int contestId = getCurrentUserContestId();
		addContestProblemsToModel(model, contestId);
		return "user/submission";
	}
	
	protected Map<Integer, Map<String, Object>> fixSubmissions(List<Map<String, Object>> userSubmissions, int problemsCount, boolean addDetails) {
		Map<Integer, Map<String, Object>> map = new TreeMap<>();
		for (int i = 1; i <= problemsCount; i++) {
			HashMap<String, Object> emptyMap = new HashMap<>();
			emptyMap.put("points", 0);
			emptyMap.put("verdict", "not solved");
			map.put(i, emptyMap);
		}
		for (Map<String, Object> submission: userSubmissions) {
			int number = (int) submission.get("number");
			Integer currentScore = (Integer) map.get(number).get("points");
			if (currentScore == null) currentScore = 0;
			Integer submissionPoints = (Integer) submission.get("points");
			if (submissionPoints == null) submissionPoints = 0;
			if (submissionPoints >= currentScore) {
				if (addDetails) {
					String newDetails = "";
					String details = repository.getSubmission((int) submission.get("id")).get().get("details").toString();
					if (details != null && !details.isEmpty()) {
						try {
							SubmissionScore score = mapper.readValue(details, SubmissionScore.class);
							if (score.getScoreSteps().size() == 1) {
								newDetails = "C";
							} else {
								for (Map.Entry<String, StepResult> entry: score.getScoreSteps().entrySet()) {
									if (entry.getKey().equals("Compile")) continue;
									Verdict verdict = entry.getValue().getVerdict();
									if (verdict == Verdict.OK) newDetails += "+";
									if (verdict == Verdict.ML) newDetails += "M";
									if (verdict == Verdict.TL) newDetails += "T";
									if (verdict == Verdict.RE) newDetails += "R";
									if (verdict == Verdict.WA) newDetails += "W";
								}
							}
						} catch (Exception e) {
						}
					}
					submission.put("details", newDetails);
				}
				map.put(number, submission);
			}
		}
		
		//map.put("total", total);
		return map;
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

	protected List<File> listSourceFiles(File zipFolder) {
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
	
	public String getChecksum(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			return DigestUtils.md5Hex(fis);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public void unzip(File file, File folder) {
		try {
			new ProcessExecutor().command("unzip", file.getCanonicalPath(), "-d", folder.getCanonicalPath()).execute();
		} catch (Exception e) {
			try {
				ZipUtil.unpack(file, folder);
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new IllegalStateException(e);
			}
		}
	}
	
	protected int getCurrentUserContestId() {
		String contest = getCurrentUserContest();
	    int contestId = (int) repository.getContest(contest).get().get("id");
		return contestId;
	}

	private String getCurrentUserContest() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
	    String contest = repository.getUserContest(username).get();
		return contest;
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
	
}
