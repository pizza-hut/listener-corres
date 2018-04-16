package org.owlet.corres.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.owlet.corres.listener.model.CorresStatusResponse;
//import org.owlet.corres.listener.model.EventMessage;

//import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Listener {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired
	RestTemplate restTemplate;
		
	@Value("${url.api.acl.corres}")
	private String url_api_acl_corres;
	
	@Value("${kafka.topic.job.request}")
	private String kafka_topic_job_request;
	
	@Value("${kafka.topic.job.status}")
	private String kafka_topic_job_status;
	
	@KafkaListener(topics = "${kafka.topic.job.request}")
	public void listen(ConsumerRecord<?,?>consumerRecord) throws JSONException {
		
		logger.info("topic: {}", consumerRecord.topic().toString());
		logger.info("value: {}", consumerRecord.value().toString());
		logger.info("timestamp: {}", String.valueOf(consumerRecord.timestamp()));
		logger.info("partition: {}", consumerRecord.partition());
		
		JSONObject json = new JSONObject(consumerRecord.value().toString());
		
		//JSONObject jobj = new JSONObject("{\"name\":\"mkyong\",\"salary\":7500,\"}");
		logger.info(json.toString());
		logger.info("get site id from json object : {}", json.get("siteId"));
		
		//Get job id and site id from consumerRecord
		//the value in consumerRecord is in JSON
		//TO-DO/EXPLORE -
		// Establish a shared message schema between publishers and consumers
		//this is very fragile as there's a strong assumption that the JSON string is consistent
				
		logger.info("json object created: '{}'", json.toString());
		String jobId = (String)json.get("jobId");
		String siteId = (String)json.get("siteId");
		
		logger.info("acl url to check status: '{}'", url_api_acl_corres);
		CorresStatusResponse corresStatusResponse = restTemplate.getForObject(url_api_acl_corres, CorresStatusResponse.class, siteId, jobId);
		
		if (corresStatusResponse == null) {
			logger.info("ACL status response is null");
		}
		
		logger.info("'{}' '{}' '{}' '{}'", corresStatusResponse.getId(), corresStatusResponse.getInfo().getJobStatus(), corresStatusResponse.getInfo().getJobCompletionTime());
		logger.info("url '{}', siteId '{}', jobId '{}', result'{}'",url_api_acl_corres, siteId, jobId, corresStatusResponse);
		
		JSONObject jsonStatusResponse = new JSONObject();
		jsonStatusResponse.put("siteId", siteId);
		jsonStatusResponse.put("jobId", jobId);
		jsonStatusResponse.put("info", corresStatusResponse.getInfo());
		
		//TO-DO re-publish to Job-Request queue
		if (!corresStatusResponse.getInfo().getJobStatus().contains("100")) {
			logger.info(corresStatusResponse.getInfo().getJobStatus());			
		}
		
		kafkaTemplate.send(this.kafka_topic_job_status, jsonStatusResponse.toString());
		
		//TO-DO add web hooks/callback_url
		
	}
	
	
	@SuppressWarnings("unused")
	private boolean isValidJSONString(String jsonString) {
		try {
			new JSONObject(jsonString);			
		} catch (JSONException ex) {			
			try {
				new JSONArray(jsonString);
			} catch (JSONException e) {
				logger.info(ex.getMessage());
				return false;
			}
		} catch (final Exception e) {
			logger.info(e.getMessage());
			return false;
		}
		logger.info("Valdiation successful...");
		return true;
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

}
