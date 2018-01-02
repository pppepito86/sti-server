package org.pesho.judge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
	
    @Scheduled(fixedDelay = 1000)
	public void gradeTask() throws IOException {
    	List<Map<String, Object>> submissions = repository.submissionsToGrade();
    	for (Map<String, Object> submission: submissions) {
    		grade(submission);
    	}
    }

	private void grade(Map<String, Object> submission) throws IOException {
//		Optional<Worker> worker = queue.take();
//		if (!worker.isPresent()) return;
		
		String group = (String) submission.get("group");
		String directory = (String) submission.get("directory");
		
		List<Map<String, Object>> groupProblems = repository.getGroupProblems(group);
		for (Map<String, Object> problem: groupProblems) {
			Optional<File> file = Arrays.stream(new File(directory).listFiles())
				.filter(f -> f.getName().equalsIgnoreCase(problem.get("name") + ".cpp"))
				.findFirst();
			
			if (!file.isPresent() || file.get().isDirectory()) {
				repository.addScore((int) submission.get("id"), (int) problem.get("number"), "not solved");
				continue;
			}
			
			int problemNumber = (int) problem.get("number");
			if (submission.get("problem"+problemNumber) != null) continue;
			
			//Worker worker = new Worker("http://35.158.88.137:8089");
//			worker.get().setFree(false);
//			Runnable runnable = () -> {
//				String result = worker.get().grade(problem, submission, file.get());
//				repository.addScore((int) submission.get("id"), problemNumber, result);
//				worker.get().setFree(true);
//			};
//			new Thread(runnable).start();
			Worker worker = new Worker("http://35.158.88.137:8089");
			String result = worker.grade(problem, submission, file.get());
			repository.addScore((int) submission.get("id"), problemNumber, result);
		}
		
	}
    
}
