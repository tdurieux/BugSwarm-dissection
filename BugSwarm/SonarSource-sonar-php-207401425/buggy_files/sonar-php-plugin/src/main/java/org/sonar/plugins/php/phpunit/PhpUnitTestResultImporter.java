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

import java.io.File;
import java.util.Map;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.php.PhpPlugin;
import org.sonar.plugins.php.phpunit.xml.TestSuites;

public class PhpUnitTestResultImporter extends SingleFilePhpUnitImporter {

  private final JUnitLogParserForPhpUnit parser = new JUnitLogParserForPhpUnit();

  public PhpUnitTestResultImporter() {
    super(PhpPlugin.PHPUNIT_TESTS_REPORT_PATH_KEY, "test");
  }

  @Override
  protected void importReport(File reportFile, SensorContext context, Map<String, Integer> numberOfLinesOfCode) {
    TestSuites testSuites = parser.parse(reportFile);
    for (PhpUnitTestFileReport fileReport : testSuites.arrangeSuitesIntoTestFileReports()) {
      fileReport.saveTestMeasures(context);
    }
  }

}
