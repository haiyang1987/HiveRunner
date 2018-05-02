package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MutantRunnerRule implements TestRule {

  private static List<ASTNode> activeMutations = new ArrayList<ASTNode>();
  private static List<ASTNode> killedMutations = new ArrayList<ASTNode>();

  public MutantRunnerRule() {}

  private static class RepeatStatement extends Statement {
    private final Statement statement;

    public RepeatStatement(Statement statement) {
      this.statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
      try {
        statement.evaluate();
        System.out.println("Assertion passed - bad");
      } catch (AssertionError e) {
        System.out.println("Assertion failed - good");
      }
    }
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Statement result = statement;
    result = new RepeatStatement(statement);
    return result;
  }

}
