package org.articioc.base.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ChunkStream {
  public static <T> Stream<Stream<T>> chunkBy(Stream<T> inputStream, int chunkSize) {
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("Chunk size must be greater than 0.");
    }

    Iterator<T> iterator = inputStream.iterator();

    return Stream.generate(() -> {
          List<T> chunk = Stream.generate(() -> iterator.hasNext() ? iterator.next() : null)
              .limit(chunkSize)
              .filter(Objects::nonNull)
              .toList();
          return chunk.isEmpty() ? null : chunk.stream();
        })
        .takeWhile(Objects::nonNull)
        .onClose(inputStream::close);
  }
}
