package org.pesho.judge.repositories;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class Repository {

    @Autowired
    private JdbcTemplate template;

    public synchronized int addContest(String name, Timestamp start, Timestamp end) {
    	template.update("INSERT INTO contests(name, start_time, end_time) VALUES(?, ?, ?)", 
    			name, start, end);
    	
    	Optional<Object> last = template.queryForList("SELECT MAX(id) FROM contests").stream()
    			.map(x -> x.get("MAX(id)")).findFirst();
    	
    	return (int) last.get();
    }

    public synchronized int addProblem(String name, int contestId, int number, String file, String checksum) {
        template.update("INSERT INTO problems(name, number, contest_id, file, checksum) VALUES(?, ?, ?, ?, ?)",
        		name,
        		number,
        		contestId,
                file,
                checksum);
        
		Optional<Object> first = template.queryForList("SELECT MAX(id) FROM problems").stream()
				.map(x -> x.get("MAX(id)")).findFirst();
		
		return (int) first.get();
	}

	public void updateProblem(int id, String name, String file, String checksum) {
        template.update("UPDATE problems set name=?, file=?, checksum=? where id=?",
        		name, file, checksum, id);
	}
	
	public Optional<Map<String, Object>> getProblem(int id) {
		return template.queryForList(
				"select * from problems where id=?", 
				id).stream().findFirst();
	}
	
	public Optional<Map<String, Object>> getProblem(int contestId, int number) {
		return template.queryForList(
				"select * from problems where contest_id=? AND number=?", 
				contestId, number).stream().findFirst();
	}

	public Optional<Map<String, Object>> getProblem(String contestName, String problemName) {
		return template.queryForList(
				"select problems.id, problems.name from problems" +
				" inner join contests on contests.name=? and contests.id=problems.contest_id" +
				" where problems.name=?", 
				contestName, problemName).stream().findFirst();
	}
	
	public List<Map<String, Object>> getContestProblems(String contest) {
		return template.queryForList(
				"select * from problems where contest=?", contest);
	}
	
	public List<Map<String, Object>> listProblems() {
        return template.queryForList("SELECT * from problems");
	}
	
	public List<Map<String, Object>> listContestProblems(int contestId) {
		return template.queryForList("SELECT * from problems where contest_id=? order by number", contestId);
	}
	
	public List<Map<String, Object>> listContests() {
		return template.queryForList("SELECT * from contests");
	}
	
	public Optional<Map<String, Object>> getContest(int id) {
		return template.queryForList("SELECT * from contests where id=?", id).stream().findFirst();
	}

	public Optional<Map<String, Object>> getContest(String contest) {
		return template.queryForList("SELECT * from contests where name=?", contest).stream().findFirst();
	}
	
	public List<Map<String, Object>> listSubmissions() {
		return template.queryForList("SELECT * from submissions");
	}
	
	public List<Map<String, Object>> listCitySubmissions(String city) {
		return template.queryForList("SELECT * from submissions where city=?", city);
	}
	
	public List<Map<String, Object>> listUserSubmissionsForProblem(String username, int problemNumber) {
		return template.queryForList("SELECT submissions.id,submissions.verdict,submissions.points,submissions.upload_time from submissions" +
				" inner join users on users.name=? and users.name=submissions.username" + 
				" inner join contests on contests.name=users.contest" + 
				" inner join problems on problems.number=? and problems.contest_id=contests.id and problems.id=submissions.problem_id" +
				" order by submissions.upload_time desc",
				username, problemNumber);
	}
	
	public int userSubmissionsNumberForProblem(String username, int problemNumber, int submissionId) {
		return template.queryForList("SELECT submissions.id from submissions" +
				" inner join users on users.name=? and users.name=submissions.username" + 
				" inner join contests on contests.name=users.contest" + 
				" inner join problems on problems.number=? and problems.contest_id=contests.id and problems.id=submissions.problem_id" +
				" where submissions.id <= ?" +
				" order by submissions.upload_time desc",
				username, problemNumber, submissionId).size();
	}
	
	public synchronized int addSubmission(String city, String username, String contest, String problemName, String file) {
		Optional<Map<String,Object>> problem = getProblem(contest, problemName);
		if (problem.isPresent()) {
			template.update("INSERT INTO submissions(city, username, file, verdict, details, problem_id) VALUES(?, ?, ?, ?, ?, ?)",
                city, username, file, "waiting", "", (int) problem.get().get("id"));
        
			Optional<Object> first = template.queryForList("SELECT MAX(id) FROM submissions").stream()
				.map(x -> x.get("MAX(id)")).findFirst();
			return (int) first.get();
		}
		return 0;
	}
	
	public boolean hasCitySubmissions(String city) {
		return template.queryForList("SELECT id from submissions where city=?", city).stream().findAny().isPresent();
	}
	
	public List<Map<String, Object>> listContestSubmissions(String city, String contest) {
        return template.queryForList("SELECT * from submissions where city=? AND contest=?",
                city, contest);
	}
	
	public List<Map<String, Object>> listDetailedSubmissions() {
		return template.queryForList("SELECT submissions.id, city, username, submissions.file, verdict, points, contests.name as contest_name, problems.name as problem_name, problems.number from submissions" + 
				" inner join problems on submissions.problem_id=problems.id" +
				" inner join contests on problems.contest_id=contests.id");
	}

	public List<Map<String, Object>> listContestSubmissions(int contestId) {
		return template.queryForList("SELECT submissions.id, city, username, verdict, points, problems.name from submissions" + 
				" inner join problems on problems.contest_id=? AND submissions.problem_id=problems.id",
			contestId);
	}

	public List<Map<String, Object>> submissionsToGrade() {
		return template.queryForList(
				"select * from submissions where verdict='waiting'");
	}
	
	public List<Map<String, Object>> failedSubmissions() {
		return template.queryForList(
				"select * from submissions where verdict='system error'");
	}

	public synchronized void addStatus(int id, String verdict) {
		template.update("UPDATE submissions SET verdict=? WHERE id=?", verdict, id);
	}
	
	public synchronized void addScore(int id, String result, String details, int points) {
		String escapedDetails = StringEscapeUtils.escapeSql(details);
		template.update("UPDATE submissions SET verdict=?, details=?, points=? WHERE id=?", result, escapedDetails, points, id);
	}
	
	public synchronized void addWorker(String url, String type) {
		if (findWorkers(url).isPresent()) {
			template.update("UPDATE workers SET type=?, deleted=false where url=?", type, url);
		} else {
			template.update("INSERT INTO workers(url, type) VALUES(?, ?)", url, type);
		}
	}
	
	public void deleteWorker(String url) {
        template.update("UPDATE workers SET deleted=true where url=?", url);
	}

	public Optional<Map<String, Object>> findWorkers(String url) {
		return template.queryForList("select * from workers where url=?", url).stream().findFirst();
	}
	
	public List<Map<String, Object>> listWorkers() {
		return template.queryForList("select * from workers where deleted=false");
	}
	
	public List<Map<String, Object>> listActiveWorkers() {
		return template.queryForList("select * from workers where active=true AND deleted=false");
	}
	
	public int maxProblemNumber() {
		return template.queryForList("select max(number) from problems").stream().findFirst().map(x -> (int) x.get("max(number)")).orElse(0);
	}

	public Optional<Map<String, Object>> getSubmission(int id) {
		return template.queryForList("SELECT submissions.id, city, username, submissions.file, problem_id, verdict, details, points, submissions.upload_time, contests.name as contest_name, problems.name as problem_name from submissions" +
				" inner join problems on submissions.problem_id=problems.id" +
				" inner join contests on problems.contest_id=contests.id" +
				"  where submissions.id=?", id).stream().findFirst();
	}
	
    public synchronized void addLog(String topic, String title, String message) {
    	template.update("INSERT INTO logs(topic, title, message) VALUES(?, ?, ?)", 
    			topic, title, message);
    }
    
    public List<Map<String, Object>> listLogs() {
		return template.queryForList("select * from logs");
	}
    
    public List<Map<String, Object>> listUsers() {
		return template.queryForList("select * from users");
	}
	
    public synchronized int addUser(String username, 
    		String password, String displayName, String group, 
    		String school, String grade) {
    	
    	template.update("INSERT INTO users(name, display_name, password, contest, role, school, grade) "
    			+ "VALUES(?, ?, ?, ?, ?, ?, ?)", 
    			username, displayName, password, group, "USER", school, grade);
    	
    	Optional<Object> last = template.queryForList("SELECT MAX(id) FROM users").stream()
    			.map(x -> x.get("MAX(id)")).findFirst();
    	
    	return (int) last.get();
	}
    
    public Optional<String> getUserContest(String username) {
    	return template.queryForList("select contest from users where name=?", username).stream().map(x -> x.get("contest").toString()).findFirst();
    }

    public Optional<String> getUserDisplayName(String username) {
    	return template.queryForList("select display_name from users where name=?", username).stream().map(x -> x.get("display_name").toString()).findFirst();
    }
    
    public List<Map<String, Object>> getUserSubmissions(String name) {
    	return template.queryForList("SELECT upload_time from submissions" +
				"  where username=? order by upload_time desc", name);
    }

	public void updateContest(int id, Timestamp start, Timestamp end) {
		template.update("UPDATE contests SET start_time=?, end_time=? where id=?", 
				start, end, id);
	}
    
}
