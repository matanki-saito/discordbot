package com.popush.henrietta.discord.project;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;

public interface Project {
    String getProjectName();

    Project makeClone(String request, MessageReceivedEvent event);

    void parseRequest() throws CommandErrorException;

    void execute();
}
