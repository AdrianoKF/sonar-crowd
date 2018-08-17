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
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import org.junit.Test;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.UserDetails;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CrowdUsersProviderTest {
  @Test
  public void returnsNullIfTheUserWasNotFound() throws Exception {
    final ExternalUsersProvider.Context context = new ExternalUsersProvider.Context("user", null);
    CrowdClient client = mock(CrowdClient.class);
    when(client.getUser(anyString())).thenThrow(new UserNotFoundException(""));

    CrowdUsersProvider provider = new CrowdUsersProvider(client);
    assertThat(provider.doGetUserDetails(context), is(nullValue()));
  }

  @Test
  public void returnsTheCrowdDisplayNameAndEmailAddress() throws Exception {
    final ExternalUsersProvider.Context context = new ExternalUsersProvider.Context("user", null);
    CrowdClient client = mock(CrowdClient.class);
    User user = mock(User.class);
    when(user.getDisplayName()).thenReturn("display name");
    when(user.getEmailAddress()).thenReturn("foo@acme.corp");
    when(client.getUser(anyString())).thenReturn(user);

    CrowdUsersProvider provider = new CrowdUsersProvider(client);
    UserDetails userDetails = provider.doGetUserDetails(context);
    assertThat(userDetails, is(notNullValue()));
    assertThat(userDetails.getEmail(), is("foo@acme.corp"));
    assertThat(userDetails.getName(), is("display name"));
  }

  @Test(expected = IllegalStateException.class)
  public void throwsSonarExceptionIfCrowdCommunicationFails() throws Exception {
    final ExternalUsersProvider.Context context = new ExternalUsersProvider.Context("user", null);
    CrowdClient client = mock(CrowdClient.class);
    when(client.getUser(anyString())).thenThrow(new OperationFailedException(""));
    new CrowdUsersProvider(client).doGetUserDetails(context);
  }
}
