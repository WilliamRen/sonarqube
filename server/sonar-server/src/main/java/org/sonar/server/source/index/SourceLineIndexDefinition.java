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
package org.sonar.server.source.index;

import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessConstants;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;
import org.sonar.server.search.BaseNormalizer;

public class SourceLineIndexDefinition implements IndexDefinition {

  public static final String FIELD_PROJECT_UUID = "projectUuid";
  public static final String FIELD_FILE_UUID = "fileUuid";
  public static final String FIELD_LINE = "line";
  public static final String FIELD_SCM_REVISION = "scmRevision";
  public static final String FIELD_SCM_AUTHOR = "scmAuthor";
  public static final String FIELD_SCM_DATE = "scmDate";
  public static final String FIELD_HIGHLIGHTING = "highlighting";
  public static final String FIELD_SOURCE = "source";
  public static final String FIELD_UT_LINE_HITS = "utLineHits";
  public static final String FIELD_UT_CONDITIONS = "utConditions";
  public static final String FIELD_UT_COVERED_CONDITIONS = "utCoveredConditions";
  public static final String FIELD_IT_LINE_HITS = "itLineHits";
  public static final String FIELD_IT_CONDITIONS = "itConditions";
  public static final String FIELD_IT_COVERED_CONDITIONS = "itCoveredConditions";
  public static final String FIELD_OVERALL_LINE_HITS = "overallLineHits";
  public static final String FIELD_OVERALL_CONDITIONS = "overallConditions";
  public static final String FIELD_OVERALL_COVERED_CONDITIONS = "overallCoveredConditions";
  public static final String FIELD_SYMBOLS = "symbols";
  public static final String FIELD_DUPLICATIONS = "duplications";

  public static final String INDEX = "sourcelines";

  public static final String TYPE = "sourceline";


  private final Settings settings;

  public SourceLineIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    // shards
    boolean clusterMode = settings.getBoolean(ProcessConstants.CLUSTER_ACTIVATE);
    if (clusterMode) {
      index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 4);
      index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1);
      // else keep defaults (one shard)
    }

    // type "sourceline"
    NewIndex.NewIndexType sourceLineMapping = index.createType(TYPE);
    sourceLineMapping.stringFieldBuilder(FIELD_PROJECT_UUID).build();
    sourceLineMapping.stringFieldBuilder(FIELD_FILE_UUID).build();
    sourceLineMapping.createIntegerField(FIELD_LINE);
    sourceLineMapping.stringFieldBuilder(FIELD_SCM_REVISION).build();
    sourceLineMapping.stringFieldBuilder(FIELD_SCM_AUTHOR).build();
    sourceLineMapping.createDateTimeField(FIELD_SCM_DATE);
    sourceLineMapping.stringFieldBuilder(FIELD_HIGHLIGHTING).build();
    sourceLineMapping.stringFieldBuilder(FIELD_SOURCE).build();
    sourceLineMapping.createIntegerField(FIELD_UT_LINE_HITS);
    sourceLineMapping.createIntegerField(FIELD_UT_CONDITIONS);
    sourceLineMapping.createIntegerField(FIELD_UT_COVERED_CONDITIONS);
    sourceLineMapping.createIntegerField(FIELD_IT_LINE_HITS);
    sourceLineMapping.createIntegerField(FIELD_IT_CONDITIONS);
    sourceLineMapping.createIntegerField(FIELD_IT_COVERED_CONDITIONS);
    sourceLineMapping.createIntegerField(FIELD_OVERALL_LINE_HITS);
    sourceLineMapping.createIntegerField(FIELD_OVERALL_CONDITIONS);
    sourceLineMapping.createIntegerField(FIELD_OVERALL_COVERED_CONDITIONS);
    sourceLineMapping.stringFieldBuilder(FIELD_SYMBOLS).build();
    sourceLineMapping.createShortField(FIELD_DUPLICATIONS);
    sourceLineMapping.createDateTimeField(BaseNormalizer.UPDATED_AT_FIELD);
  }
}
