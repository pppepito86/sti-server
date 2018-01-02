package org.pesho.judge.rest;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.print.DocFlavor.URL;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.pesho.judge.Worker;
import org.pesho.judge.WorkersQueue;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("")
public class RestService {

	@Value("${work.dir}")
	private String workDir;

	@Autowired
	private Repository repository;
	
	@Autowired
	private WorkersQueue workersQueue;

	@PostMapping("/problem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public ResponseEntity<?> addProblem(@RequestPart("file") MultipartFile file, @RequestParam("number") Integer number,
			@RequestParam("name") String name, @RequestParam("group") String group,
			@RequestParam("points") Integer points) throws Exception {
		group = group.toUpperCase();

		File zipFile = getFile("problem", group, String.valueOf(number), file.getOriginalFilename().toLowerCase());
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = new File(zipFile.getAbsolutePath().replace(".zip", ""));
		unzip(zipFile, zipFolder);

		int problemId = repository.addProblem(name, group, number, points, zipFile.getName());

		workersQueue.getAll().stream()
			.parallel()
			.forEach(worker -> {
				String endpointURL = worker.getUrl() + "/api/v1/problems/" + problemId;
				sendProblemToWorker(endpointURL, zipFile.getAbsolutePath());
			});

		try {
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
	}

	private void sendProblemToWorker(String endpointURL, String zipFilePath) {
		
		RestTemplate rest = new RestTemplate();
		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.add("file", new FileSystemResource(zipFilePath));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "multipart/form-data");
		
		boolean exists;
		try {
			exists = HttpStatus.OK == rest.getForEntity(endpointURL, String.class).getStatusCode();
		} catch (HttpClientErrorException e) {
			exists = false;
		}
		
		HttpEntity<MultiValueMap<String, Object>> params = new HttpEntity<MultiValueMap<String, Object>>(parameters,
				headers);
		if (exists) {
			rest.put(endpointURL, params);
		} else {
			rest.postForLocation(endpointURL, params);
		}
	}
	
//	private void sendProblemToWorker() {
//		
//	}

	@PostMapping("/grade")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public ResponseEntity<?> addSubmission(@RequestPart("file") MultipartFile file, @RequestParam("city") String city,
			@RequestParam("group") String group) throws Exception {
		group = group.toUpperCase();

		File zipFile = getFile("submission", city, group, file.getOriginalFilename().toLowerCase());
		zipFile.getParentFile().mkdirs();
		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
		File zipFolder = new File(zipFile.getAbsolutePath().replace(".zip", ""));
		unzip(zipFile, zipFolder);

		for (File dir : zipFolder.listFiles()) {
			if (!dir.isDirectory())
				continue;
			String dirName = dir.getCanonicalPath().replace(new File(workDir).getCanonicalPath(), "");
			if (dirName.startsWith(File.separator))
				dirName = dirName.substring(1);
			repository.addSubmissions(city, group, dirName);
		}

		try {
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
	}
	
	@GetMapping("/workers")
	public List<Map<String, Object>> getWorkers() {
		return repository.listWorkers();
	}
	
	@PostMapping("/workers/{url}")
	public ResponseEntity<?> addWorker(@PathVariable("url") String url) {
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

	private File getFile(String type, String city, String group, String fileName) {
		String path = new File(workDir).getAbsolutePath() + File.separator + type + File.separator + city
				+ File.separator + group + File.separator + fileName;
		return new File(path);
	}

	public void unzip(File file, File folder) {
		try {
			folder.mkdirs();

			try (ZipFile zipFile = new ZipFile(file)) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					File test = new File(folder, entry.getName());
					if (entry.isDirectory()) {
						test.mkdirs();
					} else {
						FileUtils.copyInputStreamToFile(zipFile.getInputStream(entry), test);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}