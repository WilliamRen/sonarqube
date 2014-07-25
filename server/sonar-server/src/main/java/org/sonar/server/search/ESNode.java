/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.search;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.server.search.es.ListUpdate;
import org.sonar.server.search.es.ListUpdate.UpdateListScriptFactory;

import java.io.File;

/**
 * ElasticSearch Node used to connect to index.
 */
public class ESNode implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ESNode.class);
  private static final String HTTP_ENABLED = "http.enabled";
  private static final String DEFAULT_HEALTH_TIMEOUT = "30s";

  private final Settings settings;
  private final String healthTimeout;

  // available only after startup
  private Client client;
  private Node node;

  public ESNode(Settings settings) {
    this(settings, DEFAULT_HEALTH_TIMEOUT);
  }

  @VisibleForTesting
  ESNode(Settings settings, String healthTimeout) {
    this.settings = settings;
    this.healthTimeout = healthTimeout;
  }

  @Override
  public void start() {
    initLogging();

    String typeValue = settings.getString(IndexProperties.TYPE);
    IndexProperties.ES_TYPE type =
      typeValue != null ?
        IndexProperties.ES_TYPE.valueOf(typeValue) :
        IndexProperties.ES_TYPE.DATA;

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder()
      .put("index.merge.policy.max_merge_at_once", "200")
      .put("index.merge.policy.segments_per_tier", "200")

      .put("indices.store.throttle.type", "merge")
      .put("indices.store.throttle.max_bytes_per_sec", "200mb")

      .put("script.default_lang", "native")
      .put("script.native." + ListUpdate.NAME + ".type", UpdateListScriptFactory.class.getName())

      .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(IndexProperties.CLUSTER_NAME), "sonarqube"));

    initAnalysis(esSettings);

    if (IndexProperties.ES_TYPE.TRANSPORT.equals(type)) {
      initRemoteClient(esSettings);
    } else {
      initLocalClient(type, esSettings);
    }

    if (client.admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(healthTimeout)
      .get()
      .getStatus() == ClusterHealthStatus.RED) {
      throw new IllegalStateException(String.format("Elasticsearch index is corrupt, please delete directory '%s' " +
        "and relaunch the SonarQube server.", esDataDir()));
    }

    addIndexTemplates();

    LOG.info("Elasticsearch started");
  }

  private void initRemoteClient(ImmutableSettings.Builder esSettings) {
    int port = settings.getInt(IndexProperties.NODE_PORT);
    client = new TransportClient(esSettings)
      .addTransportAddress(new InetSocketTransportAddress("localhost",
        port));
    LOG.info("Elasticsearch port: " + port);
  }

  private void initLocalClient(IndexProperties.ES_TYPE type, ImmutableSettings.Builder esSettings) {
    if (IndexProperties.ES_TYPE.MEMORY.equals(type)) {
      initMemoryES(esSettings);
    } else if (IndexProperties.ES_TYPE.DATA.equals(type)) {
      initDataES(esSettings);
    }
    initDirs(esSettings);
    initRestConsole(esSettings);
    initNetwork(esSettings);

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .node();
    node.start();

    client = node.client();
  }

  private void initMemoryES(ImmutableSettings.Builder builder) {
    builder
      .put("node.name", "node-mem-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("cluster.name", "cluster-mem-" + NetworkUtils.getLocalAddress().getHostName())
      .put("index.store.type", "memory")
      .put("index.store.fs.memory.enabled", "true")
      .put("gateway.type", "none")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("cluster.routing.schedule", "50ms")
      .put("node.local", true);
  }

  private void initDataES(ImmutableSettings.Builder builder) {
    builder
      .put("node.name", "sonarqube-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("node.local", true)
      .put("index.store.type", "mmapfs")
      .put("cluster.name", "sonarqube")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0");
  }

  private void addIndexTemplates() {
    client.admin().indices()
      .preparePutTemplate("default")
      .setTemplate("*")
      .addMapping("_default_", "{\"dynamic\": \"strict\"}")
      .get();
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  private void initAnalysis(ImmutableSettings.Builder esSettings) {
    esSettings
      .put("index.mapper.dynamic", false)

        // Sortable text analyzer
      .put("index.analysis.analyzer.sortable.type", "custom")
      .put("index.analysis.analyzer.sortable.tokenizer", "keyword")
      .putArray("index.analysis.analyzer.sortable.filter", "trim", "lowercase", "truncate")

        // Edge NGram index-analyzer
      .put("index.analysis.analyzer.index_grams.type", "custom")
      .put("index.analysis.analyzer.index_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.index_grams.filter", "trim", "lowercase", "gram_filter")

        // Edge NGram search-analyzer
      .put("index.analysis.analyzer.search_grams.type", "custom")
      .put("index.analysis.analyzer.search_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.search_grams.filter", "trim", "lowercase")

        // Word index-analyzer
      .put("index.analysis.analyzer.index_words.type", "custom")
      .put("index.analysis.analyzer.index_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.index_words.filter",
        "standard", "word_filter", "lowercase", "stop", "asciifolding", "porter_stem")

        // Word search-analyzer
      .put("index.analysis.analyzer.search_words.type", "custom")
      .put("index.analysis.analyzer.search_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.search_words.filter",
        "standard", "lowercase", "stop", "asciifolding", "porter_stem")

        // Edge NGram filter
      .put("index.analysis.filter.gram_filter.type", "edgeNGram")
      .put("index.analysis.filter.gram_filter.min_gram", 2)
      .put("index.analysis.filter.gram_filter.max_gram", 15)
      .putArray("index.analysis.filter.gram_filter.token_chars", "letter", "digit", "punctuation", "symbol")

        // Word filter
      .put("index.analysis.filter.word_filter.type", "word_delimiter")
      .put("index.analysis.filter.word_filter.generate_word_parts", true)
      .put("index.analysis.filter.word_filter.catenate_words", true)
      .put("index.analysis.filter.word_filter.catenate_numbers", true)
      .put("index.analysis.filter.word_filter.catenate_all", true)
      .put("index.analysis.filter.word_filter.split_on_case_change", true)
      .put("index.analysis.filter.word_filter.preserve_original", true)
      .put("index.analysis.filter.word_filter.split_on_numerics", true)
      .put("index.analysis.filter.word_filter.stem_english_possessive", true)

        // Path Analyzer
      .put("index.analysis.analyzer.path_analyzer.type", "custom")
      .put("index.analysis.analyzer.path_analyzer.tokenizer", "path_hierarchy");

  }

  private void initNetwork(ImmutableSettings.Builder esSettings) {
    esSettings.put("network.bind_host", "127.0.0.1");
  }

  private void initRestConsole(ImmutableSettings.Builder esSettings) {
    int httpPort = settings.getInt(IndexProperties.HTTP_PORT);
    if (httpPort > 0) {
      LOG.warn("Elasticsearch HTTP console enabled on port {}. Only for debugging purpose.", httpPort);
      esSettings.put(HTTP_ENABLED, true);
      esSettings.put("http.host", "127.0.0.1");
      esSettings.put("http.port", httpPort);
    } else {
      esSettings.put(HTTP_ENABLED, false);
    }
  }

  private void initDirs(ImmutableSettings.Builder esSettings) {
    File esDir = esDataDir();
    try {
      FileUtils.forceMkdir(esDir);
      esSettings.put("path.data", esDir.getAbsolutePath());
      LOG.debug("Elasticsearch data stored in {}", esDir.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create directory " + esDir, e);
    }
  }

  private File esDataDir() {
    return new File(settings.getString("sonar.path.data"), "es");
  }

  @Override
  public void stop() {
    if (client != null) {
      client.close();
      client = null;
    }
    if (node != null) {
      node.close();
      node = null;
    }
  }

  public Client client() {
    if (client == null) {
      throw new IllegalStateException("Elasticsearch is not started");
    }
    return client;
  }
}