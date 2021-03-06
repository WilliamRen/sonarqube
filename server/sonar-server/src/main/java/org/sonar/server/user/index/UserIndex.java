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

package org.sonar.server.user.index;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.ServerComponent;
import org.sonar.server.es.EsClient;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;

public class UserIndex implements ServerComponent {

  private final EsClient esClient;

  public UserIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  @CheckForNull
  public UserDoc getNullableByLogin(String login) {
    GetRequestBuilder request = esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, login)
      .setFetchSource(true)
      .setRouting(login);
    GetResponse response = request.get();
    if (response.isExists()) {
      return new UserDoc(response.getSource());
    }
    return null;
  }

  @CheckForNull
  public UserDoc getNullableByScmAccount(String scmAccount) {
    SearchRequestBuilder request = esClient.prepareSearch(UserIndexDefinition.INDEX)
      .setTypes(UserIndexDefinition.TYPE_USER)
      .setQuery(QueryBuilders.boolQuery()
        .should(QueryBuilders.termQuery(UserIndexDefinition.FIELD_LOGIN, scmAccount))
        .should(QueryBuilders.termQuery(UserIndexDefinition.FIELD_EMAIL, scmAccount))
        .should(QueryBuilders.termQuery(UserIndexDefinition.FIELD_SCM_ACCOUNTS, scmAccount)))
      .setSize(2);
    SearchHit[] result = request.get().getHits().getHits();
    if (result.length == 1) {
      return new UserDoc(result[0].sourceAsMap());
    }
    return null;
  }

  public UserDoc getByLogin(String login) {
    UserDoc userDoc = getNullableByLogin(login);
    if (userDoc == null) {
      throw new NotFoundException(String.format("User '%s' not found", login));
    }
    return userDoc;
  }

}
