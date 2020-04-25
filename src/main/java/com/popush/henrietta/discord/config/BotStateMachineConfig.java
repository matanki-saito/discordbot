package com.popush.henrietta.discord.config;

import java.util.EnumSet;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;

import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;
import com.popush.henrietta.discord.states.BotStateTemplate;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BotStateMachineConfig {
    private final List<BotStateTemplate<BotStates, BotEvents, String>> states;

    @Bean
    @Scope(scopeName = "prototype")
    public StateMachine<BotStates, BotEvents> stateMachineTarget() throws Exception {
        StateMachineBuilder.Builder<BotStates, BotEvents> builder =
                StateMachineBuilder.<BotStates, BotEvents>builder();

        builder.configureConfiguration()
               .withConfiguration()
               .autoStartup(true);

        builder.configureStates()
               .withStates()
               .initial(BotStates.IDLE)
               .states(EnumSet.allOf(BotStates.class));

        states.forEach(x -> {
            try {
                x.setTransition(builder.configureTransitions());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return builder.build();
    }
}
