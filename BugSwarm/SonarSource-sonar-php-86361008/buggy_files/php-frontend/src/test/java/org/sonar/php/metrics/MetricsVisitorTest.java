/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
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
package org.sonar.php.metrics;

import org.junit.Test;
import org.sonar.api.measures.FileLinesContext;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MetricsVisitorTest extends MetricTest {

  @Test
  public void test() {
    String filename = "lines_of_code.php";
    File file = new File(filename);

    FileLinesContext linesContext = mock(FileLinesContext.class);

    MetricsVisitor metricsVisitor = new MetricsVisitor();

    FileMeasures fileMeasures = metricsVisitor.getFileMeasures(file, parse(filename), linesContext);

    // fixme : finish this test
    assertThat(fileMeasures.getFileComplexity()).isEqualTo(1.0);
    assertThat(fileMeasures.getFunctionNumber()).isEqualTo(1.0);
    assertThat(fileMeasures.getStatementNumber()).isEqualTo(0.0);
    assertThat(fileMeasures.getClassNumber()).isEqualTo(1.0);
  }
}
