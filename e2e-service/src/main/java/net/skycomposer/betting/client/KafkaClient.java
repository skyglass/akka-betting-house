package net.skycomposer.betting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kafka")
public interface KafkaClient {

    @DeleteMapping("/topics/{topicName}/messages")
    public ResponseEntity<String> clearMessages(@PathVariable("topicName") String topicName);
}