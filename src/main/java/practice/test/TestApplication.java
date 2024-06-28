package practice.test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
		RedisClient redisClient = RedisClient.create("redis://localhost/0");
		StatefulRedisConnection<String, String> connection = redisClient.connect();

		System.out.println("Connected to Redis");
		connection.sync().set("key", "Hello World");

		connection.close();
		redisClient.shutdown();
	}

}
