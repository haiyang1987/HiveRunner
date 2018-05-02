package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MagicRule implements TestRule {

  private final List<TestRule> rules;

  /*
   * rules.addAll(super.getTestRules(target)); rules.add(hiveRunnerRule); rules.add(testBaseDir);
   * rules.addAll(super.getTestRules(target)); rules.add(hiveRunnerRule); rules.add(testBaseDir);
   */

  MagicRule(List<TestRule> rules) {
    List<TestRule> reversed = new ArrayList<>(rules);
    Collections.reverse(reversed);
    this.rules = reversed;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    for (TestRule rule : rules) {
      rule.apply(base, description);
    }
    return null;
  }

}
