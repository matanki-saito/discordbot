package com.popush.henrietta.elasticsearch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popush.henrietta.discord.states.EsResponseWithData;
import com.popush.henrietta.discord.states.ParatranzEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final RestHighLevelClient restHighLevelClient;

    public List<EsResponseWithData<ParatranzEntry>> search() {
        var objectMapper = new ObjectMapper();

        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        QueryBuilder query1 = QueryBuilders.matchQuery("translation", "猫");
        boolQuery.filter(query1);

        searchBuilder.query(boolQuery);

        SearchRequest request = new SearchRequest("eu4").source(searchBuilder);

        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            List<EsResponseWithData<ParatranzEntry>> results = new ArrayList<>();

            for (SearchHit hit : response.getHits().getHits()) {
                var data = new EsResponseWithData<>(
                        hit.getId(),
                        objectMapper.readValue(hit.getSourceAsString(), ParatranzEntry.class));

                results.add(data);
            }
            return results;
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }
}
