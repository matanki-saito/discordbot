package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GithubIssueCommentEditedWebhookResponse implements GitHubIssueCommentWebhookResponse {
    private String action = "edited";
    private GithubCommonRepositoryWebhookResponse repository;
    private GitHubIssueCommentCreatedWebhookResponse.Comment comment;
    private GithubCommonIssueWebhookResponse issue;
    private Changes changes;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class Changes {
        private Body body;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @NoArgsConstructor
        @Data
        public static class Body {
            private String from;
        }

    }
}
