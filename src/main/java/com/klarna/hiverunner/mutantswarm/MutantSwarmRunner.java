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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klarna.hiverunner.StandaloneHiveRunner;

public class MutantSwarmRunner extends StandaloneHiveRunner {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MutantSwarmRunner.class);

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
      LOGGER.debug("Finished testing. Generating report.");
      MutationReport.addScriptNames(super.scriptsUnderTest);
      MutationReport.generateMutantReport();
    }
  }

  @Override
  protected List<TestRule> getTestRules(Object target) {
    List<TestRule> rules = new ArrayList<>(super.getTestRules(target));
    HiveRunnerRule hiveRunnerRule = getHiveRunnerRule(rules);
    rules.add(rules.size() - 1, new MutantSwarmRule(hiveRunnerRule));
    return rules;
  }
  
  protected HiveRunnerRule getHiveRunnerRule(List<TestRule> rules){
    HiveRunnerRule hrRule = null;
    for (TestRule rule : rules){
      if (rule instanceof HiveRunnerRule){
        hrRule = (HiveRunnerRule) rule;
      }
    }
    return hrRule;
  }
  
}
