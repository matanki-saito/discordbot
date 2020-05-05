package com.popush.henrietta.elasticsearch.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popush.henrietta.discord.model.BotCallCommand;
import com.popush.henrietta.discord.states.ParatranzEntry;
import com.popush.henrietta.elasticsearch.model.EsResponseWithData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper elasticObjectMapper;

    public List<EsResponseWithData<ParatranzEntry>> searchTerm(@Nonnull BotCallCommand botCallCommand) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        botCallCommand.getSearchWords().forEach(x -> {
            var a = QueryBuilders.boolQuery()
                                 .should(QueryBuilders.matchPhraseQuery("translation", x))
                                 .should(QueryBuilders.matchPhraseQuery("key", x))
                                 .should(QueryBuilders.matchPhraseQuery("original", x))
                                 .minimumShouldMatch(1);
            boolQuery.must(a);
        });

        return search(boolQuery, botCallCommand);
    }

    public List<EsResponseWithData<ParatranzEntry>> searchPartialMatch(@Nonnull BotCallCommand botCallCommand) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        botCallCommand.getSearchWords().forEach(x -> {
            var a = QueryBuilders.boolQuery()
                                 .should(QueryBuilders.regexpQuery("translation", String.format(".*%s.*", x)))
                                 .should(QueryBuilders.regexpQuery("key", String.format(".*%s.*", x)))
                                 .should(QueryBuilders.regexpQuery("original", String.format(".*%s.*", x)))
                                 .minimumShouldMatch(1);
            boolQuery.must(a);
        });

        return search(boolQuery, botCallCommand);
    }

    private List<EsResponseWithData<ParatranzEntry>> search(BoolQueryBuilder boolQuery,
                                                            BotCallCommand botCallCommand) {

        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();
        searchBuilder.query(boolQuery);
        SearchRequest request = new SearchRequest(botCallCommand.getIndex()).source(searchBuilder);

        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            List<EsResponseWithData<ParatranzEntry>> results = new ArrayList<>();

            for (SearchHit hit : response.getHits().getHits()) {
                var data = new EsResponseWithData<>(
                        botCallCommand,
                        hit.getId(),
                        elasticObjectMapper.readValue(hit.getSourceAsString(), ParatranzEntry.class));

                results.add(data);
            }
            return results;
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

}
