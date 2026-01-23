package org.articioc.interfaces.triggers;

import io.vavr.collection.Stream;

import java.util.function.Supplier;

public interface TriggerOfMany<T> extends Supplier<Stream<T>> {}
