package org.articioc.base.utils;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;

public class Futures {
  public static <E> CompletableFuture<Stream<E>> whenAll(
      Stream<CompletableFuture<Stream<E>>> input) {
    var futures = input.toJavaArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures).thenApply(v -> Stream.of(futures)
        .map(f -> (CompletableFuture<Stream<E>>) f)
        .flatMap(CompletableFuture::join));
  }

  public static <E> CompletableFuture<Stream<E>> whenAllAsStream(
      Stream<CompletableFuture<E>> input) {
    var futures = input.toJavaArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures)
        .thenApply(v ->
            Stream.of(futures)
                .map(f -> (CompletableFuture<E>) f)
                .map(CompletableFuture::join));
  }
}
