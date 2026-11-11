package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public final class StandSchemaSessionListener implements LauncherSessionListener {

  @Override
  public void launcherSessionOpened(LauncherSession session) {
    StandSchemaExtension.verify(Config.repoRoot());
  }
}
