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

package org.sonar.server.component.db;

import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 4.3
 */
public class ComponentDao extends BaseDao<ComponentMapper, ComponentDto, String> implements ServerComponent, DaoComponent {

  public ComponentDao() {
    this(System2.INSTANCE);
  }

  public ComponentDao(System2 system) {
    super(ComponentMapper.class, system);
  }

  public ComponentDto getById(Long id, DbSession session) {
    ComponentDto componentDto = getNullableById(id, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with id '%s' not found", id));
    }
    return componentDto;
  }

  @CheckForNull
  public ComponentDto getNullableById(Long id, DbSession session) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public ComponentDto getNullableByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

  public ComponentDto getByUuid(DbSession session, String uuid) {
    ComponentDto componentDto = getNullableByUuid(session, uuid);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with uuid '%s' not found", uuid));
    }
    return componentDto;
  }

  public boolean existsById(Long id, DbSession session) {
    return mapper(session).countById(id) > 0;
  }

  /**
   * Return null only if the component does not exists.
   * If the component if a root project, it will return itself.
   */
  @CheckForNull
  public ComponentDto getNullableRootProjectByKey(String componentKey, DbSession session) {
    return mapper(session).selectRootProjectByKey(componentKey);
  }

  public ComponentDto getRootProjectByKey(String componentKey, DbSession session) {
    ComponentDto componentDto = getNullableRootProjectByKey(componentKey, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Root project for project '%s' not found", componentKey));
    }
    return componentDto;
  }

  @CheckForNull
  public ComponentDto getParentModuleByKey(String componentKey, DbSession session) {
    return mapper(session).selectParentModuleByKey(componentKey);
  }

  public List<ComponentDto> findModulesByProject(String projectKey, DbSession session) {
    return mapper(session).findModulesByProject(projectKey);
  }

  public List<ComponentDto> findSubProjectsByComponentUuids(DbSession session, Collection<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).findSubProjectsByComponentUuids(keys);
  }

  public List<ComponentDto> findChildrenModulesFromModule(DbSession session, String moduleKey) {
    return mapper(session).findChildrenModulesFromModule(moduleKey, Scopes.PROJECT);
  }

  public List<FilePathWithHashDto> findFilesFromModule(DbSession session, String moduleKey) {
    return mapper(session).findFilesFromModule(moduleKey, Scopes.FILE);
  }

  public List<ComponentDto> getByUuids(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return Collections.emptyList();
    }
    List<ComponentDto> components = newArrayList();
    List<List<String>> partitionList = Lists.partition(newArrayList(uuids), 1000);
    for (List<String> partition : partitionList) {
      List<ComponentDto> dtos = mapper(session).findByUuids(partition);
      components.addAll(dtos);
    }
    return components;
  }

  @Override
  protected List<ComponentDto> doGetByKeys(DbSession session, Collection<String> keys) {
    return mapper(session).findByKeys(keys);
  }

  @Override
  @CheckForNull
  protected ComponentDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected ComponentDto doInsert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(DbSession session, String key) {
    mapper(session).deleteByKey(key);
  }

  public List<String> findProjectUuids(DbSession session) {
    return mapper(session).findProjectUuids();
  }
}
