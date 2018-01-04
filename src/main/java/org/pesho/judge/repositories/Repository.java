package org.pesho.judge.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class Repository {

    @Autowired
    private JdbcTemplate template;

	public synchronized int addProblem(String name, String group, int number, int points, String file) {
        template.update("INSERT INTO problems(name, `group`, number, points, file) VALUES(?, ?, ?, ?, ?)",
        		name,
        		group,
                number,
                points,
                file);
        
		Optional<Object> first = template.queryForList("SELECT MAX(id) FROM problems").stream()
				.map(x -> x.get("MAX(id)")).findFirst();
		
		return (int) first.get();
	}

	public void updateProblem(int id, String name, int points, String file) {
        template.update("UPDATE problems set name=?, points=?, file=? where id=?",
        		name, points, file, id);
	}
	
	public Optional<Map<String, Object>> getProblem(String group, int number) {
		return template.queryForList(
				"select * from problems where `group`=? AND number=?", 
				group, number).stream().findFirst();
	}
	
	public List<Map<String, Object>> getGroupProblems(String group) {
		return template.queryForList(
				"select * from problems where `group`=?", group);
	}

	
	public List<Map<String, Object>> listProblems() {
        return template.queryForList("SELECT * from problems");
	}

	
	public boolean hasCitySubmissions(String city) {
		return template.queryForList(
				"select * from submissions where city=?", city).stream().findAny().isPresent();
	}
	
	public void addSubmission(String city, String group, String directory) {
        template.update("INSERT INTO submissions(city, `group`, directory, problem1, problem2, problem3) VALUES(?, ?, ?, ?, ?, ?)",
                city, group, directory, "waiting", "waiting", "waiting");
	}
	
	public List<Map<String, Object>> listGroupSubmissions(String city, String group) {
        List<Map<String, Object>> submissions = template.queryForList("SELECT id, problem1, problem2, problem3, points1, points2, points3, points1+points2+points3 as points, directory from submissions where city=? AND `group`=?",
                city, group);
        for (Map<String, Object> submission: submissions) {
        	String dir = (String) submission.get("directory");
        	int last = Math.max(dir.lastIndexOf("\\"), dir.lastIndexOf("/"));
        	if (last < 0) continue;
        	dir = dir.substring(last+1);
        	submission.put("directory", dir);
        }
        return submissions;
	}

	public List<Map<String, Object>> submissionsToGrade() {
		return template.queryForList(
				"select * from submissions where problem1='waiting' OR problem2='waiting' OR problem3='waiting'");
	}

	public synchronized void addStatus(int id, int number, String result) {
		String queryTemplate = "UPDATE submissions SET %s=? WHERE id=?";
		String query = String.format(queryTemplate, "problem" + number);
		template.update(query, result, id);
	}
	
	public synchronized void addScore(int id, int number, String result, int points) {
		String queryTemplate = "UPDATE submissions SET %s=?, %s=? WHERE id=?";
		String query = String.format(queryTemplate, "problem" + number, "points" + number);
		template.update(query, result, points, id);
	}
	
	public void addWorker(String url) {
        template.update("INSERT INTO workers(url) VALUES(?)", url);
	}
	
	public void deleteWorker(String url) {
        template.update("DELETE from workers where url=?", url);
	}

	public List<Map<String, Object>> listWorkers() {
		return template.queryForList("select * from workers");
	}
	
}
