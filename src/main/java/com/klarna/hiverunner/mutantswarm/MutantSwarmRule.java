package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** A rule to run a standard HR test, and then once again for each mutant. */
public class MutantSwarmRule implements TestRule {

  private List<String> originalScripts = new ArrayList<>();
  private static List<List<Mutant>> scriptMutations = new ArrayList<>();
  private HiveRunnerRule hiveRunnerRule;

  public MutantSwarmRule(HiveRunnerRule hiveRunnerRule) {
    this.hiveRunnerRule = hiveRunnerRule;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    System.out.println("mutant swarm apply method");
    return new MutantSwarmStatement(base);
  }

  class MutantSwarmStatement extends Statement {
    private Statement base;

    public MutantSwarmStatement(Statement base) {
      this.base = base;
    }

    @Override
    public void evaluate() throws Throwable {
      System.out.println("Running regular test");
      base.evaluate();
      originalScripts = hiveRunnerRule.getScriptsUnderTest();
      MutationReport.setOriginalScripts(originalScripts);
      if (scriptMutations.isEmpty()){
        System.out.println("generating mutants");
        generateMutants(originalScripts);
      }

      System.out.println("Running mutant tests");
      for (int i = 0; i < scriptMutations.size(); i++) {
        List<Mutant> mutants = scriptMutations.get(i);

        for (Mutant mutant : mutants) {
          System.out.println("Mutant: " + mutant.getMutatedScript());
          hiveRunnerRule.setScriptsUnderTest(generateMutatedScriptList(mutant, i));
          try {
            base.evaluate();
            System.out.println("mutant survived - bad");
          } catch (AssertionError e) {
            System.out.println("mutant killed - " + e.getMessage());
            mutant.setSurvived(false);
          }
        }
        MutationReport.addMutants(mutants);
      }
      MutationReport.finishedAddingMutants(true);
    }
  }

  private List<String> generateMutatedScriptList(Mutant mutant, int index) {
    List<String> mutatedScriptList = new ArrayList<>(originalScripts);
    mutatedScriptList.set(index, mutant.getMutatedScript());
    return mutatedScriptList;
  }

  private void generateMutants(List<String> underTest) {
    for (String script : underTest) {
      Mutator mutator = new Mutator(script);
      scriptMutations.add(mutator.mutateScript());
    }
  }

}
