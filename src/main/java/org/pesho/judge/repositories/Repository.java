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

	public List<Map<String, Object>> getGroupProblems(String group) {
		return template.queryForList(
				"select * from problems where `group`=?", group);
	}
	
	public void addSubmissions(String city, String group, String directory) {
        template.update("INSERT INTO submissions(city, `group`, directory) VALUES(?, ?, ?)",
                city,
                group,
                directory);
	}

	public List<Map<String, Object>> submissionsToGrade() {
		return template.queryForList(
				"select * from submissions where problem1 is NULL OR problem2 is NULL OR problem3 is NULL");
	}

	public synchronized void addScore(int id, int number, String result, int points) {
		String queryTemplate = "UPDATE submissions SET %s=?, %s=? WHERE id=?";
		String query = String.format(queryTemplate, "problem" + number, "points" + number);
		template.update(query, result, points, id);
		Map<String,Object> submission = template.queryForList("SELECT points1, points2, points3 from submissions where id=?", id).stream().findFirst().get();
		int totalPoints = submission.values().stream().filter(s -> s != null).mapToInt(s -> (int) s).sum();
		template.update("UPDATE submissions SET points=? WHERE id=?", totalPoints, id);
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
