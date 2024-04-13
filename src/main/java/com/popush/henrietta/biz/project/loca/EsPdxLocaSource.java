package com.popush.henrietta.biz.project.loca;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.github.matanki_saito.rico.exception.ArgumentException;
import com.github.matanki_saito.rico.exception.SystemException;
import com.github.matanki_saito.rico.loca.PdxLocaFilter;
import com.github.matanki_saito.rico.loca.PdxLocaSource;
import com.popush.henrietta.biz.project.states.ParatranzEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EsPdxLocaSource implements PdxLocaSource {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public PdxLocaYamlRecord get(String key, PdxLocaFilter filter) {

        GetResponse<ParatranzEntry> response;
        try {
            response = elasticsearchClient.get(g -> g
                            .index(String.join(",", filter.getIndecies()))
                            .id(key),
                    ParatranzEntry.class
            );
        } catch (IOException e) {
            throw new RuntimeException("SystemException", e);
        }
        var src = Objects.requireNonNullElse(response.source(), new ParatranzEntry());

        return new PdxLocaYamlRecord(src.getKey(), src.getStage(), src.getTranslation(),
                "", 0, src.getFile(), response.index());
    }

    @Override
    public List<String> getKeys(PdxLocaFilter filter) throws ArgumentException, SystemException {
        return List.of();
    }

    @Override
    public boolean exists(String key, PdxLocaFilter filter) {
        try {
            return elasticsearchClient.exists(ExistsRequest.of(
                    g -> g.index(String.join(",", filter.getIndecies())).id(key))
            ).value();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
