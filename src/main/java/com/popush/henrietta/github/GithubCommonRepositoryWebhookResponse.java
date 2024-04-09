package com.popush.henrietta.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
public class GithubCommonRepositoryWebhookResponse {
    private Integer id;
    private String name;
    @JsonProperty("full_name")
    private String fullName;
    private Owner owner;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class Owner {
        private String login;
    }
}
