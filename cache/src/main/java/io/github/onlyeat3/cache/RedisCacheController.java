package io.github.onlyeat3.cache;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class RedisCacheController {
    @Autowired private StringRedisTemplate redisTemplate;

    @RequestMapping("/randomRead")
    public String randomRead(){
        //随机生成个key
        int key = RandomUtil.randomInt(0, CacheLoader.MAX_CACHE_KEY);
        //使用布隆过滤器判断 数据 是否存在
        boolean containsKey = CacheLoader.BLOOM_FILTER.mightContain(key + "");
        if (!containsKey) {
            return "";
        }
        //查缓存
        String value = this.redisTemplate.opsForValue().get(key + "");
        if (StrUtil.isBlank(value)) {
            //如果缓存不存在，为了保证数据能正常返回，需要去数据库或者调用其他接口查询
            try {
                //mock 查库的结果
                TimeUnit.MILLISECONDS.sleep(200);
                return "db data";
            } catch (InterruptedException e) {
                log.warn("查询数据库失败",e);
                return "ex";
            }
        }
        return value;
    }


    /**
     * 写入数据后即可就能查到数据,可以用这个命令测试，会输出222
     * curl 'http://localhost:8080/write?key=bbb&value=222' -o /dev/null -s && curl 'http://localhost:8080/read?key=bbb'
     */
    @RequestMapping("/write")
    public String write(String key,String value){
        //1.基本的数据校验啥的，防止存的时候出错导致数据不一致
        if(StrUtil.isBlank(key)||StrUtil.isBlank(value)){
            return null;
        }
        //2.写队列.耗时高的处理逻辑放到队列消费端
        CacheApplication.QUEUE.offer(String.format("%s:%s",key,value));
        //3.队列写入成功后增加或更新缓存的数据
        this.redisTemplate.opsForValue().set(key,value,60, TimeUnit.MINUTES);
        //写缓存成功后在bloomfilter也存一份，防止误判为数据不存在
        CacheLoader.BLOOM_FILTER.put(key);
        return "success";
    }

    /**
     * 对于没有在缓存的数据，请求过来的时候还是需要查库才能返回。
     * 如果数据确实不存在，每次请求都会查库，会长时间占用线程，QPS也会因此降低
     * 如果查一个不存在的key，直接查redis QPS 6819,通过bloomfilter判断的情况，QPS 10782
     */
    @RequestMapping("/read")
    public String read(String key){
        boolean containsKey = CacheLoader.BLOOM_FILTER.mightContain(key + "");
        if (!containsKey) {
            return "";
        }
        String value = this.redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(value)) {
            //如果缓存不存在，为了保证数据能正常返回，需要去数据库或者调用其他接口查询
            try {
                //mock 查库的结果
                TimeUnit.MILLISECONDS.sleep(200);
                return "db data";
            } catch (InterruptedException e) {
                log.warn("查询数据库失败",e);
                return "ex";
            }
        }
        return value;
    }
}
