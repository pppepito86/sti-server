package org.pesho.judge.rest;

import java.util.List;
import java.util.Map;

import org.pesho.judge.Worker;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class RestService {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	private Repository repository;

	@Autowired
	private WorkersQueue workersQueue;

	@GetMapping("/workers")
	public List<Map<String, Object>> getWorkers() {
		return repository.listWorkers();
	}

	@PostMapping("/workers/{url}")
	public ResponseEntity<?> addWorker(@PathVariable("url") String url) {
		boolean isDuplicate = repository.listWorkers()
				.stream()
				.anyMatch(worker -> worker.get("url").equals(url));
		
		if (isDuplicate) return new ResponseEntity<>(HttpStatus.CONFLICT); 
			
		workersQueue.put(new Worker(url));
		repository.addWorker(url);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/workers/{url}")
	public ResponseEntity<?> deleteWorker(@PathVariable("url") String url) {
		workersQueue.remove(url);
		repository.deleteWorker(url);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}