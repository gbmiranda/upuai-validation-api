package cloud.upuai.validation.config;

import cloud.upuai.validation.service.DocumentProcessingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;

import java.net.URI;
import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static final String STREAM_KEY = "documents:process";
    public static final String CONSUMER_GROUP = "doc-processors";
    public static final String CONSUMER_NAME = "worker-1";

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException("REDIS_URL environment variable is required");
        }

        // Parse redis://:password@host:port or redis://host:port
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(uri.getHost());
        config.setPort(uri.getPort() > 0 ? uri.getPort() : 6379);

        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            config.setPassword(userInfo.substring(userInfo.indexOf(':') + 1));
        }

        log.info("Connecting to Redis at {}:{}", uri.getHost(), uri.getPort());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public StreamMessageListenerContainer<String, ?> streamListenerContainer(
            RedisConnectionFactory factory,
            DocumentProcessingConsumer consumer,
            StringRedisTemplate redisTemplate) {

        // Ensure stream and consumer group exist
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
            log.info("Created Redis Stream consumer group '{}'", CONSUMER_GROUP);
        } catch (Exception e) {
            // Group already exists — that's fine
            log.debug("Consumer group '{}' already exists: {}", CONSUMER_GROUP, e.getMessage());
        }

        var options = StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        var container = StreamMessageListenerContainer.create(factory, options);

        Subscription subscription = container.receiveAutoAck(
                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                org.springframework.data.redis.connection.stream.StreamOffset.create(STREAM_KEY,
                        org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed()),
                consumer
        );

        container.start();
        log.info("Redis Stream listener started on '{}'", STREAM_KEY);
        return container;
    }
}
