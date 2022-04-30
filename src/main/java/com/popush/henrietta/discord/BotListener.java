package com.popush.henrietta.discord;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.popush.henrietta.discord.exception.CommandErrorException;
import com.popush.henrietta.discord.service.BotCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotListener extends ListenerAdapter {

    private final BotCommandService botCommandService;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        var project = botCommandService.prepareProject(event);

        if (project.isEmpty()) {
            return;
        }

        var contents = project.get();

        try {
            contents.parseRequest();
        } catch (CommandErrorException e) {
            throw new RuntimeException(e);
        }

        contents.execute();

        MDC.clear();
    }
}
