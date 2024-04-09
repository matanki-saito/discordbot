package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "created", value = GitHubIssueCommentCreatedWebhookResponse.class),
        @JsonSubTypes.Type(name = "deleted", value = GitHubIssueCommentDeletedWebhookResponse.class),
        @JsonSubTypes.Type(name = "edited", value = GithubIssueCommentEditedWebhookResponse.class)
})
@JsonIgnoreProperties(ignoreUnknown = true)
interface GitHubIssueCommentWebhookResponse extends GithubWebhookResponse {
}
