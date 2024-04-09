package com.popush.henrietta.biz.project.search;

import com.deepl.api.DeepLException;
import com.deepl.api.Translator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matanki_saito.rico.exception.ArgumentException;
import com.github.matanki_saito.rico.exception.SystemException;
import com.github.matanki_saito.rico.loca.PdxLocaMatchPattern;
import com.github.matanki_saito.rico.loca.PdxLocaSource;
import com.github.matanki_saito.rico.loca.PdxLocaYmlTool;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.popush.henrietta.biz.project.Project;
import com.popush.henrietta.biz.project.states.ParatranzAggregationReport;
import com.popush.henrietta.biz.project.states.ParatranzEntry;
import com.popush.henrietta.elasticsearch.service.EsPdxLocaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
@Scope("prototype")
public class SearchProject implements Project {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper elasticObjectMapper;
    private final EsPdxLocaSource esPdxLocaSource;
    private final PdxLocaMatchPattern pdxLocaMatchPattern;

    private final Translator deeplTranslator;

    private static final Pattern p = Pattern.compile("^([a-zA-Z\\d]+)::([a-zA-Z\\d]*)[\s　]*(.*)");

    private String type;
    private String opecode;
    private List<String> searchWords;

    private MessageChannel messageChannel;

    @Override
    public boolean parseRequest(String request, MessageReceivedEvent event) {
        var target = request.toLowerCase(Locale.ROOT);

        var matcher = p.matcher(target);

        if (!matcher.find()) {
            return false;
        }

        type = matcher.group(1);
        opecode = matcher.group(2);
        searchWords = List.of(matcher.group(3).split("[ 　]"));
        messageChannel = event.getChannel();

        return true;
    }

