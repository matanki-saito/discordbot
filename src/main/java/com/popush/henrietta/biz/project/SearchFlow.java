package com.popush.henrietta.biz.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchFlow {
    private final List<Project> projects;

    public void messageReceived(MessageReceivedEvent event) {
        // botの投稿は無視する
        if (event.getAuthor().isBot()) {
            return;
        }

        var optionalProject = projects
                .stream()
                .filter(x -> x.parseRequest(event.getMessage().getContentRaw(),
                        event))
                .findAny();

        if (optionalProject.isEmpty()) {
            return;
        }

        optionalProject.get().execute();
    }
}
