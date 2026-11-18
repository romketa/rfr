package io.student.rangiffler.data.db;

public interface Row {

  Row create();

  void delete();

  boolean exists();
}
