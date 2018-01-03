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
    		grade(submission);
    	}
    }

	private void grade(Map<String, Object> submission) throws IOException {
		String group = (String) submission.get("group");
		String directory = (String) submission.get("directory");
		
		List<Map<String, Object>> groupProblems = repository.getGroupProblems(group);
		for (Map<String, Object> problem: groupProblems) {
			Optional<File> file = Arrays.stream(new File(directory).listFiles())
				.filter(f -> f.getName().equalsIgnoreCase(problem.get("name") + ".cpp"))
				.findFirst();
			
			int submissionId = (int) submission.get("id");
			int problemNumber = (int) problem.get("number");
			
			if (!file.isPresent() || file.get().isDirectory()) {
				repository.addScore(submissionId, problemNumber, "not solved", 0);
				continue;
			}
			
			Optional<Worker> worker = queue.take();
			if (!worker.isPresent()) return;
			
			if (submission.get("problem"+problemNumber) != null) continue;
			repository.addScore(submissionId, problemNumber, "judging", 0);
			
			long sTime = System.currentTimeMillis();
			System.out.println("Starting " + worker.get().getUrl() + " for " + submission.get("id") + ", " + problem.get("id"));

			worker.get().setFree(false);
			Runnable runnable = () -> {
				String result = "";
				int points = 0;
				try {
					SubmissionScore score = worker.get().grade(problem, submission, file.get());
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
					e.printStackTrace();
					result = "system error";
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
	}
}
