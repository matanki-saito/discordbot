package com.popush.henrietta.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.github.matanki_saito.rico.exception.ArgumentException;
import com.github.matanki_saito.rico.loca.PdxLocaSource;
import com.popush.henrietta.discord.states.ParatranzEntry;
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
    public PdxLocaYamlRecord get(String key) {

        GetResponse<ParatranzEntry> response;
        try {
            response = elasticsearchClient.get(g -> g
                            .index("eu4")
                            .id(key),
                    ParatranzEntry.class
            );
        } catch (IOException e) {
            throw new RuntimeException("SystemException", e);
        }
        var src = Objects.requireNonNullElse(response.source(), new ParatranzEntry());

        return new PdxLocaYamlRecord(src.getKey(), src.getStage(), src.getTranslation(),
                "", 0, src.getFile());
    }

    @Override
    public List<String> getKeys(String fileName) throws ArgumentException {
        return null;
    }

    @Override
    public boolean exists(String key) {
        return false;
    }
}
