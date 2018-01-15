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

	public synchronized int addProblem(String name, String contest, int number, String file) {
        template.update("INSERT INTO problems(name, number, contest, file) VALUES(?, ?, ?, ?)",
        		name,
        		number,
        		contest,
                file);
        
		Optional<Object> first = template.queryForList("SELECT MAX(id) FROM problems").stream()
				.map(x -> x.get("MAX(id)")).findFirst();
		
		return (int) first.get();
	}

	public void updateProblem(int id, String name, String file) {
        template.update("UPDATE problems set name=?, file=? where id=?",
        		name, file, id);
	}
	
	public Optional<Map<String, Object>> getProblem(int id) {
		return template.queryForList(
				"select * from problems where id=?", 
				id).stream().findFirst();
	}
	
	public Optional<Map<String, Object>> getProblem(String contest, int number) {
		return template.queryForList(
				"select * from problems where contest=? AND number=?", 
				contest, number).stream().findFirst();
	}
	
	public List<Map<String, Object>> getContestProblems(String contest) {
		return template.queryForList(
				"select * from problems where contest=?", contest);
	}
	
	public List<Map<String, Object>> listProblems() {
        return template.queryForList("SELECT * from problems");
	}
	
	public List<Map<String, Object>> listSubmissions() {
		return template.queryForList("SELECT * from submissions");
	}
	
	public List<Map<String, Object>> listCitySubmissions(String city) {
		return template.queryForList("SELECT * from submissions where city=?", city);
	}
	
	public void addSubmission(String city, String username, String contest, String problemName, String file) {
        template.update("INSERT INTO submissions(city, username, contest, file, details, problem_id) " +
        		"SELECT ?, ?, ?, ?, ?, id from problems where name=? AND contest=?",
                city, username, contest, file, "waiting", problemName, contest);		
	}
	
	public boolean hasCitySubmissions(String city) {
		return template.queryForList("SELECT id from submissions where city=?", city).stream().findAny().isPresent();
	}
	
	public List<Map<String, Object>> listContestSubmissions(String city, String contest) {
        return template.queryForList("SELECT * from submissions where city=? AND contest=?",
                city, contest);
	}

	public List<Map<String, Object>> submissionsToGrade() {
		return template.queryForList(
				"select * from submissions where details='waiting'");
	}

	public synchronized void addStatus(int id, String details) {
		template.update("UPDATE submissions SET details=? WHERE id=?", details, id);
	}
	
	public synchronized void addScore(int id, String result, int points) {
		template.update("UPDATE submissions SET details=?, points=? WHERE id=?", result, points, id);
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
