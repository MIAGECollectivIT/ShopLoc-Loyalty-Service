package fr.miage.collectivit.loyaltyservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring task scheduling and asynchronous execution for periodic VFP revisions and events
 * (SLE-45 / SLE-202).
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {}
