package com.popush.henrietta.discord.project.search;

import java.util.List;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.project.search.option.SearchOption;
import com.popush.henrietta.discord.service.SendMessageService;

import lombok.Builder;

@Builder(toBuilder = true)
public class Ck2Project extends GameProject {
    public Ck2Project(List<SearchOption> options, SendMessageService sendMessageService, String request,
                      MessageReceivedEvent context, SearchOption option) {
        super(options, sendMessageService, request, context, option);
    }

    @Override
    public String getProjectName() {
        return "ck2";
    }

    @Override
    public GameProject makeClone(String request, MessageReceivedEvent context) {
        return this.toBuilder()
                   .request(request)
                   .context(context)
                   .build();
    }
}
