package org.articioc.interfaces.triggers;

import io.vavr.collection.Stream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface FutureTriggerOfMany<T> extends Supplier<CompletableFuture<Stream<T>>> {}
