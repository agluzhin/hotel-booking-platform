package com.hotelbooking.booking.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Настраивает REST-клиент с service discovery через Eureka. */
@Configuration
public class RestClientConfig {
  /** Создаёт балансируемый RestClient.Builder. */
  @Bean
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder() {
    return RestClient.builder();
  }
}
