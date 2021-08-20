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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popush.henrietta.discord.model.BotCallCommand;
import com.popush.henrietta.discord.states.ParatranzAggregationReport;
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

    public EsResponseContainer<ParatranzAggregationReport> aggReport(@Nonnull BotCallCommand botCallCommand) {
        var boolQuery = QueryBuilders.boolQuery();

        var aggBuilders = AggregationBuilders
                .terms("game")
                .field("pz_pj_code")
                .size(20)
                .subAggregation(AggregationBuilders.range("size_original")
                                                   .field("size_original")
                                                   .addRange(0.0, 5.0)
                                                   .addRange(5.0, 10.0)
                                                   .addRange(10.0, 50.0)
                                                   .addRange(50.0, 200.0)
                                                   .addRange(200.0, 10000.0)
                                                   .subAggregation(AggregationBuilders.range("size_translation")
                                                                                      .field("size_translation")
                                                                                      .addRange(0.0, 1)
                                                                                      .addRange(1.0, 10000.0)));

        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource().size(0);
        searchBuilder.aggregation(aggBuilders);
        SearchRequest request = new SearchRequest(botCallCommand.getIndex()).source(searchBuilder);

        List<EsResponseWithData<ParatranzAggregationReport>> results = new ArrayList<>();

        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            Terms game = response.getAggregations().get("game");
            for (Terms.Bucket gameBucket : game.getBuckets()) {
                var gameId = gameBucket.getKeyAsString();
                var reportBuilder = ParatranzAggregationReport.builder();

                Range sizeOriginal = gameBucket.getAggregations().get("size_original");
                for (Range.Bucket sizeOriginalBucket : sizeOriginal.getBuckets()) {
                    var itemBuilder = ParatranzAggregationReport.PercentItem.builder();

                    itemBuilder.lengthBegin(((Double)sizeOriginalBucket.getFrom()).longValue());
                    itemBuilder.lengthEnd(((Double)sizeOriginalBucket.getTo()).longValue());
                    itemBuilder.allCount(sizeOriginalBucket.getDocCount());

                    Range sizeTranslation = sizeOriginalBucket.getAggregations().get("size_translation");
                    var a = sizeTranslation.getBuckets().get(0);
                    var b = sizeTranslation.getBuckets().get(1);
                    itemBuilder.translatedItemCount(b.getDocCount());
                    itemBuilder.translatedItemPercent(
                            (double) b.getDocCount() / (a.getDocCount() + b.getDocCount()));

                    reportBuilder.percentItem(itemBuilder.build());
                }

                results.add(new EsResponseWithData<>(
                        gameId,
                        reportBuilder.build()
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        return EsResponseContainer.<ParatranzAggregationReport>builder()
                                  .findCount(1L)
                                  .callCommand(botCallCommand)
                                  .data(results)
                                  .build();
    }

    public EsResponseContainer<ParatranzEntry> searchTerm(@Nonnull BotCallCommand botCallCommand,
                                                          int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        botCallCommand.getSearchWords().forEach(x -> {
            var queryBuilder = QueryBuilders.boolQuery();

            var isCommandSearch = false;

            if (botCallCommand.getCommands().contains("o")) {
                isCommandSearch = true;
                queryBuilder.should(QueryBuilders.matchPhraseQuery("original", x));
            }

            if (botCallCommand.getCommands().contains("k")) {
                isCommandSearch = true;
                queryBuilder.should(QueryBuilders.matchPhraseQuery("key", x));
            }

            if (botCallCommand.getCommands().contains("t")) {
                isCommandSearch = true;
                queryBuilder.should(QueryBuilders.matchPhraseQuery("translation", x));
            }

            if (!isCommandSearch) {
                queryBuilder.should(QueryBuilders.matchPhraseQuery("original", x))
                            .should(QueryBuilders.matchPhraseQuery("key", x))
                            .should(QueryBuilders.matchPhraseQuery("translation", x));
            }

            queryBuilder.minimumShouldMatch(1);
            boolQuery.must(queryBuilder);
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
