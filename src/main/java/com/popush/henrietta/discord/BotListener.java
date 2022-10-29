package com.popush.henrietta.discord;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.popush.henrietta.discord.project.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotListener extends ListenerAdapter {
    private final List<Project> projects;

    private final GitHub gitHub;

    @Value("${discord.target-channel}")
    private String targetChannel;

    // ![150x150](https://user-images.githubusercontent.com/35730970/198033585-5a7ca09d-94c3-4812-be0e-8077d5da8ceb.png)
    private static final Pattern pImage = Pattern.compile("!\\[.*]\\((.*)\\)");

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        final Pattern p = Pattern.compile("https://github.com/" + targetChannel + "/issues/(\\d+)");

        // webhookでタイトルが特殊なものはスレッドを作る
        if(event.isWebhookMessage() && event.getMessage().getEmbeds().size() > 0){
            var message = event.getMessage().getEmbeds().get(0);

            var m = p.matcher(Objects.requireNonNullElse(message.getUrl(),"-"));
            if(m.find()){
                var issueId = Integer.parseInt(m.group(1));
                try {
                    var repository = gitHub.getRepository(targetChannel);
                    var targetIssue = repository.getIssue(issueId);


                    var desc = Objects.requireNonNullElse(message.getDescription(),"-");
                    var imageMatcher = pImage.matcher(desc);

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.addField("url",Objects.requireNonNullElse(message.getUrl(),"-"),false);

                    if(imageMatcher.find()){
                        builder.setImage(imageMatcher.group(1));
                    }

                    desc = imageMatcher.replaceAll("$1");
                    builder.appendDescription(desc);

                    var auther = "?";
                    if(message.getAuthor() != null){
                        auther = message.getAuthor().getName();
                    }

                    String tag = null;
                    if(desc.contains("問題の固有名詞")){
                        tag = "固有名詞";
                    } else if (desc.contains("問題の用語")){
                        tag = "ゲームシステム用語";
                    } else if (desc.contains("問題のあるインターフェイス")){
                        tag = "インターフェイス";
                    } else if (desc.contains("問題のイベントタイトル")){
                        tag = "イベント等テキスト";
                    } else if (desc.contains("問題のツールチップ")){
                        tag = "ツールチップ";
                    } else if (desc.contains("問題の固有名詞と希望する変更")){
                        tag = "固有名詞";
                    }

                    if(tag != null){
                        targetIssue.setLabels(tag);
                    }

                    final String finalTag = tag;

                    var forum = event.getGuild().getForumChannelsByName("issues",false);
                    var discordTag = forum.get(0).getAvailableTags().stream().filter(x->x.getName().equals(finalTag)).findAny();

                    var title = Objects.requireNonNullElse(message.getTitle(),"No Title");
                    var simpleTitle = title.replaceAll("^\\[" + targetChannel + "] Issue opened: #\\d+\s+","");
                    var simpleTitleByName = String.format("%s by %s",simpleTitle, auther);

                    var forumPostAction = forum.get(0).createForumPost(simpleTitleByName, MessageCreateData.fromEmbeds(builder.build()));
                    discordTag.ifPresent(x->{
                        forumPostAction.setTags(ForumTagSnowflake.fromId(x.getId()));
                    });
                    
                    targetIssue.comment("[自動応答] 内部検討中です。進捗がありましたら追記いたします。追加のコメントが有りましたら下記にコメントの形で続けるようにお願い致します。");

                } catch (IOException e){
                    throw new IllegalStateException(e);
                }
            }
        }

        // botの投稿は無視する
        if (event.getAuthor().isBot()) {
            return;
        }

        var optionalProject = projects
                .stream()
                .filter(x -> x.parseRequest(event.getMessage().getContentRaw(),
                                            event))
                .findAny();

        if (optionalProject.isEmpty()) {
            return;
        }

        optionalProject.get().execute();

        MDC.clear();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        String guildId = event.getGuild().getId();
        String emoName = event.getReaction().getEmoji().getName();
        int emoCnt = 0;
        try {
            emoCnt = event.getReaction().getCount();
        } catch (IllegalStateException e){
            /* 初めての場合はExceptionになる */
        }

        if(emoName.equals("kihyou") && emoCnt == 0 && guildId.equals("1022767005662724096")){
            log.debug("kihyou");

            GHRepository repo = null;
            try {
                repo = gitHub.getRepository(targetChannel);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Message message = event.getChannel()
                    .retrieveMessageById(event.getMessageId())
                    .complete();

            var messageBody = message.getContentDisplay();
            var messageAuther = message.getAuthor().getName();
            var messageUrl = message.getJumpUrl();

            try {
                repo.createIssue(StringUtils.truncate(message.getContentDisplay(),0,15))
                        .body(String.format("""
                        発言者:%s
                        内容:%s
                        URL:%s
                        """,messageAuther,messageBody,messageUrl))
                        .create();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



        MDC.clear();
    }
}
