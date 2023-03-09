package com.popush.henrietta.deepl;

import com.deepl.api.Translator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DeeplConfig {

    @Value("${deepl.token}")
    private String deeplToken;

    @Bean
    public Translator beanTranslator() throws InterruptedException {
        return new Translator(deeplToken);
    }
}
