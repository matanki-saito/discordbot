package com.popush.henrietta.discord.project.discussion.option;

import java.util.List;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;

@Component
public interface DiscussionOption {
    boolean matchOption(String optionText);

    DiscussionOption makeClone(List<String> saveArguments, MessageReceivedEvent context);

    void parseArguments() throws CommandErrorException;

    void execute();
}
