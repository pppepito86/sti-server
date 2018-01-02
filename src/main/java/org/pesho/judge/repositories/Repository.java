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

	public void addScore(int id, int number, String result) {
		template.update("UPDATE submissions SET problem" + number + "=? WHERE id=?",
                result,
                id);
	}
	
}
