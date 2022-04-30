package com.popush.henrietta.discord.project.search;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;
import com.popush.henrietta.discord.project.Project;
import com.popush.henrietta.discord.project.search.option.SearchOption;
import com.popush.henrietta.discord.service.SendMessageService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor

public abstract class GameProject implements Project {
    private final List<SearchOption> options;

    private final SendMessageService sendMessageService;

    private String request;

    private MessageReceivedEvent context;

    private SearchOption option;

    @Override
    public void parseRequest() throws CommandErrorException {
        // Options arg1 arg2 ...
        var texts = List.of(request.split(" "));

        if (texts.size() < 1) {
            throw new CommandErrorException();
        }

        var result = options.stream().filter(x -> x.matchOption(texts.get(0))).findAny();

        if (result.isEmpty()) {
            throw new CommandErrorException();
        }

        this.option = result.get().makeClone(this.getProjectName(), texts.subList(1, texts.size()), context);
    }

    @Override
    public void execute() {
        this.option.execute();

//        // 検索
//        // 検索取得最大数
//        final int size = botCallCommand.getCommands().contains("g") ? 10 : 5;
//        final EsResponseContainer<ParatranzEntry> result;
//        if (botCallCommand.getCommands().contains("b")) {
//            result = elasticsearchService.searchPartialMatch(botCallCommand, size);
//        } else {
//            result = elasticsearchService.searchTerm(botCallCommand, size);
//        }
//
//        if (result.getData().isEmpty()) {
//            sendMessageService.sendPlaneMessage(event.getChannel(), "見つかりませんでした");
//            return;
//        }
//
//        if (botCallCommand.getCommands().contains("g")) {
//            sendMessageService.sendGrepMessage(event.getChannel(), result);
//        } else {
//            sendMessageService.sendEmbedMessage(event.getChannel(), result);
//        }
    }
}
