package org.articioc.tests.models;

import java.util.Objects;
import org.articioc.base.Leaf;
import org.articioc.base.Step;

public class TestLeaf extends Leaf<TestStep> {

  private String Step0;
  private String Step1;
  private String Step2;
  private String Step3;
  private String Step4;

  public TestLeaf() {
    super(null, null);
  }

  public TestLeaf(Step step) {
    super(null, step);
  }

  public String getStep0() {
    return Step0;
  }

  public TestLeaf setStep0(String step0) {
    Step0 = step0;
    return this;
  }

  public String getStep1() {
    return Step1;
  }

  public TestLeaf setStep1(String step1) {
    Step1 = step1;
    return this;
  }

  public String getStep2() {
    return Step2;
  }

  public TestLeaf setStep2(String step2) {
    Step2 = step2;
    return this;
  }

  public String getStep3() {
    return Step3;
  }

  public TestLeaf setStep3(String step3) {
    Step3 = step3;
    return this;
  }

  public String getStep4() {
    return Step4;
  }

  public TestLeaf setStep4(String step4) {
    Step4 = step4;
    return this;
  }

  public TestLeaf copy() {
    return new TestLeaf(this.getStep())
        .setStep0(this.getStep0())
        .setStep1(this.getStep1())
        .setStep2(this.getStep2())
        .setStep3(this.getStep3())
        .setStep4(this.getStep4());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TestLeaf testLeaf = (TestLeaf) o;
    return Objects.equals(Step0, testLeaf.Step0)
        && Objects.equals(Step1, testLeaf.Step1)
        && Objects.equals(Step2, testLeaf.Step2)
        && Objects.equals(Step3, testLeaf.Step3)
        && Objects.equals(Step4, testLeaf.Step4);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Step0, Step1, Step2, Step3, Step4);
  }

  @Override
  public String toString() {
    return "TestLeaf{" + "Step0='"
        + Step0 + '\'' + ", Step1='"
        + Step1 + '\'' + ", Step2='"
        + Step2 + '\'' + ", Step3='"
        + Step3 + '\'' + ", Step4='"
        + Step4 + '\'' + '}';
  }

  @Override
  public String key() {
    return "";
  }
}
