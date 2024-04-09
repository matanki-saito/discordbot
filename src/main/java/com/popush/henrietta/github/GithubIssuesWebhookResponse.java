package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action", defaultImpl = GithubIssuesNoneWebhookResponse.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "opened", value = GithubIssuesOpenedWebhookResponse.class),
        @JsonSubTypes.Type(name = "closed", value = GithubIssuesClosedWebhookResponse.class)
})
@JsonIgnoreProperties(ignoreUnknown = true)
interface GithubIssuesWebhookResponse extends GithubWebhookResponse {
}
