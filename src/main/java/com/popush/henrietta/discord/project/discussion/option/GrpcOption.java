package com.popush.henrietta.discord.project.discussion.option;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Scope("prototype")
public class GrpcOption implements DiscussionOption {

    protected List<String> arguments;
    protected MessageReceivedEvent context;

    @Override
    public boolean parseRequest(String request, MessageReceivedEvent context) {
        var requests = List.of(request.split( " "));

        if(!requests.get(0).toLowerCase(Locale.ROOT).equals("g")){
            return false;
        }

        this.context = context;
        return true;
    }

    @Override
    public void execute() {
        log.info("create thread");
        var jda = context.getChannel().getJDA();
        var channelId = context.getChannel().getIdLong();
        var guildChannel = jda.getGuildChannelById(channelId);
        var guild = Optional.ofNullable(guildChannel).orElseThrow().getGuild();
        var categories = guild.getCategoriesByName("Discussion", true);
        if (categories.isEmpty()) {
            categories = List.of(guild.createCategory("Discussion").complete());
        }

        var textChannel = guild
                .createTextChannel("test-" + UUID.randomUUID().toString().substring(0, 5),
                                   categories.get(0))
                .complete();
        var textChannelId = textChannel.getId();
        log.info("textChannelId:" + textChannelId);
        var res = textChannel.sendMessage("hogehoge: gesogeso").complete();
        log.info(res.getJumpUrl());
    }
}
