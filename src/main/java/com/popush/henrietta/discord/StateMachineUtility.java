package com.popush.henrietta.discord;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachineException;

import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class StateMachineUtility {
    public static <T> T getMessageFromHeader(StateContext<BotStates, BotEvents> context, Class<T> clazz) {
        var planeMessageObject = context.getMessageHeader("message");
        if (!clazz.isInstance(planeMessageObject)) {
            throw new StateMachineException("a");
        }

        return (T) planeMessageObject;
    }

}
