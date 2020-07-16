package com.popush.henrietta.discord.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import com.popush.henrietta.discord.states.ParatranzEntry;
import com.popush.henrietta.elasticsearch.model.EsResponseContainer;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendMessageService {

    public void sendPlaneMessage(MessageChannel channel, String message) {
        channel.sendMessage(message).queue();
    }

    public void sendEmbedMessage(MessageChannel channel, EsResponseContainer<ParatranzEntry> container) {

        final EmbedBuilder builder = new EmbedBuilder();

        final var data = container.getData().get(0).getData();

        final var url = String.format("https://paratranz.cn/projects/%d/strings?key=%s",
                                      data.getPzPjCode(),
                                      data.getKey());
        final var key = String.format("[%s](%s)", data.getKey(), url);

        builder.appendDescription(String.format("%d件見つかりました。1件目を表示します", container.getFindCount()));

        builder.addField("key",
                         StringUtils.abbreviate(Optional.ofNullable(key).orElse("不明"),
                                                1000),
                         false);

        builder.addField("file",
                         StringUtils.abbreviate(Optional.ofNullable(data.getFile()).orElse("不明"),
                                                1000),
                         false);

        builder.addField("original",
                         StringUtils.abbreviate(Optional.ofNullable(data.getOriginal()).orElse("不明"),
                                                1000),
                         false);

        builder.addField("translation",
                         StringUtils.abbreviate(Optional.ofNullable(data.getTranslation()).orElse("不明"),
                                                1000),
                         false);

        channel.sendMessage(builder.build()).queue(res -> {
        }, res -> log.error(res.getMessage()));
    }

    public void sendGrepMessage(MessageChannel channel,
                                EsResponseContainer<ParatranzEntry> container) {

        List<String> result = new ArrayList<>();

        String[] emojiNumber = { "0⃣", "1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣" };

        int idx = 0;
        for (var x : container.getData().subList(0, Math.min(9, container.getData().size()))) {
            result.add(String.format(
                    "%s %s",
                    emojiNumber[idx++],
                    x.getData().getKey()));

            Pattern p = Pattern.compile(".{0,10}("
                                        + String.join("|", container.getCallCommand().getSearchWords())
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
