package com.popush.henrietta.biz.project;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Project {
    boolean parseRequest(String request, MessageReceivedEvent context);

    void execute();
}
