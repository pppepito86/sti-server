package org.pesho.judge;

import java.io.File;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class Worker {

	private RestTemplate restTemplate = new RestTemplate();
	private RestTemplate restTemplateIsAlive = new RestTemplate();
	
	private String url;
	private boolean isFree = true;

	public Worker(String url) {
		if (!url.startsWith("http"))
			url = "http://" + url;
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		this.url = url;

		restTemplateIsAlive.setRequestFactory(new SimpleClientHttpRequestFactory());
        SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplateIsAlive.getRequestFactory();
        rf.setReadTimeout(1000);
        rf.setConnectTimeout(1000);
	}

	public SubmissionScore grade(Map<String, Object> problem, Map<String, Object> submission, File file)
			throws Exception {
		int problemId = (int) problem.get("id");

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();

		HttpHeaders metadataHeader = new HttpHeaders();
		metadataHeader.setContentType(MediaType.APPLICATION_JSON);
		parameters.add("metadata", new HttpEntity<>("{\"problemId\":" + problemId + "}", metadataHeader));

		HttpHeaders fileHeader = new HttpHeaders();
		fileHeader.setContentType(MediaType.TEXT_PLAIN);
		fileHeader.add(HttpHeaders.CONTENT_DISPOSITION,
				String.format("form-data; name=\"file\"; filename=\"%s\"", file.getName()));
		parameters.add("file", new HttpEntity<>(FileUtils.readFileToByteArray(file), fileHeader));
		String submissionId = submission.get("id") + "_" + problem.get("name") + "_" + new Random().nextInt(100);

		HttpHeaders multipartHeaders = new HttpHeaders();
		multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity<SubmissionScore> response = restTemplate.postForEntity(url + "/api/v1/submissions/" + submissionId,
				new HttpEntity<>(parameters, multipartHeaders), SubmissionScore.class);
		System.out.println("Response for " + submissionId + " is: " + response.getStatusCode() + ", points are: " + response.getBody().getScore());
		return response.getBody();
	}
	
	public boolean isAlive() {
		try {
			ResponseEntity<String> entity = restTemplateIsAlive.getForEntity(url + "/api/v1/health-check", String.class);
			return entity.getStatusCode() == HttpStatus.OK;
		} catch (Exception e) {
			System.out.println("Cannot connect to worker " + url);
			return false;
		}
	}

	public void setFree(boolean isFree) {
		this.isFree = isFree;
	}

	public boolean isFree() {
		return isFree;
	}

	public String getUrl() {
		return url;
	}

}
