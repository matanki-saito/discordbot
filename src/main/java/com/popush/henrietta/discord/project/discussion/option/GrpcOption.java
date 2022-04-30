package com.popush.henrietta.discord.project.discussion.option;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor
@Builder(toBuilder = true)
public class GrpcOption implements DiscussionOption {

    protected List<String> arguments;
    protected MessageReceivedEvent context;

    @Override
    public boolean matchOption(String optionText) {
        return optionText.contains("g");
    }

    @Override
    public DiscussionOption makeClone(List<String> arguments, MessageReceivedEvent context) {
        return this.toBuilder()
                   .arguments(arguments)
                   .context(context)
                   .build();
    }

    @Override
    public void parseArguments() throws CommandErrorException {
        //
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
