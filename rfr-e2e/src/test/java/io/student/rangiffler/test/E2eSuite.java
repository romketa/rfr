package io.student.rangiffler.test;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.jupiter.extension.StandSchemaExtension;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite(failIfNoTests = false)
@SelectPackages({"io.student.rangiffler.test.web", "io.student.rangiffler.test.people"})
class E2eSuite {

  @BeforeSuite
  static void checkStandSchema() {
    StandSchemaExtension.verify(Config.repoRoot());
  }
}
