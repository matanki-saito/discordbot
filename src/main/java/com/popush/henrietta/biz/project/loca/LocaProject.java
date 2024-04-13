package com.popush.henrietta.biz.project.loca;

import com.github.matanki_saito.rico.exception.ArgumentException;
import com.github.matanki_saito.rico.exception.MachineException;
import com.github.matanki_saito.rico.loca.PdxLocaMatchPattern;
import com.popush.henrietta.biz.project.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
@Scope("prototype")
@Order(0)
public class LocaProject implements Project {

    private static final Pattern p = Pattern.compile("^::([a-zA-Z])");

    private final PdxLocaMatchPattern pdxLocaMatchPattern;

    private MessageChannel messageChannel;

    @Override
    public boolean parseRequest(String request, MessageReceivedEvent context) {
        var target = request.toLowerCase(Locale.ROOT);

        var matcher = p.matcher(target);

        if (!matcher.find()) {
            return false;
        }

        messageChannel = context.getChannel();

        return true;
    }

    @Override
    public void execute() {
        try {
            pdxLocaMatchPattern.reload();
        } catch (MachineException | ArgumentException e) {
            throw new RuntimeException(e);
        }

        messageChannel.sendMessage("success").queue();
    }
}
