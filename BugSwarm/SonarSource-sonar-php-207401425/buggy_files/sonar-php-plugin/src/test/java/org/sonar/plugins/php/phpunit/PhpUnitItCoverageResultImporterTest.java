/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.php.phpunit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThat;

public class PhpUnitItCoverageResultImporterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private SensorContext context;

  private PhpUnitCoverageResultImporter importer;

  @Before
  public void setUp() throws Exception {
    importer = new PhpUnitItCoverageResultImporter();
  }

  @Test
  public void shouldSetMetrics() {
    assertThat(importer.linesToCoverMetric).isEqualTo(CoreMetrics.IT_LINES_TO_COVER);
    assertThat(importer.uncoveredLinesMetric).isEqualTo(CoreMetrics.IT_UNCOVERED_LINES);
  }

}
