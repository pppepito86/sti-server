package org.pesho.judge;

import java.io.File;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class Worker {

	private String url;
	private boolean isFree;

	public Worker(String url) {
		this.url = url;
		if (!url.startsWith("http"))
			url = "http://" + url;
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
	}

	public String grade(Map<String, Object> problem, Map<String, Object> submission, File file) {
		try {
			int problemId = (int) problem.get("id");

			RestTemplate rest = new RestTemplate();
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

			ResponseEntity<SubmissionScore> response = rest.postForEntity(url + "api/v1/submissions/" + submissionId,
					new HttpEntity<>(parameters, multipartHeaders), SubmissionScore.class);

			System.out.println(response.getStatusCode());
			SubmissionScore score = response.getBody();
			StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
			String result = "";
			if (values.length > 1) {
				for (int i = 1; i < values.length; i++) {
					StepResult step = values[i];
					if (i != 1)
						result += ",";
					result += step.getVerdict();
				}
			} else {
				result = values[0].getVerdict().toString();
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "system error";
		}
	}

	public void setFree(boolean isFree) {
		this.isFree = isFree;
	}
	
	public boolean isFree() {
		return isFree;
	}
	
}
