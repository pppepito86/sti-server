package org.pesho.judge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	
    @Scheduled(fixedDelay = 5000)
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
			Optional<Worker> worker = queue.take();
			if (!worker.isPresent()) return;
			
			Optional<File> file = Arrays.stream(new File(directory).listFiles())
				.filter(f -> f.getName().equalsIgnoreCase(problem.get("name") + ".cpp"))
				.findFirst();
			
			int submissionId = (int) submission.get("id");
			int problemNumber = (int) problem.get("number");
			
			if (!file.isPresent() || file.get().isDirectory()) {
				repository.addScore(submissionId, problemNumber, "not solved");
				continue;
			}
			
			if (submission.get("problem"+problemNumber) != null) continue;
			repository.addScore(submissionId, problemNumber, "judging");
			
			worker.get().setFree(false);
			Runnable runnable = () -> {
				String result = worker.get().grade(problem, submission, file.get());
				System.out.println("Judging " + worker.get().getUrl() + " " + result + " " + submissionId + " " + problemNumber);
				repository.addScore(submissionId, problemNumber, result);
				worker.get().setFree(true);
			};
			
			new Thread(runnable).start();
		}	
	}
}
