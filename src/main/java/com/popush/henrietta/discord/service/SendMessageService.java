package com.popush.henrietta.discord.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import com.popush.henrietta.discord.states.ParatranzEntry;
import com.popush.henrietta.elasticsearch.model.EsResponseWithData;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendMessageService {

    public void sendPlaneMessage(MessageChannel channel, String message) {
        channel.sendMessage(message).queue();
    }

    public void sendEmbedMessage(MessageChannel channel, EsResponseWithData<ParatranzEntry> withData) {

        final EmbedBuilder builder = new EmbedBuilder();

        builder.addField("key", String.format(
                "[%s](%s)",
                withData.getData().getKey(),
                String.format("https://paratranz.cn/projects/%d/strings?key=%s",
                              withData.getData().getPzPjCode(),
                              withData.getData().getKey())
        ), false);
        builder.addField("file", withData.getData().getFile(), false);
        builder.addField("original", withData.getData().getOriginal(), false);
        builder.addField("translation", withData
                                 .getData()
                                 .getTranslation(),
                         false);
        channel.sendMessage(builder.build()).queue(res -> {
        }, res -> {
            log.error(res.getMessage());
        });
    }

    public void sendGrepMessage(MessageChannel channel, List<EsResponseWithData<ParatranzEntry>> withDatas) {

        List<String> result = new ArrayList<>();

        String[] emojiNumber = { "0⃣", "1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣" };

        int idx = 0;
        for (var x : withDatas.subList(0, Math.min(9, withDatas.size()))) {
            result.add(String.format(
                    "%s %s",
                    emojiNumber[idx++],
                    x.getData().getKey()));

            Pattern p = Pattern.compile(".{0,10}("
                                        + String.join("|", x.getCallCommand().getSearchWords())
                                        + ").{0,10}",
                                        Pattern.CASE_INSENSITIVE);

            var mTrans = p.matcher(x.getData().getTranslation());
            while (mTrans.find()) {
                result.add(String.format("　　\uD83D\uDCAC %s", mTrans.group()));
            }

            var mKey = p.matcher(x.getData().getKey());
            while (mKey.find()) {
                result.add(String.format("　　\uD83D\uDCAD %s", mKey.group()));
            }

            var mOriginal = p.matcher(x.getData().getOriginal());
            while (mOriginal.find()) {
                result.add(String.format("　　\uD83D\uDC41\u200D\uD83D\uDDE8 %s", mOriginal.group()));
            }
        }

        channel.sendMessage(String.join("\n", result)).queue();
    }

}
