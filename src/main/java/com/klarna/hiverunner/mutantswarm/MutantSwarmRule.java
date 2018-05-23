package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klarna.hiverunner.sql.ASTConverter;
import com.klarna.hiverunner.sql.StatementsSplitter;

/** A rule to run a standard HR test, and then once again for each mutant. */
public class MutantSwarmRule implements TestRule {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MutantSwarmRule.class);

  private List<String> originalScripts = new ArrayList<>();
  private static List<List<Mutant>> scriptMutations = new ArrayList<>();
  private static List<List<String>> mutatedScriptList = new ArrayList<>();
  private HiveRunnerRule hiveRunnerRule;

  public MutantSwarmRule(HiveRunnerRule hiveRunnerRule) {
    this.hiveRunnerRule = hiveRunnerRule;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    LOGGER.debug("mutant swarm apply method");
    return new MutantSwarmStatement(base);
  }

  class MutantSwarmStatement extends Statement {
    private Statement base;

    public MutantSwarmStatement(Statement base) {
      this.base = base;
    }

    @Override
    public void evaluate() throws Throwable {
      LOGGER.debug("Running regular test");
      System.out.println("*running regular test");
      base.evaluate();
      setUpScripts();

      LOGGER.debug("Running mutant tests");
      System.out.println("*running mutant test");
      for (int i = 0; i < scriptMutations.size(); i++) {
        List<Mutant> mutants = scriptMutations.get(i);

        for (Mutant mutant : mutants) {
          LOGGER.debug("Mutant: " + mutant.getMutatedScript());
          hiveRunnerRule.setScriptsUnderTest(mutatedScriptList.get(i));
          try {
            base.evaluate();
            LOGGER.debug("Mutant survived the test - bad");
            System.out.println("*mutant survived - bad");
          } catch (AssertionError e) {
            LOGGER.debug("Mutant successfully killed by test - " + e.getMessage());
            System.out.println("*mutant killed");
            mutant.setKilled();
          }
        }
      }
    }
  }

  private void setUpScripts() {
    List<String> scripts = hiveRunnerRule.getScriptsUnderTest();
    System.out.println("*setting up " + scripts.size() + " scripts");
    for (int i = 0; i < scripts.size(); i++) {
      String scriptString = scripts.get(i);
      Script script = new Script(scriptString);

      List<String> statements = StatementsSplitter.splitStatementsRemoveComments(scriptString);
      for (String statement : statements) {
        statement = formatQuery(statement);
        QueryStatement queryStatement = new QueryStatement(statement);
        script.addStatement(queryStatement);
      }

      // script full of statements
      // parse to the mutator to generate mutations for each statement

      originalScripts.add(scriptString);
      generateMutants(script, i);

    }
  }

  private String formatQuery(String script) {
    ASTConverter converter = new ASTConverter(false);
    try {
      return converter.treeToQuery(ParseUtils.parse(script));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return script;
  }

  // private void generateMutants(String script, int index) {
  // if (mutatedScriptList.isEmpty()) {
  // Mutator mutator = new Mutator(script, index);
  // List<Mutant> mutants = mutator.mutateScript();
  // scriptMutations.add(mutants);
  //
  // // could set up mutation report here
  // MutationReport.setUpScript(script, mutants);
  //
  // generateMutatedScripts(mutants, index);
  // }
  // }

  private void generateMutants(Script script, int index) {
    if (mutatedScriptList.isEmpty()) {
      System.out.println("* generating mutants");
      // **** not sure if the index is needed
      Mutator mutator = new Mutator(script);
      List<Mutant> mutants = mutator.mutateScript();
      scriptMutations.add(mutants);

      generateMutatedScripts(mutants, index);
      MutationReport.addScript(script);
    }
  }

  private void generateMutatedScripts(List<Mutant> mutants, int index) {
    List<String> mutatedScript = new ArrayList<>(originalScripts);
    for (Mutant mutant : mutants) {
      mutatedScript.set(index, mutant.getMutatedScript());
      mutatedScriptList.add(mutatedScript);
    }
  }

}
