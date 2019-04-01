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

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.Version;
import org.sonar.plugins.php.PhpTestUtils;
import org.sonar.plugins.php.api.Php;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class PhpUnitCoverageResultImporterTest {

  private static final String BASE_DIR = "/org/sonar/plugins/php/phpunit/sensor/src/";
  private static final String MONKEY_FILE_NAME = "Monkey.php";
  private static final String BANANA_FILE_NAME = "Banana.php";
  private static final File MONKEY_FILE = TestUtils.getResource(BASE_DIR + MONKEY_FILE_NAME);
  private static final File BANANA_FILE = TestUtils.getResource(BASE_DIR + BANANA_FILE_NAME);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private PhpUnitCoverageResultImporter importer;

  private Map<String, Integer> numberOfLinesOfCode;
  private SensorContextTester context;

  private SensorContext setUpForMockedSensorContext() {
    return Mockito.mock(SensorContext.class);
  }

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(TestUtils.getResource(BASE_DIR));
    DefaultInputFile monkeyFile = new DefaultInputFile("moduleKey", MONKEY_FILE_NAME)
        .setType(InputFile.Type.MAIN)
        .setLanguage(Php.KEY)
        .setCharset(Charset.defaultCharset())
        .setLines(50);
    context.fileSystem().add(monkeyFile);

    numberOfLinesOfCode = new HashMap<>();

    importer = new PhpUnitCoverageResultImporter();
  }

  @Test
  public void should_throw_an_exception_when_report_not_found() {
    SensorContext context = setUpForMockedSensorContext();
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can't read phpUnit report:");
    importer.importReport(new File("notfound.txt"), context, numberOfLinesOfCode);
  }


  @Test
  public void should_parse_even_with_package_node() throws Exception {
    String componentKey = "moduleKey:Monkey.php"; // see call to method getReportsWithAbsolutePath below

    importer.importReport(getReportsWithAbsolutePath("phpunit.coverage-with-package.xml"), context, numberOfLinesOfCode);

    assertCoverageLineHits(context, componentKey, 34, 1);
  }

   @Test
   public void should_generate_coverage_measures() throws Exception {
     String componentKey = "moduleKey:Monkey.php"; // see call to method getReportsWithAbsolutePath below

     importer.importReport(getReportsWithAbsolutePath("phpunit.coverage.xml"), context, numberOfLinesOfCode);

     // UNCOVERED_LINES is implicitly stored in the NewCoverage
     PhpTestUtils.assertNoMeasure(context, componentKey, CoreMetrics.UNCOVERED_LINES);

     assertCoverageLineHits(context, componentKey, 34, 1);
     assertCoverageLineHits(context, componentKey, 35, 1);
     assertCoverageLineHits(context, componentKey, 38, 1);
     assertCoverageLineHits(context, componentKey, 40, 0);
     assertCoverageLineHits(context, componentKey, 45, 1);
     assertCoverageLineHits(context, componentKey, 46, 1);
   }

  /**
   * SONARPLUGINS-1591
   */
  @Test
  public void should_not_fail_if_no_statement_count() throws Exception {
    String componentKey = "moduleKey:Monkey.php"; // see call to method getReportsWithAbsolutePath below

    importer.importReport(getReportsWithAbsolutePath("phpunit.coverage-with-no-statements-covered.xml"), context, numberOfLinesOfCode);

    assertCoverageLineHits(context, componentKey, 31, 0);
  }

  /**
   * SONARPLUGINS-1675
   */
  @Test
  public void should_not_fail_if_no_line_for_file_node() throws Exception {
    importer.importReport(getReportsWithAbsolutePath("phpunit.coverage-with-filenode-without-line.xml"), context, numberOfLinesOfCode);
  }

  @Test
  public void should_set_metrics_to_ncloc_for_missing_files() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SCANNER));
    String componentKey = "moduleKey:Monkey.php"; // see call to method getReportsWithAbsolutePath below

    numberOfLinesOfCode.put(MONKEY_FILE_NAME, 42);

    importer.importReport(getReportsWithAbsolutePath("phpunit.coverage-empty.xml"), context, numberOfLinesOfCode);

    PhpTestUtils.assertMeasure(context, componentKey, CoreMetrics.LINES_TO_COVER, 42);
    PhpTestUtils.assertMeasure(context, componentKey, CoreMetrics.UNCOVERED_LINES, 42);
  }

  @Test
  public void should_not_set_metrics_to_ncloc_for_missing_files_sq_62() throws Exception {
    String componentKey = "moduleKey:Monkey.php"; // see call to method getReportsWithAbsolutePath below

    numberOfLinesOfCode.put(MONKEY_FILE_NAME, 42);

    importer.importReport(getReportsWithAbsolutePath("phpunit.coverage-empty.xml"), context, numberOfLinesOfCode);

    // since SQ 6.2 these are not saved
    assertThat(context.measure(componentKey, CoreMetrics.LINES_TO_COVER)).isNull();
    assertThat(context.measure(componentKey, CoreMetrics.UNCOVERED_LINES)).isNull();
  }

  /**
   * Replace file name with absolute path in coverage report.
   *
   * This hack allow to have this unit test, as only absolute path
   * in report is supported.
   * */
  private File getReportsWithAbsolutePath(String reportName) throws Exception {
    File fileWIthAbsolutePaths = folder.newFile("report_with_absolute_paths.xml");

    Files.write(
      Files.toString(TestUtils.getResource(PhpTestUtils.PHPUNIT_REPORT_DIR + reportName), StandardCharsets.UTF_8)
        .replace("/" + MONKEY_FILE_NAME, MONKEY_FILE.getAbsolutePath())
        .replace("/" + BANANA_FILE_NAME, BANANA_FILE.getAbsolutePath()),
      fileWIthAbsolutePaths, StandardCharsets.UTF_8);

    return fileWIthAbsolutePaths;
  }

  private void assertCoverageLineHits(SensorContextTester context, String componentKey, int line, int expectedHits) {
    assertThat(context.lineHits(componentKey, line)).as("coverage line hits for line: " + line).isEqualTo(expectedHits);
  }

}
