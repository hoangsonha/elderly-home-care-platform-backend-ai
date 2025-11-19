package com.capstone_project.elderly_platform.configurations;

import com.capstone_project.elderly_platform.services.ExpiredCareServiceProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration with Keyspace Notifications support for real-time
 * expiration events.
 * 
 * Required Redis configuration (run in Redis CLI or set in redis.conf):
 * CONFIG SET notify-keyspace-events Ex
 * 
 * This enables expiration events to be published to __keyevent@0__:expired
 * channel.
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    private static final String EXPIRED_KEYS_CHANNEL = "__keyevent@0__:expired";

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        
        // Auto-configure Redis Keyspace Notifications on startup
        configureKeyspaceNotifications(template);
        
        return template;
    }
    
    /**
     * Automatically configures Redis Keyspace Notifications to enable expiration events.
     * This ensures Redis will publish events to __keyevent@0__:expired channel when keys expire.
     * 
     * @param redisTemplate The Redis template to use for configuration
     */
    private void configureKeyspaceNotifications(StringRedisTemplate redisTemplate) {
        try {
            // Get current configuration
            String currentConfig = redisTemplate.execute((RedisCallback<String>) connection -> {
                RedisServerCommands commands = connection.serverCommands();
                java.util.Properties config = commands.getConfig("notify-keyspace-events");
                return config.getProperty("notify-keyspace-events", "");
            });
            
            log.info("Current Redis notify-keyspace-events config: {}", currentConfig);
            
            // Check if already configured
            if (currentConfig != null && !currentConfig.isEmpty() 
                    && currentConfig.contains("x") && currentConfig.contains("E")) {
                log.info("Redis Keyspace Notifications already enabled (contains 'Ex')");
                return;
            }
            
            // Set configuration: E = Enable keyspace events, x = Enable expired events
            redisTemplate.execute((RedisCallback<String>) connection -> {
                RedisServerCommands commands = connection.serverCommands();
                commands.setConfig("notify-keyspace-events", "Ex");
                return "OK";
            });
            
            log.info("✅ Successfully configured Redis Keyspace Notifications: notify-keyspace-events = Ex");
            log.info("Redis will now automatically publish expiration events to channel: {}", EXPIRED_KEYS_CHANNEL);
            
        } catch (Exception e) {
            log.error("❌ Failed to configure Redis Keyspace Notifications: {}", e.getMessage(), e);
            log.warn("⚠️  Please manually configure Redis: CONFIG SET notify-keyspace-events Ex");
            log.warn("⚠️  Without this configuration, expiration events will not work!");
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis Message Listener Container for listening to expiration events.
     * Subscribes to __keyevent@0__:expired channel to receive real-time expiration
     * notifications.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ExpiredCareServiceProcessor expiredCareServiceProcessor) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to expiration events channel
        // ExpiredCareServiceProcessor implements MessageListener
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(
                expiredCareServiceProcessor, "onMessage");

        container.addMessageListener(listenerAdapter, new ChannelTopic(EXPIRED_KEYS_CHANNEL));

        log.info("Redis Keyspace Notifications listener configured for channel: {}", EXPIRED_KEYS_CHANNEL);
        log.warn("IMPORTANT: Make sure Redis has 'notify-keyspace-events Ex' configured!");

        return container;
    }
}
