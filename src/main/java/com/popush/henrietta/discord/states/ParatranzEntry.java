package com.popush.henrietta.discord.states;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParatranzEntry {
    private String file;

    private String key;

    private String original;

    private String filePath;

    private String translation;

    private String context;

    private int stage;

    @JsonProperty("pz_pj_code")
    private int pzPjCode;
}
