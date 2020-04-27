package com.popush.henrietta.discord.states;

import static com.popush.henrietta.discord.StateMachineUtility.getMessageFromHeader;

import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;

public abstract class InputBasedBotState implements BotStateTemplate<BotStates, BotEvents, String> {
    public Action<BotStates, BotEvents> inputAction() {
        return context -> {
            MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
            String messageText = event.getMessage().getContentRaw();

            if (event.getAuthor().isBot()) {
                return;
            }

            if (this.stateMap().containsKey(messageText)) {
                context.getStateMachine()
                       .sendEvent(MessageBuilder
                                          .withPayload(this.stateMap().get(messageText))
                                          .setHeader("message", event)
                                          .build());
            } else {
                otherInput(messageText, context);
            }
        };
    }

    void otherInput(String messageText, StateContext<BotStates, BotEvents> context) {
    }
}
