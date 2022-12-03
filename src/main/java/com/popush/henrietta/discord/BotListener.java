package com.popush.henrietta.discord;

import com.popush.henrietta.biz.discussion.DiscussionFlow;
import com.popush.henrietta.biz.project.SearchFlow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class BotListener extends ListenerAdapter {
    private final DiscussionFlow discussionFlow;

    private final SearchFlow searchFlow;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        searchFlow.messageReceived(event);

        MDC.clear();
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        discussionFlow.messageReactionAdd(event);

        MDC.clear();
    }
}
