package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
public class GithubCommonIssueWebhookResponse {
    private Long id;
    private Integer number;
    private String title;
    private String url;
    @JsonProperty("html_url")
    private String htmlUrl;
    private String body;
}
