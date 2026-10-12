package com.queueify.campaignservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.queueify.campaignservice.storage.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(S3Properties.class)
public class QueueifyCampaignServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueueifyCampaignServiceApplication.class, args);
	}

}
