package com.popush.henrietta.github;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GithubIssuesOpenedWebhookResponse implements GithubIssuesWebhookResponse {
    private String action = "opened";
    private GithubCommonIssueWebhookResponse issue;
    private GithubCommonRepositoryWebhookResponse repository;
    private GithubCommonUserWebhookResponse sender;
}
