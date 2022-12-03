package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private String body;
}
