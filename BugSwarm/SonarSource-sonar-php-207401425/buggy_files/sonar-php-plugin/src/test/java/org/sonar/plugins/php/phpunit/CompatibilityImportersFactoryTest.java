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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.plugins.php.PhpPlugin;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.php.phpunit.CompatibilityImportersFactory.DEPRECATION_WARNING_TEMPLATE;
import static org.sonar.plugins.php.phpunit.CompatibilityImportersFactory.SKIPPED_WARNING_TEMPLATE;

public class CompatibilityImportersFactoryTest {

  private static final String BASE_DIR = "/org/sonar/plugins/php/phpunit/sensor/src/";
  private static final Class[] LEGACY_IMPORTER_CLASSES = {
    PhpUnitTestResultImporter.class,
    PhpUnitCoverageResultImporter.class,
    PhpUnitItCoverageResultImporter.class,
    PhpUnitOverallCoverageResultImporter.class
  };
  private static final SonarRuntime SONAR_QUBE_6_2 = SonarRuntimeImpl.forSonarQube(Version.create(6, 2), SonarQubeSide.SCANNER);
  private static final SonarRuntime SONAR_QUBE_6_1 = SonarRuntimeImpl.forSonarQube(Version.create(6, 1), SonarQubeSide.SCANNER);
  private static final String COVERAGES_KEY = PhpPlugin.PHPUNIT_COVERAGE_REPORT_PATHS_KEY;
  private static final String COVERAGE_KEY = PhpPlugin.PHPUNIT_COVERAGE_REPORT_PATH_KEY;
  private static final String OVERALL_COVERAGE_KEY = PhpPlugin.PHPUNIT_OVERALL_COVERAGE_REPORT_PATH_KEY;
  private static final String IT_COVERAGE_KEY = PhpPlugin.PHPUNIT_IT_COVERAGE_REPORT_PATH_KEY;

  private SensorContextTester context;
  private CompatibilityImportersFactory importersFactory;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(TestUtils.getResource(BASE_DIR));
    importersFactory = new CompatibilityImportersFactory(context);
  }

  @Test
  public void should_create_legacy_coverage_importers_before_6_2() throws Exception {
    context.setRuntime(SONAR_QUBE_6_1);
    final List<Class> importerClasses = importerClasses(importersFactory.createImporters());
    assertThat(importerClasses).containsExactly(LEGACY_IMPORTER_CLASSES);
  }

  @Test
  public void should_create_test_result_and_multi_coverage_importer_starting_from_6_2() throws Exception {
    context.setRuntime(SONAR_QUBE_6_2);
    context.settings().setProperty(COVERAGES_KEY, "coverage report");
    final List<Class> importerClasses = importerClasses(importersFactory.createImporters());
    assertThat(importerClasses).containsExactly(PhpUnitTestResultImporter.class, MultiPathImporter.class);
  }

  @Test
  public void should_fallback_to_legacy_importers_if_multipath_coverage_property_not_used() throws Exception {
    context.setRuntime(SONAR_QUBE_6_2);
    final List<Class> importerClasses = importerClasses(importersFactory.createImporters());
    assertThat(importerClasses).containsExactly(LEGACY_IMPORTER_CLASSES);
  }

  @Test
  public void should_print_deprecation_warning_if_legacy_properties_used_from_6_2() throws Exception {
    context.setRuntime(SONAR_QUBE_6_2);
    context.settings().setProperty(COVERAGE_KEY, "coverage report");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      String.format(DEPRECATION_WARNING_TEMPLATE, COVERAGE_KEY));
    context.settings().setProperty(IT_COVERAGE_KEY, "integration coverage report");
    context.settings().setProperty(OVERALL_COVERAGE_KEY, "overall coverage report");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      String.format(DEPRECATION_WARNING_TEMPLATE, COVERAGE_KEY),
      String.format(DEPRECATION_WARNING_TEMPLATE, IT_COVERAGE_KEY),
      String.format(DEPRECATION_WARNING_TEMPLATE, OVERALL_COVERAGE_KEY));
  }

  @Test
  public void should_print_skipped_property_warning_if_both_multipath_and_legacy_properties_used_from_6_2() throws Exception {
    context.setRuntime(SONAR_QUBE_6_2);
    context.settings().setProperty(COVERAGES_KEY, "coverage report");
    assertThat(importersFactory.deprecationWarnings()).isEmpty();
    context.settings().setProperty(COVERAGE_KEY, "some forgotten configuration");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      String.format(SKIPPED_WARNING_TEMPLATE, COVERAGE_KEY));
    context.settings().setProperty(OVERALL_COVERAGE_KEY, "some forgotten configuration");
    context.settings().setProperty(IT_COVERAGE_KEY, "some forgotten configuration");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      String.format(SKIPPED_WARNING_TEMPLATE, COVERAGE_KEY),
      String.format(SKIPPED_WARNING_TEMPLATE, IT_COVERAGE_KEY),
      String.format(SKIPPED_WARNING_TEMPLATE, OVERALL_COVERAGE_KEY));
  }

  @Test
  public void should_not_print_any_warning_before_6_1() throws Exception {
    context.setRuntime(SONAR_QUBE_6_1);
    context.settings().setProperty(COVERAGES_KEY, "future report configuration");
    assertThat(importersFactory.deprecationWarnings()).isEmpty();
    context.settings().setProperty(COVERAGE_KEY, "coverage report");
    assertThat(importersFactory.deprecationWarnings()).isEmpty();
    context.settings().setProperty(OVERALL_COVERAGE_KEY, "integration coverage report");
    context.settings().setProperty(IT_COVERAGE_KEY, "overall coverage report");
    assertThat(importersFactory.deprecationWarnings()).isEmpty();
  }

  @Test
  public void should_print_properly_formatted_warnings() throws Exception {
    context.setRuntime(SONAR_QUBE_6_2);
    context.settings().setProperty(COVERAGE_KEY, "old coverage report");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      COVERAGE_KEY + " is deprecated as of SonarQube 6.2. It still works, but please consider using " + COVERAGES_KEY + ".");
    context.settings().setProperty(COVERAGES_KEY, "new coverage report");
    assertThat(importersFactory.deprecationWarnings()).containsExactly(
      "Ignoring " + COVERAGE_KEY + " since you are already using " + COVERAGES_KEY + ". Please remove " + COVERAGE_KEY + ".");
  }

  private List<Class> importerClasses(List<PhpUnitImporter> importers) {
    return importers.stream().map(PhpUnitImporter::getClass).collect(Collectors.toList());
  }

}
