package com.popush.henrietta.discord.project.discussion;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;
import com.popush.henrietta.discord.project.Project;
import com.popush.henrietta.discord.project.discussion.option.DiscussionOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor
@Builder(toBuilder = true)
public class DiscussionProject implements Project {

    private final List<DiscussionOption> options;

    private String request;
    private MessageReceivedEvent context;

    private DiscussionOption option;

    @Override
    public String getProjectName() {
        return "dis";
    }

    @Override
    public Project makeClone(String request, MessageReceivedEvent context) {
        return this.toBuilder()
                   .request(request)
                   .context(context)
                   .build();
    }

    @Override
    public void parseRequest() throws CommandErrorException {
        // Options arg1 arg2 ...
        var texts = List.of(request.split(" "));

        if (texts.size() < 2) {
            throw new CommandErrorException();
        }

        var result = options.stream().filter(x -> x.matchOption(texts.get(0))).findAny();

        if (result.isEmpty()) {
            throw new CommandErrorException();
        }

        this.option = result.get().makeClone(texts.subList(1, texts.size()), context);
    }

    @Override
    public void execute() {
        this.option.execute();
    }
}
