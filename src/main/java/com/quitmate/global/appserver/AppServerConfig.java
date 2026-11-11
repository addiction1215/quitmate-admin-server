package com.quitmate.global.appserver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppServerProperties.class)
public class AppServerConfig {
}
