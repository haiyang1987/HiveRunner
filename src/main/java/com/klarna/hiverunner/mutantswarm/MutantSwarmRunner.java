package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.rules.TestRule;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.klarna.hiverunner.StandaloneHiveRunner;

public class MutantSwarmRunner extends StandaloneHiveRunner {

  public MutantSwarmRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

  @Override
  public void run(final RunNotifier notifier) {
    EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
    testNotifier.fireTestStarted();
    try {
      Statement statement = classBlock(notifier);
      statement.evaluate();
    } catch (AssumptionViolatedException e) {
      testNotifier.addFailedAssumption(e);
    } catch (StoppedByUserException e) {
      throw e;
    } catch (Throwable e) {
      testNotifier.addFailure(e);
    } finally {
      testNotifier.fireTestFinished();
      System.out.println("Finished testing. Generating report.");
      MutationReport.addScriptNames(super.scriptsUnderTest);
      MutationReport.generateMutantReport();
    }
  }

  @Override
  protected List<TestRule> getTestRules(Object target) {
    List<TestRule> rules = new ArrayList<>(super.getTestRules(target));
    HiveRunnerRule hiveRunnerRule = (HiveRunnerRule) rules.get(rules.size() - 4);
    rules.add(rules.size() - 1, new MutantSwarmRule(hiveRunnerRule));
    // System.out.println("Rules: " + rules);
    return rules;
  }

}
