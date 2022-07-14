package com.baixiaozheng.distributedlock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {

    @Value("${zookeeper.server}")
    String server;

    @Bean(name = "curatorClientUtil", initMethod = "init", destroyMethod = "destroy")
    public CuratorClientUtil curatorClientUtil() {
        CuratorClientUtil clientUtil = new CuratorClientUtil(server);
        return clientUtil;
    }
}
