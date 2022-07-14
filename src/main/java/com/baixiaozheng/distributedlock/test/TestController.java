package com.baixiaozheng.distributedlock.test;

import com.baixiaozheng.distributedlock.config.CuratorClientUtil;
import com.baixiaozheng.distributedlock.config.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class TestController {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CuratorClientUtil curatorClientUtil;
    @Value("${zookeeper.lockpath}")
    private String lockPath;


    @RequestMapping(value = "/testRedisson", method = RequestMethod.POST)
    public void testRedisson(String key) throws ExecutionException, InterruptedException {

        RLock lock = redissonClient.getLock(key);
        CompletableFuture<Void> completableFuture1 = CompletableFuture.runAsync(() -> {

            try {
                if (lock.tryLock(10, 3, TimeUnit.SECONDS)) {
                    log.info("线程1获取锁成功");
                    Thread.sleep(2000);
                } else {
                    log.info("线程1获取锁失败");
                }
            } catch (InterruptedException e) {
                log.info("线程1获取锁异常");
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("线程1释放锁");
                }
            }
        }, threadPoolTaskExecutor);

        CompletableFuture<Void> completableFuture2 = CompletableFuture.runAsync(() -> {
            try {
                if (lock.tryLock(10, 3, TimeUnit.SECONDS)) {
                    log.info("线程2获取锁成功");
                    Thread.sleep(2000);
                } else {
                    log.info("线程2获取锁失败");
                }
            } catch (InterruptedException e) {
                log.info("线程2获取锁异常");
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("线程2释放锁");
                }
            }
        }, threadPoolTaskExecutor);

        CompletableFuture.allOf(completableFuture1, completableFuture2).get();


    }

    @RequestMapping(value = "/testRedis")
    public void testRedis(String key) throws ExecutionException, InterruptedException {

        CompletableFuture<Void> completableFuture1 = CompletableFuture.runAsync(() -> {
            String value = UUID.randomUUID().toString();
            try {
                if (redisUtil.lockBlock(key, value, 3L, 10L, TimeUnit.SECONDS)) {
                    log.info("线程1获取锁成功，value is {}", value);
                    Thread.sleep(2000);
                } else {
                    log.info("线程1获取锁失败，value is {}", value);
                }
            } catch (InterruptedException e) {

                log.info("线程1获取锁异常，value is {}", value);
            } finally {
                if (redisUtil.unlock(key, value)) {
                    log.info("线程1释放锁，value is {}", value);
                }
            }
        }, threadPoolTaskExecutor);

        CompletableFuture<Void> completableFuture2 = CompletableFuture.runAsync(() -> {
            String value = UUID.randomUUID().toString();
            try {
                if (redisUtil.lockBlock(key, value, 3L, 10L, TimeUnit.SECONDS)) {
                    log.info("线程2获取锁成功，value is {}", value);
                    Thread.sleep(2000);
                } else {
                    log.info("线程2获取锁失败，value is {}", value);
                }
            } catch (InterruptedException e) {
                log.info("线程2获取锁异常，value is {}", value);
            } finally {
                if (redisUtil.unlock(key, value)) {
                    log.info("线程2释放锁，value is {}", value);
                }
            }
        }, threadPoolTaskExecutor);

        CompletableFuture.allOf(completableFuture1, completableFuture2).get();
    }

    @RequestMapping(value = "/testZookeeper", method = RequestMethod.POST)
    public void testZookeeper() throws ExecutionException, InterruptedException {

        InterProcessMutex mutex = new InterProcessMutex(curatorClientUtil.getClient(), lockPath);

        CompletableFuture<Void> completableFuture1 = CompletableFuture.runAsync(() -> {
            try {
                if (mutex.acquire(3L, TimeUnit.SECONDS)) {
                    log.info("线程1获取锁成功");
                    Thread.sleep(5000);
                } else {
                    log.info("线程1获取锁失败");
                }
            } catch (Exception e) {
                log.info("线程1获取锁异常");
                throw new RuntimeException(e);
            } finally {
                try {
                    if (mutex.isOwnedByCurrentThread()) {
                        mutex.release();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, threadPoolTaskExecutor);

        CompletableFuture<Void> completableFuture2 = CompletableFuture.runAsync(() -> {
            try {
                if (mutex.acquire(3L, TimeUnit.SECONDS)) {
                    log.info("线程2获取锁成功");
                    Thread.sleep(5000);
                } else {
                    log.info("线程2获取锁失败");
                }
            } catch (Exception e) {
                log.info("线程2获取锁异常");
                throw new RuntimeException(e);
            } finally {
                try {
                    if (mutex.isOwnedByCurrentThread()) {
                        mutex.release();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                log.info("线程2释放锁");
            }
        }, threadPoolTaskExecutor);
        CompletableFuture.allOf(completableFuture1, completableFuture2).get();
    }

}
