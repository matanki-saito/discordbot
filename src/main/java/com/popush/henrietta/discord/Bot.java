package com.popush.henrietta.discord;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class Bot extends ListenerAdapter {
    private static final String REDIS_KEY_PREFIX = "bot-state";

    private final StateMachine<BotStates, BotEvents> stateMachine;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final StateMachinePersister<BotStates, BotEvents, String> stateMachinePersister;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        var botEventWithMessage = MessageBuilder
                .withPayload(BotEvents.INPUT)
                .setHeader("message", event)
                .build();

        var key = String.format("%s:%s", REDIS_KEY_PREFIX, event.getAuthor().getId());

        try {
            stateMachinePersister.restore(stateMachine, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        stateMachine.sendEvent(botEventWithMessage);

        try {
            stateMachinePersister.persist(stateMachine, key);
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
