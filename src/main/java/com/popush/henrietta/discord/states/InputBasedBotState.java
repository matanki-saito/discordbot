package com.popush.henrietta.discord.states;

import static com.popush.henrietta.discord.StateMachineUtility.getMessageFromHeader;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.exception.CommandErrorException;
import com.popush.henrietta.discord.model.BotCallCommand;
import com.popush.henrietta.discord.model.BotEvents;
import com.popush.henrietta.discord.model.BotStates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class InputBasedBotState implements BotStateTemplate<BotStates, BotEvents, String> {

    private final Pattern pattern = Pattern.compile("^([a-zA-Z0-9\\-_]{1,4})::([a-z0-9=]*)[\\n|\\r| |　]+(.*)?");

    private BotCallCommand parseCallOutMeCommand(MessageReceivedEvent event) throws CommandErrorException {
        // botの投稿は無視する
        if (event.getAuthor().isBot()) {
            throw new CommandErrorException();
        }

        log.info(event.getMessage().getContentRaw());

        // パターンに一致しているかどうか
        String message = event.getMessage().getContentRaw();
        var m = pattern.matcher(message);
        if (!m.find()) {
            throw new CommandErrorException();
        }

        // パース結果を保存
        var result = new BotCallCommand();
        result.setIndex(m.group(1).toLowerCase());

        if (m.group(2) != null) {
            result.setCommands(List.of(m.group(2).split("")));
        } else {
            result.setCommands(List.of("n"));
        }

        // 検索ワード
        if (m.group(3) != null) {
            result.setSearchWords(List.of(m.group(3).split("[ |　]")));
        }

        return result;
    }

    public Action<BotStates, BotEvents> inputAction() {
        return context -> {
            MessageReceivedEvent event = getMessageFromHeader(context, MessageReceivedEvent.class);
            String messageText = event.getMessage().getContentRaw();

            try {
                BotCallCommand callCommand = parseCallOutMeCommand(event);
                otherInput(callCommand, context);
            } catch (CommandErrorException e) {
                log.debug("command error");
            }
        };
    }

    void otherInput(BotCallCommand messageText, StateContext<BotStates, BotEvents> context) {
    }
}
