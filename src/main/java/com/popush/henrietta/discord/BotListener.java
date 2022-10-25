package com.popush.henrietta.discord;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.popush.henrietta.discord.project.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.apache.coyote.http11.Constants.a;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotListener extends ListenerAdapter {
    private final List<Project> projects;

    private final GitHub gitHub;

    private static final Pattern p = Pattern.compile("https://github.com/matanki-saito/vic3jpadvmod/issues/(\\d+)");

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        // webhookでタイトルが特殊なものはスレッドを作る
        if(event.isWebhookMessage()){
            var sourceUrl = Objects.requireNonNull(event.getMessage()
                    .getEmbeds()
                    .get(0)
                    .getUrl(),"-");

            var messageJumpUrl = event.getMessage().getJumpUrl();

            var m = p.matcher(sourceUrl);
            if(m.find()){
                var issueId = Integer.parseInt(m.group(1));
                try {
                    var repository = gitHub.getRepository("matanki-saito/vic3jpadvmod");
                    var targetIssue = repository.getIssue(issueId);
                    targetIssue.comment(String.format("""
                            内部検討中です。議論の進捗についてご確認したい場合はお手数ですがDiscordのアカウントご用意後、下記にアクセスをお願い致します。
                            Discord:%s
                            *アクセス権が必要になりますので #アクセス権申請板 で申請をお願い致します。
                            """,messageJumpUrl));

                    var a = event.getMessage().createThreadChannel("New Thread").complete();

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
                repo = gitHub.getRepository("matanki-saito/vic3jpadvmod");
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
