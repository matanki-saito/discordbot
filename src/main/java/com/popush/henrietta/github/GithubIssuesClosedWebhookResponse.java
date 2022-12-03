package com.popush.henrietta.github;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GithubIssuesClosedWebhookResponse implements GithubIssuesWebhookResponse {
    private String action = "closed";
    private GithubCommonIssueWebhookResponse issue;
    private GithubCommonRepositoryWebhookResponse repository;
    private GithubCommonUserWebhookResponse sender;
}
