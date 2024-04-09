package com.popush.henrietta.biz.project.loca;

import com.github.matanki_saito.rico.exception.ArgumentException;
import com.github.matanki_saito.rico.exception.MachineException;
import com.github.matanki_saito.rico.loca.PdxLocaMatchPattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocaConfig {
    @Bean
    public PdxLocaMatchPattern pdxLocaMatchPattern() throws MachineException, ArgumentException {
        return new PdxLocaMatchPattern();
    }
}
