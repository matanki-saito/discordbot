package com.popush.henrietta.elasticsearch.service;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.popush.henrietta.discord.model.BotCallCommand;
import com.popush.henrietta.elasticsearch.config.ElasticSearchConfig;

@SpringBootTest(classes = {
        ElasticSearchConfig.class,
        ElasticsearchService.class
})
@ExtendWith(SpringExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
class ElasticsearchServiceTest {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Disabled
    @Test
    void search(SoftAssertions softly) {
        var result = elasticsearchService.searchTerm(BotCallCommand.builder()
                                                                   .searchWords(List.of("猫"))
                                                                   .build(),
                                                     10);
        softly.assertThat(result.getData()).hasSize(4);
    }
}
