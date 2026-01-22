package org.articioc.tests.scenarios;

import java.util.Objects;
import org.articioc.base.Leaf;
import org.articioc.base.Step;

public class BasicProviderTestLeaf<M> extends Leaf<M> {

  private String step0;
  private String step1;
  private String step2;
  private String step3;
  private String step4;

  public BasicProviderTestLeaf() {
    super(null, null);
  }

  public BasicProviderTestLeaf(M metadata, Step step) {
    super(metadata, step);
  }

  public String getStep0() {
    return step0;
  }

  public BasicProviderTestLeaf<M> setStep0(String step0) {
    this.step0 = step0;
    return this;
  }

  public String getStep1() {
    return step1;
  }

  public BasicProviderTestLeaf<M> setStep1(String step1) {
    this.step1 = step1;
    return this;
  }

  public String getStep2() {
    return step2;
  }

  public BasicProviderTestLeaf<M> setStep2(String step2) {
    this.step2 = step2;
    return this;
  }

  public String getStep3() {
    return step3;
  }

  public BasicProviderTestLeaf<M> setStep3(String step3) {
    this.step3 = step3;
    return this;
  }

  public String getStep4() {
    return step4;
  }

  public BasicProviderTestLeaf<M> setStep4(String step4) {
    this.step4 = step4;
    return this;
  }

  public BasicProviderTestLeaf<M> copy() {
    return new BasicProviderTestLeaf<>(this.getMetadata(), this.getStep())
        .setStep0(this.getStep0())
        .setStep1(this.getStep1())
        .setStep2(this.getStep2())
        .setStep3(this.getStep3())
        .setStep4(this.getStep4());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BasicProviderTestLeaf<M> i = (BasicProviderTestLeaf<M>) o;
    return Objects.equals(step0, i.step0)
        && Objects.equals(step1, i.step1)
        && Objects.equals(step2, i.step2)
        && Objects.equals(step3, i.step3)
        && Objects.equals(step4, i.step4);
  }

  @Override
  public int hashCode() {
    return Objects.hash(step0, step1, step2, step3, step4);
  }

  @Override
  public String toString() {
    return "MyLeaf{" + "Step0='"
        + step0 + '\'' + ", Step1='"
        + step1 + '\'' + ", Step2='"
        + step2 + '\'' + ", Step3='"
        + step3 + '\'' + ", Step4='"
        + step4 + '\'' + '}';
  }

  @Override
  public String key() {
    return this.getStep().getName();
  }
}
