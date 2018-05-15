package com.klarna.hiverunner.mutantswarm;

import java.util.List;

import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.klarna.hiverunner.HiveShellContainer;
import com.klarna.hiverunner.StandaloneHiveRunner;

/** A rule that executes the scripts under test. */
public class HiveRunnerRule implements TestRule {

  private final StandaloneHiveRunner runner;
  private final Object target;
  private final TemporaryFolder testBaseDir;

  private List<String> scriptsUnderTest;

  public HiveRunnerRule(StandaloneHiveRunner runner, Object target, TemporaryFolder testBaseDir) {
    this.runner = runner;
    this.target = target;
    this.testBaseDir = testBaseDir;
  }

  public void setScriptsUnderTest(List<String> scriptsUnderTest) {
    this.scriptsUnderTest = scriptsUnderTest;
  }

  List<String> getScriptsUnderTest() {
    return scriptsUnderTest;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    System.out.println("running hive runner rule apply");
    return new HiveRunnerRuleStatement(runner, target, base, testBaseDir);
  }

  class HiveRunnerRuleStatement extends Statement {

    private Object target;
    private Statement base;
    private TemporaryFolder testBaseDir;
    private StandaloneHiveRunner runner;

    public HiveRunnerRuleStatement(
        StandaloneHiveRunner runner,
        Object target,
        Statement base,
        TemporaryFolder testBaseDir) {
      this.runner = runner;
      this.target = target;
      this.base = base;
      this.testBaseDir = testBaseDir;
    }

    @Override
    public void evaluate() throws Throwable {
      HiveShellContainer container = runner.evaluateStatement(scriptsUnderTest, target, testBaseDir, base);
      scriptsUnderTest = container.getScriptsUnderTest();
      // System.out.println("Ran HiveRunner test scripts: " + scriptsUnderTest);
    }

  }
}
