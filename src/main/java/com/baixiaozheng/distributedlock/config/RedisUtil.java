package com.baixiaozheng.distributedlock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 */
@Slf4j
public class RedisUtil {
    private RedisTemplate<Serializable, Object> redisTemplate;


    public void setRedisTemplate(RedisTemplate<Serializable, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    private static final Long RELEASE_SUCCESS = 1L;

    /**
     * 尝试获取锁 立即返回
     *
     * @param key
     * @param value
     * @param timeout
     * @return
     */
    public boolean lock(String key, String value, long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 以阻塞方式的获取锁
     *
     * @param key key
     * @param value value
     * @param lockTimeout 锁超时时间
     * @param getTimeout 获取锁超时时间
     * @return
     */
    public boolean lockBlock(String key, String value, long lockTimeout, long getTimeout, TimeUnit timeUnit) {
        long start = System.currentTimeMillis();
        while (true) {
            //检测是否超时
            if (System.currentTimeMillis() - start > getTimeout) {
                log.error("get lock timeout");
                return false;
            }
            //执行set命令
            //1
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, value, lockTimeout, timeUnit);
            //其实没必要判NULL，这里是为了程序的严谨而加的逻辑
            if (absent == null) {
                log.error("get lock absent is null");
                return false;
            }
            //是否成功获取锁
            if (absent) {
//                log.info("get lock : {},{}",key,value);
                return true;
            } else {
                log.info("get lock fail：{},{}",key,value);
            }
        }
    }

    public boolean unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        RedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);

        Object result = redisTemplate.execute(redisScript, Collections.singletonList(key),value);
//        log.info("unlock : {},{} and result is {}",key,value,result);
        if(RELEASE_SUCCESS.equals(result)) {
//            log.info("unlock ：{},{}",key,value);
            return true;
        }
        log.error("unlock error");
        return false;


    }
}
