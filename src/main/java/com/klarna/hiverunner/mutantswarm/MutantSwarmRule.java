package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.klarna.hiverunner.sql.StatementsSplitter;

/** A rule to run a standard HR test, and then once again for each mutant. */
public class MutantSwarmRule implements TestRule {

  private List<String> originalScripts = new ArrayList<>();
  private List<List<String>> mutants;
  private List<List<String>> aliveMutations = new ArrayList<>();
  private HiveRunnerRule hiveRunnerRule;
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");

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
      mutants = generateMutants(originalScripts);

      System.out.println("Running mutant tests");
      for (int i = 0; i < mutants.size(); i++) {
        System.out.println("Running mutant test: " + mutants.get(i));
        hiveRunnerRule.setScriptsUnderTest(mutants.get(i));
        try {
          base.evaluate();
          // catch mutants *****
          aliveMutations.add(mutants.get(i));
          System.out.println("mutant survived - bad");
        } catch (AssertionError e) {
          // make note of killed mutants *****
          System.out.println("mutant killed - " + e.getMessage());
        }
        // need to have a try catch around this to catch any AssertionErrors - good
        // not catching them is bad - mutant survived
      }

      // generate mutation report
      MutationReport.generateHtmlMutantReport(originalScripts, aliveMutations);
    }
  }

  private List<List<String>> generateMutants(List<String> underTest) {
    // add script mutation functionality
    List<List<String>> mutatedScripts = new ArrayList<>();

    for (int i = 0; i < underTest.size(); i++) {
      String script = underTest.get(i);
      // will need to split the string into statements and then generate mutations
      List<String> scriptMutations = generateMutatedScripts(script);

      for (String mutation : scriptMutations) {
        List<String> mutantsUnderTest = new ArrayList<>(underTest);
        mutantsUnderTest.set(i, mutation); // replace original script with mutation
        mutatedScripts.add(mutantsUnderTest);
      }
    }
    return mutatedScripts;
  }

  private List<String> generateMutatedScripts(String script) {
    System.out.println("generating mutant scripts");
    List<String> scriptMutations = new ArrayList<>();
    StringBuilder scriptBuilder = new StringBuilder();

    List<String> statements = StatementsSplitter.splitStatementsRemoveComments(script);
    for (int i = 0; i < statements.size(); i++) {
      String statement = statements.get(i);
      if (canMutate(statement)) {
        List<String> statementMutations = mutateQuery(statement);
        for (String mutant : statementMutations) {
          // build up a new script
          scriptMutations
              .add(buildMutatedScript(scriptBuilder.toString(), mutant, statements.subList(i + 1, statements.size())));
        }
      }
      scriptBuilder.append(statement + "; "); // add query to string builder after generating mutated scripts
    }
    return scriptMutations;
  }

  private String buildMutatedScript(String startOfScript, String mutant, List<String> restOfScript) {
    // System.out.println("building mutant script. Start of string is; " + startOfScript);
    StringBuilder mutantScriptBuilder = new StringBuilder();
    mutantScriptBuilder.append(startOfScript);
    mutantScriptBuilder.append(mutant + "; ");
    for (String statement : restOfScript) {
      mutantScriptBuilder.append(statement + "; ");
    }
    return mutantScriptBuilder.toString();
  }

  private boolean canMutate(String query) {
    if (query.equals(" ") || query.equals("")) {
      return false;
    }
    String startsWith = query;
    if (query.contains(" ")) { // get the first word of the query
      startsWith = query.substring(0, query.indexOf(" "));
    }
    return !queriesToIgnore.contains(startsWith.toLowerCase());
  }

  private List<String> mutateQuery(String query) {
    query = checkForVariableSubstitution(query);
    Mutator mutator = new Mutator();
    return mutator.generateMutations(query);
  }

  private String checkForVariableSubstitution(String query) {
    Pattern pattern = Pattern.compile("(.*)(\\$\\{.*\\})(.*)");
    Matcher matcher = pattern.matcher(query);
    while (matcher.find()) {
      query = query.replace(matcher.group(2), "varSub");
      matcher = pattern.matcher(query);
    }
    return query;
  }
}
