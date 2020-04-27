package com.popush.henrietta.elasticsearch.config;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {
        ElasticSearchConfig.class,
        ElasticsearchService.class
})
@ExtendWith(SpringExtension.class)
@ExtendWith(SoftAssertionsExtension.class)
class ElasticsearchServiceTest {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Test
    void search(SoftAssertions softly) {
        var result = elasticsearchService.search("猫");
        softly.assertThat(result).hasSize(4);
    }
}
