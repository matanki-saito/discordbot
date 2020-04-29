package com.popush.henrietta.discord.states;

import static com.popush.henrietta.discord.StateMachineUtility.getMessageFromHeader;

import java.util.Map;

import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.service.SendMessageService;
import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotCk2SearchState extends InputBasedBotState {
    private final SendMessageService sendMessageService;

    private static final Map<String, BotEvents> eventHandleMap = Map.of("end", BotEvents.END,
                                                                        "move eu4 mode",
                                                                        BotEvents.EU4_TRANSITION);

    @Override
    public Map<String, BotEvents> stateMap() {
        return eventHandleMap;
    }

    @Override
    public void setTransition(StateMachineTransitionConfigurer<BotStates, BotEvents> transitions)
            throws Exception {
        var base = BotStates.CK2_SEARCH;

        transitions
                .withInternal()
                .source(base)
                .action(inputAction())
                .event(BotEvents.INPUT)

                .and()

                .withExternal()
                .source(base)
                .target(BotStates.IDLE)
                .event(BotEvents.END)
                .action(context -> {
                    MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
                    sendMessageService.sendPlaneMessage(event.getChannel(), "終了します");
                })

                .and()

                .withExternal()
                .source(base)
                .target(BotStates.EU4_SEARCH)
                .event(BotEvents.EU4_TRANSITION)
                .action(context -> {
                    MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
                    sendMessageService.sendPlaneMessage(event.getChannel(), "EU4モードに遷移します");
                });
    }
}
