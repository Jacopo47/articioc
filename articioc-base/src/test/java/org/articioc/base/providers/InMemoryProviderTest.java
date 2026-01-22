package org.articioc.base.providers;

import io.vavr.collection.Stream;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.articioc.base.LeafCarrier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InMemoryProviderTest {

  Function<CompletableFuture<Stream<LeafCarrier<TestLeaf>>>, TestLeaf> readAndCheckEqualsToData =
      read -> read.join().headOption().get().getData();

  @Test
  void basicScenario() {
    try (var provider = new InMemoryProvider<TestLeaf>()) {
      var data = new TestLeaf((TestStep._0));

      Assertions.assertThrows(
          NoSuchElementException.class, () -> readAndCheckEqualsToData.apply(provider.read()));

      provider.write(data);
      provider.write(data);
      provider.write(data);

      provider.write(Stream.of(data, data));

      Assertions.assertEquals(data, readAndCheckEqualsToData.apply(provider.read()));
      Assertions.assertEquals(data, readAndCheckEqualsToData.apply(provider.read()));
      Assertions.assertEquals(data, readAndCheckEqualsToData.apply(provider.read()));
      Assertions.assertEquals(data, readAndCheckEqualsToData.apply(provider.read()));
      Assertions.assertEquals(data, readAndCheckEqualsToData.apply(provider.read()));

      Assertions.assertThrows(
          NoSuchElementException.class, () -> readAndCheckEqualsToData.apply(provider.read()));
    }
  }
}
