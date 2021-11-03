package io.github.onlyeat3.mybest.cache;

import cn.hutool.core.util.RandomUtil;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheLoader {
    @Autowired private StringRedisTemplate redisTemplate;
    public static final Integer MAX_CACHE_KEY = 10000;
    public static final BloomFilter<String> BLOOM_FILTER = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),Integer.MAX_VALUE);

    //如果是多个实例，需要使用分布式的任务调度框架处理，避免多个节点同时执行导致业务逻辑错误
    //执行频率为1分，失效时间为60分。保证请求过来的时候一定有缓存
    @Scheduled(fixedDelay = 1*60*1000)
    public void loadAndRefreshCache(){
        log.info("开始加载缓存");
        for (int i = 0; i < MAX_CACHE_KEY; i++) {
            String s = RandomUtil.randomString(RandomUtil.randomInt(MAX_CACHE_KEY/2, MAX_CACHE_KEY));
            //有效期为60分
            String key = i + "";
            this.redisTemplate.opsForValue().set(key,s,60, TimeUnit.MINUTES);
            BLOOM_FILTER.put(key);
        }
        log.info("已加载0-{}的数据缓存",MAX_CACHE_KEY);
    }

    @PostConstruct
    public void init(){
        log.info("加载缓存的定时任务已启动");
    }
}
