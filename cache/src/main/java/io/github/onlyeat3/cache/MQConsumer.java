package io.github.onlyeat3.cache;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MQConsumer implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread consumeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                        String queueValue = CacheApplication.QUEUE.poll();
                        if (queueValue != null) {
                            String[] split = queueValue.split(":");
                            String key = split[0];
                            String value = split[1];
                            log.info("消费数据 {}:{}", key, value);
                            //存入布隆过滤器,虽然写MQ的时候已经存过了，再存一次也没问题
                            CacheLoader.BLOOM_FILTER.put(key);
                            //写入数据库或者其他比较耗时的操作
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        consumeThread.setName("consumer-thread");
        consumeThread.start();
        log.info("MQ消费端已启动");
    }
}
