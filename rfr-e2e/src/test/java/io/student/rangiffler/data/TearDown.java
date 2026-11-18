package io.student.rangiffler.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class TearDown {

  private final List<Runnable> steps = new ArrayList<>();

  TearDown with(Runnable step) {
    steps.add(step);
    return this;
  }

  <T> TearDown withEach(Iterable<T> items, Consumer<? super T> step) {
    items.forEach(item -> steps.add(() -> step.accept(item)));
    return this;
  }

  void run() {
    List<RuntimeException> failures = new ArrayList<>();
    for (Runnable step : steps) {
      try {
        step.run();
      } catch (RuntimeException e) {
        failures.add(e);
      }
    }
    if (!failures.isEmpty()) {
      RuntimeException first = failures.getFirst();
      failures.stream().skip(1).forEach(first::addSuppressed);
      throw first;
    }
  }
}
