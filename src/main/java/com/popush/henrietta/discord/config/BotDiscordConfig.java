package com.popush.henrietta.discord.config;

import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.PaginatorBuilder;
import com.popush.henrietta.discord.Bot;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BotDiscordConfig {
    private final Bot bot;

    @Value("${discord.token}")
    private String discordToken;

    @Value("${discord.name}")
    private String name;

    @Bean
    public JDA beanJda() throws LoginException, InterruptedException, InvalidHandlerException {
        Thread.sleep(5000);

        var jda = JDABuilder.createDefault(discordToken)
                            .addEventListeners(bot)
                            .setActivity(Activity.playing(name))
                            .build();

        Pages.activate(PaginatorBuilder
                               .createPaginator()
                               .setHandler(jda)
                               .shouldRemoveOnReact(true)
                               .build());

        return jda;
    }
}
