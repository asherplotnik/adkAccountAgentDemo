package com.idb.directchannels.adkAccountAgentDemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaData(
        String updateTime) {
}
