package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GitHubIssueCommentDeletedWebhookResponse implements GitHubIssueCommentWebhookResponse {
    private String action = "deleted";
    private GithubCommonRepositoryWebhookResponse repository;
    private GitHubIssueCommentCreatedWebhookResponse.Comment comment;
    private GithubCommonIssueWebhookResponse issue;
    private GithubCommonUserWebhookResponse sender;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class Comment {
        @JsonProperty("author_association")
        private String authorAssociation;
        private String body;
        @JsonProperty("created_at")
        private String createdAt;
        private String url;
    }
}
