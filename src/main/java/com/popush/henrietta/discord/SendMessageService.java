package com.popush.henrietta.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import com.popush.henrietta.discord.states.ParatranzEntry;
import com.popush.henrietta.elasticsearch.model.EsResponseWithData;

@Service
public class SendMessageService {

    public void sendPlaneMessage(MessageChannel channel, String message) {
        channel.sendMessage(message).queue();
    }

    public void sendEmbedMessage(MessageChannel channel, EsResponseWithData<ParatranzEntry> withData) {

        final EmbedBuilder builder = new EmbedBuilder();

        builder.addField("key", String.format(
                "[%s](%s)",
                withData.getData().getKey(),
                String.format("https://paratranz.cn/projects/76/strings?key=%s", withData.getData().getKey())
        ), false);
        builder.addField("file", withData.getData().getFile(), false);
        builder.addField("original", withData.getData().getOriginal(), false);
        builder.addField("translation", withData
                                 .getData()
                                 .getTranslation(),
                         false);
        channel.sendMessage(builder.build()).queue();
    }

    public void sendGrepMessage(MessageChannel channel, List<EsResponseWithData<ParatranzEntry>> withDatas) {

        List<String> result = new ArrayList<>();

        int idx = 1;
        for (var x : withDatas) {
            Pattern p = Pattern.compile(".{0,7}" + x.getCallCommand().getSearchWords().get(0) + ".{0,7}");
            var m = p.matcher(x.getData().getTranslation());
            if (!m.find()) {
                return;
            }
            result.add(String.format(
                    "> %d: %s",
                    idx++,
                    x.getData().getKey()));
            result.add(String.format("　　%s", m.group(0)));
        }

        channel.sendMessage(String.join("\n", result)).queue();
    }

}
