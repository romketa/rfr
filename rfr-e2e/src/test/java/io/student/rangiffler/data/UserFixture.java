package io.student.rangiffler.data;

public interface UserFixture extends AutoCloseable {

  void create();

  TestUser user();

  @Override
  void close();
}
