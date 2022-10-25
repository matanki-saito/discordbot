package com.popush.henrietta.discord.config;

import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.PaginatorBuilder;
import com.popush.henrietta.discord.BotListener;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BotDiscordConfig {
    private final BotListener bot;

    @Value("${discord.token}")
    private String discordToken;

    @Value("${discord.name}")
    private String name;

    @Bean
    public JDA beanJda() throws InterruptedException {
        for (var i = 0; i < 20; i++) {
            try {
                var jda = JDABuilder.createDefault(discordToken)
                        .enableIntents(GatewayIntent.DIRECT_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.DIRECT_MESSAGE_REACTIONS)
                        .addEventListeners(bot)
                                    .setActivity(Activity.playing(name))
                                    .build();

                jda.awaitReady();

                Pages.activate(PaginatorBuilder
                                       .createPaginator()
                                       .setHandler(jda)
                                       .shouldRemoveOnReact(true)
                                       .build());

                return jda;

            } catch (Exception e) {
                // DNSの問題かIPv6の問題かmicrok8sの問題か分からないが、コンテナ起動時にネットワークが切れていることがある。
                // このように接続する施行を入れないとbeanができなくて繋がらないまま起動し続けてしまう。
                Thread.sleep(5000);
            }
        }
        throw new IllegalStateException();
    }
}
