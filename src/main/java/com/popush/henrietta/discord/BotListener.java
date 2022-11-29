package com.popush.henrietta.discord;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
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

    @Value("${discord.target-guild-id}")
    private String targetGuildId;

    @Value("#{${discord.detection-users}}")
    private List<String> detectionUsers;

    private static final Pattern markdownImagePattern = compile("!\\[.*]\\((.*)\\)");
    private static final Pattern forumIssueTagPattern = Pattern.compile("^\\[(\\d+)]\s(.*)");

    private Pattern githubIssueUrlPattern;
    private Pattern githubNewCommitTitlePattern;
    private Pattern githubOpenIssueTitlePattern;
    private Pattern githubCloseIssueTitlePattern;

    @PostConstruct
    void postConstruct(){
        githubIssueUrlPattern = Pattern.compile("https://github.com/" + targetChannel + "/issues/(\\d+)");
        githubOpenIssueTitlePattern = Pattern.compile("^\\[" + targetChannel + "] Issue opened: #(\\d+)\s+(.+)");
        githubNewCommitTitlePattern = Pattern.compile("^\\[" + targetChannel + "] New comment on issue #(\\d+):\s+(.+)");
        githubCloseIssueTitlePattern = Pattern.compile("^\\[" + targetChannel + "] Issue closed: #(\\d+)\s+(.+)");

    }

    private void issue(MessageEmbed message, ForumChannel forumChannel) throws IOException{
        var githubIssueTitle = Objects.requireNonNullElse(message.getTitle(),"No Title");
        var githubIssueAuther = Optional
                .ofNullable(message.getAuthor())
                .map(MessageEmbed.AuthorInfo::getName)
                .orElse("?");
        var githubIssueBody = Objects.requireNonNullElse(message.getDescription(),"-");
        var markdownImageMatcher = markdownImagePattern.matcher(githubIssueBody);
        var githubIssueBodyFix = markdownImageMatcher.replaceAll("$1");

        var githubIssueUrl = Objects.requireNonNullElse(message.getUrl(),"-");
        var repository = gitHub.getRepository(targetChannel);
        var githubOpenMatcher = githubOpenIssueTitlePattern.matcher(githubIssueTitle);
        var githubCloseMatcher = githubCloseIssueTitlePattern.matcher(githubIssueTitle);
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
            } else if (x.contains("問題の固有名詞：希望する変更")){
                return "固有名詞";
            }
            return null;
        });

        if(githubCommentMatcher.find()){
            var githubIssueId = Integer.parseInt(githubCommentMatcher.group(1));
            //var targetIssue = repository.getIssue(githubIssueId);
            forumChannel.getThreadChannels()
                    .stream()
                    .filter(x->{
                        var m = forumIssueTagPattern.matcher(x.getName());
                        return m.find() && m.group(1).equals(githubIssueId+"");
                    })
                    .findAny()
                    .ifPresent(x->{
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.addField("auther",githubIssueAuther,false);
                        builder.setDescription(githubIssueBodyFix);
                        x.sendMessageEmbeds(builder.build()).complete();
                    });
        } else if(githubCloseMatcher.find()) {
            var githubIssueId = Integer.parseInt(githubCloseMatcher.group(1));
            forumChannel.getThreadChannels()
                    .stream()
                    .filter(x->{
                        var m = forumIssueTagPattern.matcher(x.getName());
                        return m.find() && m.group(1).equals(githubIssueId+"");
                    })
                    .findAny()
                    .ifPresent(x->{
                        x.getManager().setArchived(true).complete();
                    });
        } else if(githubOpenMatcher.find()){
            var githubIssueId = Integer.parseInt(githubOpenMatcher.group(1));
            var targetIssue = repository.getIssue(githubIssueId);

            MessageCreateBuilder createBuilder = new MessageCreateBuilder();
            createBuilder.addContent(githubIssueBodyFix);

            var forumPostAction = forumChannel
                    .createForumPost(
                            String.format("[%d] %s by %s",githubIssueId, githubOpenMatcher.group(2), githubIssueAuther),
                            createBuilder.build());

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

            if(message.getAuthor() != null && detectionUsers.contains(message.getAuthor().getName())){
                return;
            }

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

        var guildId = event.getGuild().getId();
        var emoName = event.getReaction().getEmoji().getName();
        var emoCnt = 0;
        try {
            emoCnt = event.getReaction().getCount();
        } catch (IllegalStateException e){
            /* 初めての場合はExceptionになる */
        }

        if(!guildId.equals(targetGuildId) || emoCnt != 0 || !List.of("kihyou","commit").contains(emoName)){
            return;
        }

        GHRepository githubRepository = null;
        try {
            githubRepository = gitHub.getRepository(targetChannel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var forumThreadComment = event.getChannel()
                .retrieveMessageById(event.getMessageId())
                .complete();

        var forumThreadMessageBody = forumThreadComment.getContentDisplay();
        var forumThreadMessageAuther = forumThreadComment.getAuthor().getName();
        var forumThreadMessageUrl = forumThreadComment.getJumpUrl();

        Matcher forumIssueTagMatcher = null;
        if(List.of(ChannelType.GUILD_PRIVATE_THREAD,ChannelType.GUILD_PUBLIC_THREAD)
                .contains(forumThreadComment.getChannel().getType())) {
            var forumThreadTitle = forumThreadComment.getChannel().asThreadChannel().getName();
            forumIssueTagMatcher = forumIssueTagPattern.matcher(forumThreadTitle);
        }

        try {
            // リアクションがついたコメントの内容をもとに新規のgithub issueを作る
            if(emoName.equals("kihyou")){
                    githubRepository.createIssue(StringUtils.truncate(forumThreadComment.getContentDisplay(),0,15))
                            .body(String.format("""
                            発言者:%s
                            内容:%s
                            URL:%s
                            """,forumThreadMessageAuther,forumThreadMessageBody,forumThreadMessageUrl))
                            .create();
            }
            // リアクションがついためコメントの内容をgithub issueに戻す
            else if(emoName.equals("commit") && forumIssueTagMatcher != null && forumIssueTagMatcher.find()){
                var githubIssueId = Integer.parseInt(forumIssueTagMatcher.group(1));
                githubRepository.getIssue(githubIssueId).comment(forumThreadMessageBody);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MDC.clear();
    }
}
