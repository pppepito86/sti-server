package org.pesho.judge;

import java.util.LinkedHashSet;
import java.util.Optional;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkersQueue {

	private LinkedHashSet<Worker> workers = new LinkedHashSet<>();
	
	public WorkersQueue() {
		put(new Worker("http://35.158.88.137:8089"));
	}
	
	public synchronized void put(Worker worker) {
		workers.add(worker);
	}
	
	public synchronized Optional<Worker> take() {
		return workers.stream().filter(w -> w.isFree()).findFirst();
	}
	
}
