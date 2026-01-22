package org.articioc.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public class Step {

  public static final Step FINAL = new Step("final", true);
  public static final Step ENDLESS = new Step("endless", false);

  private final String name;
  private final boolean isFinal;
  private boolean isReadonly;

  public Step(String name) {
    this(name, false);
  }

  @JsonCreator
  private Step(@JsonProperty("name") String name, @JsonProperty("isFinal") boolean isFinal) {
    this.name = name;
    this.isFinal = isFinal;
  }

  public String getName() {
    return name;
  }

  @JsonProperty("isFinal")
  public boolean isFinal() {
    return isFinal;
  }

  public boolean isReadonly() {
    return Optional.of(isReadonly).orElse(false);
  }

  public Step setReadonly(boolean readonly) {
    isReadonly = readonly;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Step step)) return false;

    return Objects.equals(name, step.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public String toString() {
    return "Step{" + "name='" + name + '\'' + ", isFinal=" + isFinal + '}';
  }
}
