package com.popush.henrietta.github;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GithubIssuesNoneWebhookResponse implements GithubIssuesWebhookResponse {
    private String action = "none";
}
