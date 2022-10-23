package com.popush.henrietta.discord.project.discussion.option;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Component
public interface DiscussionOption {

    boolean parseRequest(String request, MessageReceivedEvent context);

    void execute();
}
