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

import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.service.client.CrowdClient;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.security.ExternalGroupsProvider;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CrowdGroupsProviderTest {
  private final ExternalGroupsProvider.Context context = new ExternalGroupsProvider.Context("user", null);

  @Test
  public void returnsNullIfTheUserWasNotFound() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    when(client.getGroupsForNestedUser(anyString(), anyInt(), anyInt())).thenThrow(
      new UserNotFoundException(""));
    assertThat(new CrowdGroupsProvider(client).doGetGroups(context), is(nullValue()));
  }

  @Test(expected = IllegalStateException.class)
  public void throwsSonarExceptionIfCommunicationWithCrowdFails() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    when(client.getGroupsForNestedUser(anyString(), anyInt(), anyInt())).thenThrow(
      new OperationFailedException(""));
    new CrowdGroupsProvider(client).doGetGroups(context);
  }

  private List<Group> makeGroups(int count, int offset) {
    Builder<Group> builder = new Builder<>();
    for (int i = 0; i < count; i++) {
      Group group = mock(Group.class);
      when(group.getName()).thenReturn("group" + (offset + i));
      builder.add(group);
    }
    return builder.build();
  }

  private List<Group> makeGroups(int count) {
    return makeGroups(count, 0);
  }

  @Test
  public void returnsGroups() throws Exception {
    CrowdClient client = mock(CrowdClient.class);

    List<Group> groups = makeGroups(10);
    when(client.getGroupsForNestedUser(anyString(), anyInt(), anyInt())).thenReturn(groups);

    List<String> resolvedGroups =
      Lists.newLinkedList(new CrowdGroupsProvider(client).doGetGroups(context));
    assertThat(resolvedGroups.size(), is(groups.size()));
    for (int i = 0; i < groups.size(); i++) {
      assertThat(resolvedGroups.get(i), is(groups.get(i).getName()));
    }
    verify(client, times(1)).getGroupsForNestedUser(anyString(), anyInt(), anyInt());
  }

  @Test
  public void performsPagination() throws Exception {
    CrowdClient client = mock(CrowdClient.class);

    List<Group> firstGroups = makeGroups(100);
    List<Group> secondGroups = makeGroups(25, 100);
    List<Group> allGroups = new LinkedList<>();
    allGroups.addAll(firstGroups);
    allGroups.addAll(secondGroups);
    when(client.getGroupsForNestedUser(anyString(), eq(0), anyInt())).thenReturn(firstGroups);
    when(client.getGroupsForNestedUser(anyString(), eq(100), anyInt())).thenReturn(secondGroups);

    List<String> resolvedGroups =
      Lists.newLinkedList(new CrowdGroupsProvider(client).doGetGroups(context));
    assertThat(resolvedGroups.size(), is(125));
    for (int i = 0; i < resolvedGroups.size(); i++) {
      assertThat(resolvedGroups.get(i), is(allGroups.get(i).getName()));
    }
    verify(client, times(2)).getGroupsForNestedUser(anyString(), anyInt(), anyInt());
  }
}
