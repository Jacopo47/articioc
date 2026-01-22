package org.articioc.base.models;

import org.articioc.base.Step;

public record RollbackOperationOptions<A>(Throwable cause, Step targetStep) {}
