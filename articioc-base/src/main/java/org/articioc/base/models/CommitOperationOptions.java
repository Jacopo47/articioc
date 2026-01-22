package org.articioc.base.models;

import io.vavr.collection.Stream;
import org.articioc.base.Step;

public record CommitOperationOptions<A>(Stream<A> recordsInOutput, Step targetStep) {}
