package com.popush.henrietta.discord.states;

import static com.popush.henrietta.discord.StateMachineUtility.getMessageFromHeader;

import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.service.SendMessageService;
import com.popush.henrietta.discord.model.BotCallCommand;
import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;
import com.popush.henrietta.elasticsearch.service.ElasticsearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotIdleState extends InputBasedBotState {

    private final SendMessageService sendMessageService;
    private final ElasticsearchService elasticsearchService;

    private static final Map<String, BotEvents> eventHandleMap = Map.of("eu4", BotEvents.EU4_TRANSITION,
                                                                        "ck2", BotEvents.CK2_TRANSITION);

    @Override
    public Map<String, BotEvents> stateMap() {
        return eventHandleMap;
    }

    @Override
    public void setTransition(StateMachineTransitionConfigurer<BotStates, BotEvents> transitions)
            throws Exception {
        var base = BotStates.IDLE;

        transitions
                .withInternal()
                .source(base)
                .event(BotEvents.INPUT)
                .action(inputAction())

                .and()

                .withExternal()
                .source(base)
                .target(BotStates.EU4_SEARCH)
                .event(BotEvents.EU4_TRANSITION)
                .action(context -> {
                    MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
                    sendMessageService.sendPlaneMessage(event.getChannel(), "EU4モードに遷移します");
                })

                .and()

                .withExternal()
                .source(base)
                .target(BotStates.CK2_SEARCH)
                .event(BotEvents.CK2_TRANSITION)
                .action(context -> {
                    MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
                    sendMessageService.sendPlaneMessage(event.getChannel(), "CK2モードに遷移します");
                });
    }

    @Override
    void otherInput(BotCallCommand botCallCommand, StateContext<BotStates, BotEvents> context) {
        final MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
        final var result = elasticsearchService.search(botCallCommand);

        if (result.isEmpty()) {
            sendMessageService.sendPlaneMessage(event.getChannel(), "見つかりませんでした");
            return;
        }

        switch (botCallCommand.getCommand()) {
            case "g":
                sendMessageService.sendGrepMessage(event.getChannel(), result);
                break;
            case "n":
            default:
                sendMessageService.sendEmbedMessage(event.getChannel(), result.get(0));
                break;
        }
    }
}
