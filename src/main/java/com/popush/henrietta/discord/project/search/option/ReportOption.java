package com.popush.henrietta.discord.project.search.option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.popush.henrietta.discord.exception.CommandErrorException;
import com.popush.henrietta.discord.service.SendMessageService;
import com.popush.henrietta.discord.states.ParatranzAggregationReport;
import com.popush.henrietta.elasticsearch.model.EsResponseWithData;
import com.popush.henrietta.elasticsearch.service.ElasticsearchService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor
@Builder(toBuilder = true)
public class ReportOption implements SearchOption {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper elasticObjectMapper;
    private final SendMessageService sendMessageService;
    private final ElasticsearchService elasticsearchService;

    private MessageReceivedEvent context;
    private String request;

    @Override
    public boolean matchOption(String optionText) {
        return optionText.contains("r");
    }

    @Override
    public SearchOption makeClone(String request, List<String> saveArguments, MessageReceivedEvent context) {
        return this.toBuilder()
                   .request(request)
                   .context(context)
                   .build();
    }

    @Override
    public void parseArguments() throws CommandErrorException {
        //
    }

    @Override
    public void execute() {

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
        SearchRequest request = new SearchRequest(this.request).source(searchBuilder);

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

                    itemBuilder.lengthBegin(((Double) sizeOriginalBucket.getFrom()).longValue());
                    itemBuilder.lengthEnd(((Double) sizeOriginalBucket.getTo()).longValue());
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

        final ArrayList<Page> pages = new ArrayList<>();

        for (var idx = 0; idx < results.size(); idx++) {
            final EmbedBuilder builder = new EmbedBuilder();
            var withData = results.get(idx);
            var data = withData.getData();

            for (var item : data.getPercentItems()) {
                builder.addField("%d文字 ~ %d文字".formatted(item.getLengthBegin(), item.getLengthEnd()),
                                 "完了率：%.2f%%(翻訳済み：%d個/全体：%d個)".formatted(item.getTranslatedItemPercent() * 100,
                                                                         item.getTranslatedItemCount(),
                                                                         item.getAllCount()),
                                 false);
            }
            pages.add(new Page(builder.build()));
        }

        if (pages.size() == 1) {
            context.getChannel().sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
            context.getChannel().sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> {
                Pages.paginate(success, pages, false, 60, TimeUnit.SECONDS);
            }, res -> log.error(res.getMessage()));
        }
    }
}
