/*
 * Sonar Crowd Plugin
 * Copyright (C) 2009 Evgeny Mandrikov
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.crowd;

import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.DirectoryEntity;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.service.client.CrowdClient;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.security.ExternalGroupsProvider;

import java.util.*;
import java.util.function.Function;

import static com.google.common.collect.Collections2.transform;

/**
 * External groups provider implementation for Atlassian Crowd.
 */
public class CrowdGroupsProvider extends ExternalGroupsProvider {
  private static final Logger LOG = LoggerFactory.getLogger(CrowdGroupsProvider.class);
  private static final int PAGING_SIZE = 100; // no idea what a reasonable size might be
  private static final Function<Group, String> GROUP_TO_STRING = DirectoryEntity::getName;
  private final CrowdClient crowdClient;

  public CrowdGroupsProvider(CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

  private Collection<String> getGroupsForUser(String username, int start, int pageSize)
    throws UserNotFoundException, OperationFailedException, InvalidAuthenticationException,
    ApplicationPermissionException {
    return transform(crowdClient.getGroupsForNestedUser(username, start, pageSize), GROUP_TO_STRING::apply);
  }

  private List<String> getGroupsForUser(String username)
    throws UserNotFoundException,
    OperationFailedException, InvalidAuthenticationException,
    ApplicationPermissionException {

    Collection<String> groups = new LinkedHashSet<>();
    boolean mightHaveMore = true;
    int groupIndex = 0;
    Collection<String> newGroups;

    while (mightHaveMore) {
      newGroups = getGroupsForUser(username, groupIndex, PAGING_SIZE);
      if (newGroups.size() < PAGING_SIZE) {
        mightHaveMore = false;
      }
      groups.addAll(newGroups);
      groupIndex += newGroups.size();
    }
    return ImmutableList.copyOf(groups);
  }

  @Override
  public Collection<String> doGetGroups(Context context) {
      final String username = context.getUsername();
      LOG.debug("Looking up user groups for user {}", username);

    try {
      return getGroupsForUser(username);
    } catch (UserNotFoundException e) {
      return null; // API contract for ExternalGroupsProvider
    } catch (OperationFailedException e) {
      throw new IllegalStateException("Unable to retrieve groups for user" + username + " from crowd.", e);
    } catch (ApplicationPermissionException e) {
      throw new IllegalArgumentException("Unable to retrieve groups for user" + username
        + " from crowd. The application name and password are incorrect.", e);
    } catch (InvalidAuthenticationException e) {
      throw new IllegalStateException(
        "Unable to retrieve groups for user" + username
          + " from crowd. The application is not permitted to perform the "
          + "requested operation on the crowd server.", e);
    }
  }
}