    @Override
    public void execute() {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Match selection
        if (opecode.contains("a")) {
            if (opecode.contains("b")) {
                searchWords.forEach(x -> {
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
            } else {
                searchWords.forEach(x -> {
                    var a = QueryBuilders.boolQuery()
                            .should(QueryBuilders.matchPhraseQuery("translation", x))
                            .should(QueryBuilders.matchPhraseQuery("key", x))
                            .should(QueryBuilders.matchPhraseQuery("original", x))
                            .minimumShouldMatch(1);
                    boolQuery.must(a);
                });
            }
        } else {
            // source selection
            searchWords.forEach(s -> {
                var isCommandSearch = false;
                var queryBuilder = QueryBuilders.boolQuery();
                if (opecode.contains("o")) {
                    isCommandSearch = true;
                    queryBuilder.should(
                            opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("original.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("original", s)
                    );
                }
                if (opecode.contains("k")) {
                    isCommandSearch = true;
                    queryBuilder.should(
                            opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("key.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("key", s)
                    );
                }
                if (opecode.contains("t")) {
                    isCommandSearch = true;
                    queryBuilder.should(
                            opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("translation.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("translation", s)
                    );
                }
                if (!isCommandSearch) {
                    queryBuilder.should(opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("original.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("original", s))
                            .should(opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("key.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("key", s))
                            .should(opecode.contains("b")
                                    ? QueryBuilders.wildcardQuery("translation.keyword", String.format("*%s*", s))
                                    : QueryBuilders.matchPhraseQuery("translation", s));
                }
                queryBuilder.minimumShouldMatch(1);
                boolQuery.should(queryBuilder);
            });
        }

        // search
        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource().size(opecode.contains("r") ? 1 : 10);
        if (opecode.contains("r")) {
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
            searchBuilder.aggregation(aggBuilders);
        }

        if (!String.join("", searchWords).isEmpty()) {
            searchBuilder.query(boolQuery);
        }
        SearchRequest request = new SearchRequest(type).source(searchBuilder);
        SearchResponse response;
        try {
            response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        // out selection
        final ArrayList<Page> pages = new ArrayList<>();
        var results = List.of(response.getHits().getHits());
        if (results.isEmpty()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.appendDescription("見つかりませんでした");
            pages.add(Page.of(builder.build()));

        } else if (opecode.contains("r")) {
            final EmbedBuilder builder = new EmbedBuilder();

            Terms game = response.getAggregations().get("game");
            for (Terms.Bucket gameBucket : game.getBuckets()) {
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

                reportBuilder.build().getPercentItems().forEach(item -> {
                    builder.addField("%d文字 ~ %d文字".formatted(item.getLengthBegin(), item.getLengthEnd()),
                            "完了率：%.2f%%(翻訳済み：%d個/全体：%d個)".formatted(item.getTranslatedItemPercent() * 100,
                                    item.getTranslatedItemCount(),
                                    item.getAllCount()),
                            false);
                });
            }
            pages.add(Page.of(builder.build()));

        } else if (opecode.contains("g")) {
            List<String> result = new ArrayList<>();

            String[] emojiNumber = {"0⃣", "1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣"};

            int idx = 0;
            for (var x : results.subList(0, Math.min(9, results.size()))) {
                ParatranzEntry data;
                try {
                    data = elasticObjectMapper.readValue(x.getSourceAsString(), ParatranzEntry.class);
                } catch (IOException e) {
                    data = new ParatranzEntry();
                }

                result.add(String.format(
                        "%s %s",
                        emojiNumber[idx++],
                        data.getKey()));

                Pattern p = Pattern.compile(".{0,10}("
                                + String.join("|", searchWords)
                                + ").{0,10}",
                        Pattern.CASE_INSENSITIVE);

                var mTrans = p.matcher(data.getTranslation());
                while (mTrans.find()) {
                    result.add(String.format("　　\uD83D\uDCAC %s", mTrans.group()));
                }

                var mKey = p.matcher(data.getKey());
                while (mKey.find()) {
                    result.add(String.format("　　\uD83D\uDCAD %s", mKey.group()));
                }

                var mOriginal = p.matcher(data.getOriginal());
                while (mOriginal.find()) {
                    result.add(String.format("　　\uD83D\uDC41\u200D\uD83D\uDDE8 %s", mOriginal.group()));
                }
            }
            EmbedBuilder builder = new EmbedBuilder();
            builder.appendDescription(String.join("\n", result));
            pages.add(Page.of(builder.build()));
        } else {
            esPdxLocaSource.apply(PdxLocaSource.PdxLocaSourceFilter
                    .builder()
                    .indecies(List.of(type))
                    .build());

            var length = results.size();
            if (opecode.contains("d"))
                length = 3;

            for (var idx = 0; idx < length; idx++) {

                final EmbedBuilder builder = new EmbedBuilder();

                ParatranzEntry data;
                try {
                    data = elasticObjectMapper.readValue(results.get(idx).getSourceAsString(), ParatranzEntry.class);
                } catch (IOException e) {
                    data = new ParatranzEntry();
                }

                final var url = String.format("https://paratranz.cn/projects/%d/strings?key=%s&advanced=1",
                        data.getPzPjCode(),
                        data.getKey());
                final var key = String.format("[%s](%s)", data.getKey(), url);


                var normalizedText = "";
                try {
                    normalizedText = PdxLocaYmlTool.normalize(data.getKey(), esPdxLocaSource, pdxLocaMatchPattern);
                } catch (SystemException | ArgumentException e) {
                    //
                }

                builder.appendDescription(
                        String.format("%d件見つかりました。%d件目を表示します（最大%d件）", results.size(), idx + 1, length));

                builder.addField("key",
                        StringUtils.abbreviate(Optional.ofNullable(key).orElse("不明"),
                                1000),
                        false);

                builder.addField("normalize",
                        StringUtils.abbreviate(Optional.ofNullable(normalizedText).orElse("不明"),
                                1000),
                        false);

                builder.addField("original",
                        StringUtils.abbreviate(Optional.ofNullable(data.getOriginal()).orElse("不明"),
                                1000),
                        false);

                builder.addField("translation",
                        StringUtils.abbreviate(Optional.ofNullable(data.getTranslation()).orElse("不明"),
                                1000),
                        false);

                if (opecode.contains("d")) {
                    try {
                        var text = deeplTranslator.translateText(data.getOriginal(), null, "ja");
                        builder.addField("translation(deepl)",
                                StringUtils.abbreviate(Optional.ofNullable(text.getText()).orElse("-"),
                                        1000),
                                false);
                    } catch (InterruptedException | DeepLException e) {
                        throw new IllegalStateException(e);
                    }
                }

                builder.addField("filePath",
                        StringUtils.abbreviate(Optional.ofNullable(data.getFilePath()).orElse("不明"),
                                1000),
                        false);

                pages.add(Page.of(builder.build()));
            }
        }


        // send
        messageChannel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> {
            Pages.paginate(success, pages,
                    false, 60,
                    TimeUnit.SECONDS);
        }, res -> log.error(res.getMessage()));
    }
}
