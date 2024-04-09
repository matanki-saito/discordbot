package com.popush.henrietta.github;

import com.popush.henrietta.biz.discussion.DiscussionFlow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebHookAsyncService {

    private final DiscussionFlow discussionFlow;

    private final JDA jda;

    @Async
    public CompletableFuture<String> makeProcess(
            GithubWebhookResponse response
    ) throws InterruptedException, ArgumentException, OtherSystemException {

        try {
            var result = switch (response) {
                case GitHubIssueCommentCreatedWebhookResponse r -> discussionFlow.commitIssueMessageFromWebhook(r, jda);
                case GithubIssuesOpenedWebhookResponse r -> discussionFlow.startIssueFromWebhook(r, jda);
                case GithubIssuesClosedWebhookResponse r -> discussionFlow.endIssueFromWebhook(r, jda);
                default -> "no match";
            };

            return CompletableFuture.completedFuture(result);
        } catch (IOException e) {
            throw new OtherSystemException(e.getMessage());
        }
    }
}
