package com.popush.henrietta.biz.project.discussion;

import java.util.List;
import java.util.Locale;

import com.popush.henrietta.biz.project.Project;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.biz.project.discussion.option.DiscussionOption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Scope("prototype")
public class DiscussionProject implements Project {

    private final List<DiscussionOption> options;

    private String request;
    private MessageReceivedEvent context;

    private DiscussionOption option;

    @Override
    public boolean parseRequest(String request, MessageReceivedEvent context) {
        var target = request.toLowerCase(Locale.ROOT);

        if (!target.startsWith("dis::")) {
            return false;
        }

        var optionRequest = target.subSequence(5,target.length()).toString();

        this.option = options
                .stream()
                .filter(x -> x.parseRequest(optionRequest, context))
                .findAny()
                .orElseThrow();

        return true;
    }

    @Override
    public void execute() {
        this.option.execute();
    }
}
