package org.pesho.judge;

import java.io.File;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.pesho.grader.SubmissionScore;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

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

		String submissionId = submission.get("id") + "_" + problem.get("name") + "_" + new Random().nextInt(100);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.TEXT_PLAIN, file.getName())
                .addTextBody("metadata", "{\"problemId\":" + problemId + "}", ContentType.APPLICATION_JSON)
                .build();
		
		HttpPost post = new HttpPost(url + "/api/v1/submissions/" + submissionId);
		post.setEntity(entity);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = httpclient.execute(post);
		String responseString = EntityUtils.toString(response.getEntity());
		httpclient.close();
		ObjectMapper mapper = new ObjectMapper();
		SubmissionScore score = mapper.readValue(responseString, SubmissionScore.class);
		
		System.out.println("Response for " + submissionId + " is: " + response.getStatusLine() + ", points are: " + score.getScore());
		return score;
	}
	
	public boolean isAlive() {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(url + "/api/v1/health-check");
			CloseableHttpResponse response = httpclient.execute(httpGet);
			return response.getStatusLine().getStatusCode() == HttpStatus.OK.value();
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
