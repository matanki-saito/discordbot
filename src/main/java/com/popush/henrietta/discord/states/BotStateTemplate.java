package com.popush.henrietta.discord.states;

import java.util.Map;

import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

public interface BotStateTemplate<T, S, P> {
    void setTransition(StateMachineTransitionConfigurer<T, S> transitions) throws Exception;

    Map<P, S> stateMap();
}
