/*
 * Copyright (c) 2022 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.infor_compass;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.airbyte.commons.json.Jsons;
import io.airbyte.db.factory.DatabaseDriver;
import io.airbyte.db.jdbc.JdbcUtils;
import io.airbyte.db.jdbc.streaming.AdaptiveStreamingQueryConfig;
import io.airbyte.integrations.base.IntegrationRunner;
import io.airbyte.integrations.base.Source;
import io.airbyte.integrations.source.jdbc.AbstractJdbcSource;

import java.sql.JDBCType;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InforCompassSource extends AbstractJdbcSource<JDBCType> implements Source {

  private static final Logger LOGGER = LoggerFactory.getLogger(InforCompassSource.class);

  static final String DRIVER_CLASS = DatabaseDriver.INFORCOMPASS.getDriverClassName();

  public InforCompassSource() {
    // TODO: if the JDBC driver does not support custom fetch size, use NoOpStreamingQueryConfig
    // instead of AdaptiveStreamingQueryConfig.
    super(DRIVER_CLASS, AdaptiveStreamingQueryConfig::new, JdbcUtils.getDefaultSourceOperations());
  }

  public static void main(final String[] args) throws Exception {
    final Source source = new InforCompassSource();
    LOGGER.info("starting source: {}", InforCompassSource.class);
    new IntegrationRunner(source).run(args);
    LOGGER.info("completed source: {}", InforCompassSource.class);
  }

  // TODO The config is based on spec.json, update according to your DB
  @Override
  public JsonNode toDatabaseConfig(final JsonNode config) {
    final StringBuilder connectionProperties = new StringBuilder(String.format("ionApiCredentials=%s", config.get("ion_api_key").asText()));

    if (config.has("jdbc_url_params")) {
      connectionProperties.append("&").append(config.get("jdbc_url_params").asText());
    }

    final ImmutableMap.Builder<Object, Object> configBuilder = ImmutableMap.builder()
        .put("jdbc_url", String.format("jdbc:infordatalake://%s", config.get("tenant_id").asText()))
        .put("connection_properties", connectionProperties.toString());

    return Jsons.jsonNode(configBuilder.build());
  }

  @Override
  public Set<String> getExcludedInternalNameSpaces() {
    // TODO Add tables to exclude, Ex "INFORMATION_SCHEMA", "sys", "spt_fallback_db", etc
    return Set.of("");
  }

}
