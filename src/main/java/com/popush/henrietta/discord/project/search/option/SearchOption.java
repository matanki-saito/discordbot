package com.popush.henrietta.discord.project.search.option;

import java.util.List;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;

public interface SearchOption {
    boolean matchOption(String optionText);

    SearchOption makeClone(String request, List<String> saveArguments, MessageReceivedEvent context);

    void parseArguments() throws CommandErrorException;

    void execute();
}
