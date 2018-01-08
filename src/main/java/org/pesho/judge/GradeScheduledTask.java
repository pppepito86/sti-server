package org.pesho.judge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GradeScheduledTask {

    @Value("${work.dir}")
    private String workDir;
	
	@Autowired
	private Repository repository;
	
	@Autowired
	private WorkersQueue queue;
	
    @Scheduled(fixedDelay = 1000)
	public void gradeTask() throws IOException {
    	List<Map<String, Object>> submissions = repository.submissionsToGrade();
    	for (Map<String, Object> submission: submissions) {
    		boolean graded = grade(submission);
    		if (!graded) return;
    	}
    }

	private boolean grade(Map<String, Object> submission) throws IOException {
		String group = (String) submission.get("group");
		String directory = (String) submission.get("directory");
		
		List<Map<String, Object>> groupProblems = repository.getGroupProblems(group);
		for (Map<String, Object> problem: groupProblems) {
			int problemNumber = (int) problem.get("number");
			if (!"waiting".equals(submission.get("problem"+problemNumber))) continue;

			File[] allFiles = new File(directory).listFiles();
			if (allFiles == null) allFiles = new File[0];

			Optional<File> cFile = Arrays.stream(allFiles)
					.filter(f -> f.getName().equalsIgnoreCase(problem.get("name") + ".c"))
					.findFirst();
			if (cFile.isPresent()) {
				File oldFile = cFile.get();
				File newFile = new File(cFile.get().getAbsolutePath()+"pp");
				oldFile.renameTo(newFile);
			}

			allFiles = new File(directory).listFiles();
			if (allFiles == null) allFiles = new File[0];
			
			Optional<File> file = Arrays.stream(allFiles)
				.filter(f -> f.getName().equalsIgnoreCase(problem.get("name") + ".cpp"))
				.findFirst();
			
			int submissionId = (int) submission.get("id");
			
			if (!file.isPresent() || file.get().isDirectory()) {
				repository.addScore(submissionId, problemNumber, "not solved", 0);
				continue;
			}

			String fileName = file.get().getName();
			if (!fileName.endsWith(".cpp")) {
				fileName = fileName.substring(0, fileName.length()-4) + ".cpp";
				File newFile = new File(file.get().getParentFile(), fileName);
				file.get().renameTo(newFile);
				file = Optional.of(newFile);
			}
			
			Optional<Worker> worker = queue.take();
			if (!worker.isPresent()) return false;

			System.out.println("Starting " + worker.get().getUrl() + " for " + submission.get("id") + ", " + problem.get("id"));
			repository.addStatus(submissionId, problemNumber, "judging");
			
			long sTime = System.currentTimeMillis();

			worker.get().setFree(false);
			File fileToJudge = file.get();
			Runnable runnable = () -> {
				String result = "";
				int points = 0;
				try {
					SubmissionScore score = worker.get().grade(problem, submission, fileToJudge, workDir);
					points = (int) Math.round(score.getScore());
					StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
					if (values.length > 1) {
						for (int i = 1; i < values.length; i++) {
							StepResult step = values[i];
							if (i != 1)
								result += ",";
							result += step.getVerdict();
						}
					} else {
						result = values[0].getVerdict().toString();
						System.out.println("Submission <" + submissionId + "> failed with " + values[0].getReason());
					}
				} catch (Exception e) {
					System.out.println("Failed judging for submission: " + submissionId);
					e.printStackTrace();
					result = "waiting";
				} finally {
					System.out.println("Finishing " + worker.get().getUrl() + " " + submission.get("id") + ", " + problem.get("id") + " time - " + (System.currentTimeMillis() - sTime)/1000);
					worker.get().setFree(true);
					System.out.println("Judging " + worker.get().getUrl() + " " + result + " " + submissionId + " " + problemNumber);
					System.out.println("Scoring " + worker.get().getUrl() + " " + submissionId + " " + problemNumber);
					repository.addScore(submissionId, problemNumber, result, points);
				}
			};
			
			new Thread(runnable).start();
		}
		return true;
	}
}
