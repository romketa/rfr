package io.student.rangiffler.data.db;

import java.util.List;

public final class SchemaDrift {

  private final List<Migration> ahead;
  private final List<Migration> behind;
  private final List<Migration> changed;

  private SchemaDrift(List<Migration> ahead, List<Migration> behind, List<Migration> changed) {
    this.ahead = ahead;
    this.behind = behind;
    this.changed = changed;
  }

  public static SchemaDrift between(List<Migration> applied, List<Migration> resolved) {
    return new SchemaDrift(
        applied.stream().filter(migration -> resolved.stream().noneMatch(migration::sameVersion))
            .toList(),
        resolved.stream().filter(migration -> applied.stream().noneMatch(migration::sameVersion))
            .toList(),
        resolved.stream().filter(migration -> applied.stream()
            .anyMatch(row -> row.sameVersion(migration) && !row.equals(migration))).toList());
  }

  public boolean isEmpty() {
    return ahead.isEmpty() && behind.isEmpty() && changed.isEmpty();
  }

  public List<Migration> ahead() {
    return ahead;
  }

  public List<Migration> behind() {
    return behind;
  }

  public List<Migration> changed() {
    return changed;
  }
}
