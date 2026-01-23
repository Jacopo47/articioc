package org.articioc.interfaces.triggers;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface FutureTriggerOfOne<T> extends Supplier<CompletableFuture<T>> {}
