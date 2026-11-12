package io.student.rangiffler.data.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public final class Migration {

  private static final Pattern FILE_NAME = Pattern.compile(
      "V(?<version>[0-9][0-9._]*?)__(?<description>.+)\\.sql");

  private final String version;
  private final String description;
  private final int checksum;

  Migration(String version, String description, int checksum) {
    this.version = version;
    this.description = description;
    this.checksum = checksum;
  }

  public static List<Migration> inDirectory(Path directory) {
    if (!Files.isDirectory(directory)) {
      throw new IllegalStateException("Migration directory " + directory + " does not exist");
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files
          .filter(file -> FILE_NAME.matcher(file.getFileName().toString()).matches())
          .sorted()
          .map(Migration::fromFile)
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("Migration directory " + directory + " could not be listed",
          e);
    }
  }

  static Migration fromFile(Path file) {
    Matcher name = FILE_NAME.matcher(file.getFileName().toString());
    if (!name.matches()) {
      throw new IllegalArgumentException(file + " is not a versioned Flyway migration");
    }
    return new Migration(name.group("version"), name.group("description").replace('_', ' '),
        checksum(file));
  }

  public String version() {
    return version;
  }

  public boolean sameVersion(Migration other) {
    return version.equals(other.version);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Migration that
        && version.equals(that.version)
        && description.equals(that.description)
        && checksum == that.checksum;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, description, checksum);
  }

  @Override
  public String toString() {
    return "V" + version + " (" + description + ")";
  }

  private static int checksum(Path file) {
    CRC32 crc = new CRC32();
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line = reader.readLine();
      if (line != null && line.startsWith("﻿")) {
        line = line.substring(1);
      }
      while (line != null) {
        crc.update(line.getBytes(StandardCharsets.UTF_8));
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Migration " + file + " could not be read", e);
    }
    return (int) crc.getValue();
  }
}
