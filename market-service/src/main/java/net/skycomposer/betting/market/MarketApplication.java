package net.skycomposer.betting.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class MarketApplication {

  public static void main(String[] args) {
    SpringApplication.run(MarketApplication.class, args);
  }
}
