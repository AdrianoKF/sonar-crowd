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

import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CrowdConfigurationTest {
  @Test(expected = IllegalArgumentException.class)
  public void crowdUrlMissing() {
    final Configuration config = new ConfigurationBridge(new MapSettings());
    new CrowdConfiguration(config).getCrowdUrl();
  }

  @Test(expected = IllegalArgumentException.class)
  public void applicationPasswordMissing() {
    final MapSettings settings = new MapSettings();
    final Configuration config = new ConfigurationBridge(settings);
    settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
    new CrowdConfiguration(config).getCrowdApplicationPassword();
  }

  @Test
  public void usesFallbackForUnsetApplicationName() {
    final MapSettings settings = new MapSettings();
    settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_PASSWORD, "secret");
    final Configuration config = new ConfigurationBridge(settings);
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(config);
    assertThat(crowdConfiguration.getCrowdApplicationName(), is(CrowdConfiguration.FALLBACK_NAME));
  }

  @Test
  public void createsClientProperties() {
    final MapSettings settings = new MapSettings();
    settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_NAME, "SonarQube");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_PASSWORD, "secret");

    final Configuration config = new ConfigurationBridge(settings);
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(config);

    assertThat(crowdConfiguration.getCrowdUrl(), is("http://localhost:8095"));
    assertThat(crowdConfiguration.getCrowdApplicationName(), is("SonarQube"));
    assertThat(crowdConfiguration.getCrowdApplicationPassword(), is("secret"));
  }
}
