package io.github.onlyeat3.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ArrayBlockingQueue;

@EnableScheduling //启用定时任务
@SpringBootApplication
public class CacheApplication {
    public static final ArrayBlockingQueue<String> QUEUE = new ArrayBlockingQueue<String>(10000);

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }
}
