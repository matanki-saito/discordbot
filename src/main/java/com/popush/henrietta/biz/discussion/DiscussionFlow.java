package com.popush.henrietta.biz.discussion;

import com.popush.henrietta.github.GitHubIssueCommentCreatedWebhookResponse;
import com.popush.henrietta.github.GithubIssuesClosedWebhookResponse;
import com.popush.henrietta.github.GithubIssuesOpenedWebhookResponse;
import com.popush.henrietta.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscussionFlow {
    @Value("${discord.target-channel}")
    private String targetGithubRepository;

    @Value("${discord.target-guild-id}")
    private String targetDiscordGuildId;

    @Value("#{${discord.detection-users}}")
    private List<String> detectionUsers;

    private final GitHub gitHub;

    private static final Pattern forumIssueTagPattern = Pattern.compile("^\\[(\\d+)]\s(.*)");

    private static final String confirmMessage = "[自動応答] 内部検討中です。";

    public void internalStartIssue(
            String githubIssueTitle, // 今日の献立について
            Integer githubIssueNumber, // 35
            String githubRepository, // matanki-saito/ck3jpmod
            String githubIssueAuther, // matanki-saito
            String githubIssueBody, // ## オムライスの作り方 ...
            String githubIssueUrl,
            ForumChannel discordForumChannel) throws IOException {

        if (detectionUsers.contains(githubIssueAuther)) {
            return;
        }

        var repository = gitHub.getRepository(githubRepository);

        var targetGithubIssue = repository.getIssue(githubIssueNumber);

        // discordに１回で投稿できるのが最大2000文字なのでそれ以上になる場合は分割投稿する
        var githubIssueBodies = Stream.concat(TextUtils
                        .splitMessage(githubIssueBody, 1900, "###")
                        .stream(), Stream.of(githubIssueUrl))
                .toList();

        var discordMessageBuilder = new MessageCreateBuilder();
        discordMessageBuilder.addContent(githubIssueBodies.get(0));

        // discordからgithubに辿れるようにタイトルの先頭に[ISSUE番号]を入れるのが仕様
        var forumTitle = String.format("[%d] %s by %s", githubIssueNumber, githubIssueTitle, githubIssueAuther);

        var discordForumPostAction = discordForumChannel.createForumPost(forumTitle, discordMessageBuilder.build());

        // タグをつける
        // コラボレータでないとラベルが付けられない。とりあえず文章で判断する。
        var githubIssueLabel = Optional.of(githubIssueBody).map(x -> {
            if (x.contains("問題の固有名詞")) {
                return "固有名詞";
            } else if (x.contains("問題の用語")) {
                return "ゲームシステム用語";
            } else if (x.contains("問題のあるインターフェイス")) {
                return "インターフェイス";
            } else if (x.contains("問題のイベントタイトル")) {
                return "イベント等テキスト";
            } else if (x.contains("問題のツールチップ")) {
                return "ツールチップ";
            } else if (x.contains("問題の固有名詞：希望する変更")) {
                return "固有名詞";
            }
            return null;
        });

        if (githubIssueLabel.isPresent()) {
            // github issueにつける
            targetGithubIssue.setLabels(githubIssueLabel.get());
            // forumからも同じ名前のタグを探してきて付ける
            discordForumChannel
                    .getAvailableTags()
                    .stream()
                    .filter(x -> x.getName().equals(githubIssueLabel.get()))
                    .findAny()
                    .ifPresent(x -> {
                        discordForumPostAction.setTags(ForumTagSnowflake.fromId(x.getId()));
                    });
        }

        // discord forumに投稿
        var discordPostResponse = discordForumPostAction.complete();

        // 複数のメッセージがある場合は続けて投稿する
        if (githubIssueBodies.size() > 1) {
            githubIssueBodies.subList(1, githubIssueBodies.size()).forEach(x -> {
                MessageCreateBuilder nextCreateBuilder = new MessageCreateBuilder();
                nextCreateBuilder.addContent(x);
                discordPostResponse.getThreadChannel().sendMessage(nextCreateBuilder.build()).complete();
            });
        }

        // 自動応答であることを投稿する。WebHookはたまに実行されないので、この投稿の有無で実際に処理が行われたかを確認する
        targetGithubIssue.comment("""
                %s 進捗がありましたら追記いたします。
                追加のコメントが有りましたら下記にコメントの形で続けるようにお願い致します。
                """.formatted(confirmMessage));
    }

    public void messageReactionAdd(MessageReactionAddEvent event) {

        var guildId = event.getGuild().getId();
        var emoName = event.getReaction().getEmoji().getName();
        var emoCnt = 0;
        try {
            emoCnt = event.getReaction().getCount();
        } catch (IllegalStateException e) {
            /* 初めての場合はExceptionになる */
        }

        if (!guildId.equals(targetDiscordGuildId) || emoCnt != 0 || !List.of("kihyou", "commit").contains(emoName)) {
            return;
        }

        GHRepository githubRepository;
        try {
            githubRepository = gitHub.getRepository(targetGithubRepository);
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
        if (List.of(ChannelType.GUILD_PRIVATE_THREAD, ChannelType.GUILD_PUBLIC_THREAD)
                .contains(forumThreadComment.getChannel().getType())) {
            var forumThreadTitle = forumThreadComment.getChannel().asThreadChannel().getName();
            forumIssueTagMatcher = forumIssueTagPattern.matcher(forumThreadTitle);
        }

        try {
            // リアクションがついたコメントの内容をもとに新規のgithub issueを作る
            if (emoName.equals("kihyou")) {
                githubRepository.createIssue(StringUtils.truncate(forumThreadComment.getContentDisplay(), 0, 15))
                        .body(String.format("""
                                発言者:%s
                                内容:%s
                                URL:%s
                                """, forumThreadMessageAuther, forumThreadMessageBody, forumThreadMessageUrl))
                        .create();
            }
            // リアクションがついためコメントの内容をgithub issueに戻す
            else if (emoName.equals("commit") && forumIssueTagMatcher != null && forumIssueTagMatcher.find()) {
                var githubIssueId = Integer.parseInt(forumIssueTagMatcher.group(1));
                githubRepository.getIssue(githubIssueId).comment(forumThreadMessageBody);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<GHIssueComment> tryGetComments(GHIssue ghIssue) {
        try {
            return ghIssue.getComments();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String tryGetUserLogin(GHIssueComment ghIssueComment) {
        try {
            return ghIssueComment.getUser().getLogin();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ForumChannel getDefaultForumChannel(JDA jda) {
        return Optional
                .ofNullable(jda.getGuildById(targetDiscordGuildId))
                .map(x -> x.getForumChannelsByName("issues", true))
                .stream()
                .findFirst()
                .orElseThrow()
                .get(0);
    }

    public void checkPost(JDA jda) throws IOException {
        var repo = gitHub.getRepository(targetGithubRepository);
        var lostIssues = repo
                .getIssues(GHIssueState.OPEN)
                .stream()
                .filter(x -> tryGetComments(x).stream().noneMatch(c ->
                        tryGetUserLogin(c).equals("matanki-saito") && c.getBody().contains(confirmMessage)))
                .toList();

        lostIssues.forEach(issue -> {
            try {
                internalStartIssue(issue.getTitle(),
                        issue.getNumber(),
                        issue.getRepository().getFullName(),
                        issue.getUser().getLogin(),
                        issue.getBody(),
                        issue.getHtmlUrl().toString(),
                        getDefaultForumChannel(jda));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public String startIssueFromWebhook(GithubIssuesOpenedWebhookResponse response, JDA jda) throws IOException {
        internalStartIssue(response.getIssue().getTitle(),
                response.getIssue().getNumber(),
                response.getRepository().getFullName(),
                response.getSender().getLogin(),
                response.getIssue().getBody(),
                response.getIssue().getHtmlUrl(),
                getDefaultForumChannel(jda));

        return "success";
    }

    public String endIssueFromWebhook(GithubIssuesClosedWebhookResponse response, JDA jda) {
        getDefaultForumChannel(jda)
                .getThreadChannels()
                .stream()
                .filter(x -> {
                    var m = forumIssueTagPattern.matcher(x.getName());
                    return m.find() && m.group(1).equals(response.getIssue().getNumber() + "");
                })
                .findAny()
                .ifPresent(x -> {
                    x.getManager().setArchived(true).complete();
                });
        return "success";
    }

    public String commitIssueMessageFromWebhook(GitHubIssueCommentCreatedWebhookResponse response, JDA jda) {
        getDefaultForumChannel(jda).getThreadChannels()
                .stream()
                .filter(x -> {
                    var m = forumIssueTagPattern.matcher(x.getName());
                    return m.find() && m.group(1).equals(response.getIssue().getNumber() + "");
                })
                .findAny()
                .ifPresent(x -> {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.addField("auther", response.getSender().getLogin(), false);
                    builder.setDescription(response.getComment().getBody());
                    x.sendMessageEmbeds(builder.build()).complete();
                });
        return "success";
    }

}
