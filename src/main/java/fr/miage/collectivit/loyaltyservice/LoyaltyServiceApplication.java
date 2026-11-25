package fr.miage.collectivit.loyaltyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

/** Main entry point for the ShopLoc Loyalty Service application. */
@EnableCaching
@SpringBootApplication
public class LoyaltyServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LoyaltyServiceApplication.class, args);
  }

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("loyaltyRates");
  }
}
