package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runners.model.InitializationError;

import com.klarna.hiverunner.StandaloneHiveRunner;

public class MutantSwarmRunner extends StandaloneHiveRunner {

  public MutantSwarmRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

  @Override
  protected List<TestRule> getTestRules(Object target) {
    List<TestRule> rules = new ArrayList<>(super.getTestRules(target));
    HiveRunnerRule hiveRunnerRule = (HiveRunnerRule) rules.get(rules.size() - 4);
    rules.add(rules.size() - 1, new MutantSwarmRule(hiveRunnerRule));
    return rules;
  }

}
