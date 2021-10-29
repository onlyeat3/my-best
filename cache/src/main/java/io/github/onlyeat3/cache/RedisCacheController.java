package io.github.onlyeat3.cache;

import cn.hutool.bloomfilter.BloomFilterUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.onlyeat3.cache.CacheLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@RestController
public class RedisCacheController {
    @Autowired private StringRedisTemplate redisTemplate;

    @RequestMapping("/randomRead")
    public String randomRead(){
        int key = RandomUtil.randomInt(0, CacheLoader.MAX_CACHE_KEY);
        return this.redisTemplate.opsForValue().get(key+"");
    }


    /**
     * 写入数据后即可就能查到数据,可以用这个命令测试，会输出222
     * curl 'http://localhost:8080/write?key=bbb&value=222' -o /dev/null -s && curl 'http://localhost:8080/read?key=bbb'
     */
    @RequestMapping("/write")
    public String write(String key,String value){
        //1.写队列.耗时高的处理逻辑放到队列消费端
        CacheApplication.QUEUE.offer(String.format("%s:%s",key,value));
        //2.队列写入成功后增加或更新缓存的数据
        this.redisTemplate.opsForValue().set(key,value,60, TimeUnit.MINUTES);
        return "success";
    }

    @RequestMapping("/read")
    public String read(String key){
        return this.redisTemplate.opsForValue().get(key);
    }
}
