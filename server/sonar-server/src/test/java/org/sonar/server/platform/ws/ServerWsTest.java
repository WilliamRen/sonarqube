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

package org.sonar.server.platform.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerWsTest {

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new ServerWs());
  }

  @Test
  public void define_controller() throws Exception {
    WebService.Controller controller = tester.controller("api/server");
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("2.10");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(3);
  }

  @Test
  public void define_index_action() throws Exception {
    WebService.Controller controller = tester.controller("api/server");

    WebService.Action action = controller.action("index");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_system_action() throws Exception {
    WebService.Controller controller = tester.controller("api/server");

    WebService.Action action = controller.action("system");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_setup_action() throws Exception {
    WebService.Controller controller = tester.controller("api/server");

    WebService.Action action = controller.action("setup");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }
}
