package com.baixiaozheng.distributedlock.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.Objects;

@Slf4j
public class CuratorClientUtil {
    private String zookeeperServer;

    @Getter
    private CuratorFramework client;

    public CuratorClientUtil(String zookeeperServer) {
        this.zookeeperServer = zookeeperServer;
    }

    // 创建CuratorFrameworkFactory并且启动
    public void init() {
        // 重试策略,等待1s,最大重试3次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        this.client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .build();
        this.client.start();
    }

    // 容器关闭,CuratorFrameworkFactory关闭
    public void destroy() {
        try {
            if (Objects.nonNull(getClient())) {
                getClient().close();
            }
        } catch (Exception e) {
            log.info("CuratorFramework close error=>{}", e.getMessage());
        }
    }
}
