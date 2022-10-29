package com.popush.henrietta.discord;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
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

import static java.util.regex.Pattern.compile;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotListener extends ListenerAdapter {
    private final List<Project> projects;

    private final GitHub gitHub;

    @Value("${discord.target-channel}")
    private String targetChannel;

    private static final Pattern markdownImagePattern = compile("!\\[.*]\\((.*)\\)");

    private Pattern githubIssueUrlPattern;
    private Pattern githubNewCommitTitlePattern;
    private Pattern githubOpenIssueTitlePattern;

    @PostConstruct
    void postConstruct(){
        githubIssueUrlPattern = Pattern.compile("https://github.com/" + targetChannel + "/issues/(\\d+)");
        githubOpenIssueTitlePattern = Pattern.compile("^\\[" + targetChannel + "] Issue opened: #(\\d+)\s+(.+)");
        githubNewCommitTitlePattern = Pattern.compile("^\\[" + targetChannel + "] New comment on issue: #(\\d+)\s+(.+)");
    }

    private void issue(MessageEmbed message, ForumChannel forumChannel) throws IOException{
        var githubIssueTitle = Objects.requireNonNullElse(message.getTitle(),"No Title");
        var githubIssueAuther = Optional
                .ofNullable(message.getAuthor())
                .map(MessageEmbed.AuthorInfo::getName)
                .orElse("?");
        var githubIssueBody = Objects.requireNonNullElse(message.getDescription(),"-");
        var githubIssueUrl = Objects.requireNonNullElse(message.getUrl(),"-");

        var repository = gitHub.getRepository(targetChannel);
        var githubOpenMatcher = githubOpenIssueTitlePattern.matcher(githubIssueTitle);
        var githubCommentMatcher = githubNewCommitTitlePattern.matcher(githubIssueTitle);

        var githubIssueLabel = Optional.of(githubIssueBody).map(x->{
            if(x.contains("問題の固有名詞")){
                return "固有名詞";
            } else if (x.contains("問題の用語")){
                return "ゲームシステム用語";
            } else if (x.contains("問題のあるインターフェイス")){
                return "インターフェイス";
            } else if (x.contains("問題のイベントタイトル")){
                return "イベント等テキスト";
            } else if (x.contains("問題のツールチップ")){
                return "ツールチップ";
            } else if (x.contains("問題の固有名詞と希望する変更")){
                return "固有名詞";
            }
            return null;
        });

        if(githubCommentMatcher.find()) {
            var githubIssueId = Integer.parseInt(githubCommentMatcher.group(1));
            var targetIssue = repository.getIssue(githubIssueId);

        } else if(githubOpenMatcher.find()){
            var githubIssueId = Integer.parseInt(githubOpenMatcher.group(1));
            var targetIssue = repository.getIssue(githubIssueId);

            EmbedBuilder builder = new EmbedBuilder();
            builder.addField("Github URL",githubIssueUrl,false);

            // 画像処理
            var markdownImageMatcher = markdownImagePattern.matcher(githubIssueBody);
            if(markdownImageMatcher.find()){
                builder.setImage(markdownImageMatcher.group(1));
            }
            var githubIssueBodyFix = markdownImageMatcher.replaceAll("$1");
            builder.appendDescription(githubIssueBodyFix);

            var forumPostAction = forumChannel
                    .createForumPost(
                            String.format("[%d] %s by %s",githubIssueId, githubOpenMatcher.group(2), githubIssueAuther),
                            MessageCreateData.fromEmbeds(builder.build()));

            // タグ処理
            if(githubIssueLabel.isPresent()){
                // github issueにつける
                targetIssue.setLabels(githubIssueLabel.get());
                // forumにもつける
                var forumTag =forumChannel.getAvailableTags().stream().filter(x->x.getName().equals(githubIssueLabel.get())).findAny();
                forumTag.ifPresent(x->{
                    forumPostAction.setTags(ForumTagSnowflake.fromId(x.getId()));
                });
            }

            // forum投稿
            forumPostAction.complete();
            // github issue投稿
            targetIssue.comment("[自動応答] 内部検討中です。進捗がありましたら追記いたします。追加のコメントが有りましたら下記にコメントの形で続けるようにお願い致します。");

        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        MDC.put("X-Track", UUID.randomUUID().toString());

        // new commit / issue
        if(event.isWebhookMessage() && event.getMessage().getEmbeds().size() > 0){
            var message = event.getMessage().getEmbeds().get(0);
            var m = githubIssueUrlPattern.matcher(Objects.requireNonNullElse(message.getUrl(),"-"));
            var forum = event.getGuild().getForumChannelsByName("issues",false);
            if(m.find() && forum.size() > 0) {
                try {
                    issue(message, forum.get(0));
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
