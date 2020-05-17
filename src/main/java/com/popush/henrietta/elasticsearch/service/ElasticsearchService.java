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
import com.popush.henrietta.elasticsearch.model.EsResponseContainer;
import com.popush.henrietta.elasticsearch.model.EsResponseWithData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper elasticObjectMapper;

    public EsResponseContainer<ParatranzEntry> searchTerm(@Nonnull BotCallCommand botCallCommand,
                                                          int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        botCallCommand.getSearchWords().forEach(x -> {
            var a = QueryBuilders.boolQuery()
                                 .should(QueryBuilders.matchPhraseQuery("translation", x))
                                 .should(QueryBuilders.matchPhraseQuery("key", x))
                                 .should(QueryBuilders.matchPhraseQuery("original", x))
                                 .minimumShouldMatch(1);
            boolQuery.must(a);
        });

        return search(boolQuery, botCallCommand, size);
    }

    public EsResponseContainer<ParatranzEntry> searchPartialMatch(@Nonnull BotCallCommand botCallCommand,
                                                                  int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        botCallCommand.getSearchWords().forEach(x -> {
            var a = QueryBuilders.boolQuery()
                                 .should(QueryBuilders.wildcardQuery(
                                         "translation.keyword",
                                         String.format("*%s*", x)))
                                 .should(QueryBuilders.wildcardQuery(
                                         "key.keyword",
                                         String.format("*%s*", x)))
                                 .should(QueryBuilders.wildcardQuery(
                                         "original.keyword",
                                         String.format("*%s*", x)))
                                 .minimumShouldMatch(1);
            boolQuery.must(a);
        });

        return search(boolQuery, botCallCommand, size);
    }

    private EsResponseContainer<ParatranzEntry> search(BoolQueryBuilder boolQuery,
                                                       BotCallCommand botCallCommand,
                                                       int size) {

        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource().size(size);
        searchBuilder.query(boolQuery);
        SearchRequest request = new SearchRequest(botCallCommand.getIndex()).source(searchBuilder);

        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            List<EsResponseWithData<ParatranzEntry>> results = new ArrayList<>();

            for (SearchHit hit : response.getHits().getHits()) {
                var data = new EsResponseWithData<>(
                        hit.getId(),
                        elasticObjectMapper.readValue(hit.getSourceAsString(), ParatranzEntry.class));
                results.add(data);
            }
            return EsResponseContainer.<ParatranzEntry>builder()
                    .findCount(response.getHits().getTotalHits().value)
                    .callCommand(botCallCommand)
                    .data(results)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

}
