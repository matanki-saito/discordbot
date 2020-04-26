package com.popush.henrietta.elasticsearch.config;

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
class ElasticsearchServiceTest {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Test
    void search() {
        elasticsearchService.search();
    }
}
