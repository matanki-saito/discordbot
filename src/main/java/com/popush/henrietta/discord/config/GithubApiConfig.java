package com.popush.henrietta.discord.config;

import lombok.RequiredArgsConstructor;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class GithubApiConfig {

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public GitHub beanGithubApi() throws IOException {
        return new GitHubBuilder()
                .withPassword("matanki-saito",githubToken)
                .build();
    }
}
