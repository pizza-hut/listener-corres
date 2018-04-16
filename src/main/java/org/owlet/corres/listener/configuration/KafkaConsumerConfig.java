package org.owlet.corres.listener.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.owlet.corres.listener.Listener;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {


	@Value("${kafka.bootstrapAddress}")
	private String kafka_bootstrapAddress;

	@Value("${kafka.username}")
	private String kafka_usr;

	@Value("${kafka.password}")
	private String kafka_pwd;

	@Value("${spring.kafka.client-id}")
    private String spring_kafka_client_id;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String spring_kafka_consumer_group_id;
    
    
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {

		String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
		String jaasCfg = String.format(jaasTemplate, this.kafka_usr, this.kafka_pwd);
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafka_bootstrapAddress);
		configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configProps.put(ConsumerConfig.CLIENT_ID_CONFIG, spring_kafka_client_id);
		configProps.put("security.protocol", "SASL_SSL");        
		configProps.put("sasl.mechanism", "SCRAM-SHA-256");
		configProps.put("sasl.jaas.config", jaasCfg);
		configProps.put(ConsumerConfig.GROUP_ID_CONFIG, this.spring_kafka_consumer_group_id);
		
		return new DefaultKafkaConsumerFactory<>(configProps);		
	}

	
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		//factory.setConcurrency(2);
		return factory;
	}
	
	
	@Bean
	public Listener listener() {
		return new Listener();
	}

}
