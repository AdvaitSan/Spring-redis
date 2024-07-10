package practice.test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.connection.RedisClusterConnection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
//		RedisClient redisClient = RedisClient.create("redis://localhost/0");
//		StatefulRedisConnection<String, String> connection = redisClient.connect();
//
//		System.out.println("Connected to Redis");
//		connection.sync().set("key", "Hello World");
//
//		connection.close();
//		redisClient.shutdown();
//		RedisClient clusterClient = RedisClient.create("redis://172.20.0.7:6379,redis://172.20.0.5:6379,redis://172.20.0.6:6379,redis://172.20.0.2:6379,redis://172.20.0.3:6379,redis://172.20.0.4:6379");
//
//		// Connect to the cluster
//		StatefulRedisClusterConnection<String, String> clusterConnection = (StatefulRedisClusterConnection<String, String>) clusterClient.connect();
//
//		// Get the async commands for the cluster
//		RedisAdvancedClusterAsyncCommands<String, String> async = clusterConnection.async();
//
//		// Example usage:
//		async.set("thing1", "test1");
//		async.set("thing2", "test2");
//
//		async.keys("*");
//
//		// Don't forget to close the connection when you're done
//		clusterConnection.close();
//		clusterClient.shutdown();

	}

}
