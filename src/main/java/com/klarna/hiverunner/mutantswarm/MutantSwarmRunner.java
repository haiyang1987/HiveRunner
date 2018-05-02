package com.klarna.hiverunner.mutantswarm;

import static org.reflections.ReflectionUtils.withAnnotation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;
import org.apache.hadoop.hive.ql.security.authorization.DefaultHiveMetastoreAuthorizationProvider;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.klarna.hiverunner.HiveServerContainer;
import com.klarna.hiverunner.HiveServerContext;
import com.klarna.hiverunner.HiveShell;
import com.klarna.hiverunner.HiveShellContainer;
import com.klarna.hiverunner.StandaloneHiveRunner;
import com.klarna.hiverunner.StandaloneHiveServerContext;
import com.klarna.hiverunner.annotations.HiveProperties;
import com.klarna.hiverunner.annotations.HiveSQL;
import com.klarna.hiverunner.builder.HiveShellBuilder;
import com.klarna.hiverunner.sql.StatementsSplitter;
import com.klarna.reflection.ReflectionUtils;

public class MutantSwarmRunner extends StandaloneHiveRunner {

  private static List<ASTNode> activeMutations = new ArrayList<ASTNode>();
  private static List<ASTNode> killedMutations = new ArrayList<ASTNode>();
  // private static final TemporaryFolder testBaseDir = new TemporaryFolder();
  // private static final File testBaseDir = new File("../hiverunner/src/test/resources/mutantSwarmTest");
  private final List<Path> scriptsUnderTest = new ArrayList<>();
  private List<List<String>> mutatedScriptsUnderTest = new ArrayList<>();
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");

  public MutantSwarmRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

  @Override
  protected void loadAnnotatedProperties(Object testCase, HiveShellBuilder workFlowBuilder) {
    for (Field hivePropertyField : ReflectionUtils.getAllFields(testCase.getClass(),
        withAnnotation(HiveProperties.class))) {
      Preconditions.checkState(ReflectionUtils.isOfType(hivePropertyField, Map.class),
          "Field annotated with @HiveProperties should be of type Map<String, String>");
      workFlowBuilder.putAllProperties(ReflectionUtils.getFieldValue(testCase, hivePropertyField.getName(), Map.class));
    }
    // add in extra hive properties here
    @SuppressWarnings("unchecked")
    Map<String, String> hiveProperties = MapUtils.putAll(new HashMap<String, String>(),
        new Object[] {
            HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER.varname,
            DefaultHiveMetastoreAuthorizationProvider.class.getName(),
            HiveConf.ConfVars.SEMANTIC_ANALYZER_HOOK.varname,
            SemanticAnalyzerHook.class.getName() });
    workFlowBuilder.putAllProperties(hiveProperties);
  }

  /**
   * Drives the unit test.
   */
  @Override
  protected void evaluateStatement(Object target, TemporaryFolder temporaryFolder, Statement base) throws Throwable {
    container = null;
    FileUtil.setPermission(temporaryFolder.getRoot(), FsPermission.getDirDefault());
    try {
      LOGGER.info("Setting up {} in {}", getName(), temporaryFolder.getRoot().getAbsolutePath());
      System.out.println("running evaluate statement");
      container = createHiveServerContainer(target, temporaryFolder);
      base.evaluate();
    } finally {
      tearDown();
    }

    // **** TODO ****
    // run the mutated scripts here
    System.out.println("finished first test");

    // File testBaseDir = new File("../hiverunner/src/test/resources/mutantSwarmTest");

    for (List<String> mutatedScriptList : mutatedScriptsUnderTest) {
      try {
        for (File f : temporaryFolder.getRoot().listFiles()) {
          FileUtils.deleteQuietly(f);
        }
        // TemporaryFolder folder = new TemporaryFolder(temporaryFolder.getRoot().getParentFile());
        // FileUtil.setPermission(folder.getRoot(), FsPermission.getDirDefault());

        // go through each of the diff lists of scripts
        // create a new container
        // run base.evaluate to run the test again
        container = createMutatedHiveServerContainer(target, temporaryFolder, mutatedScriptList);
        // runMutatedScripts(mutatedScriptList, target);
        base.evaluate();
      } finally {
        tearDown();
      }
    }
  }

  private HiveShellContainer createMutatedHiveServerContainer(
      Object testCase,
      TemporaryFolder baseDir,
      List<String> mutatedScriptList)
    throws IOException {

    HiveServerContext context = new StandaloneHiveServerContext(baseDir, config);
    final HiveServerContainer hiveTestHarness = new HiveServerContainer(context);

    HiveShellBuilder hiveShellBuilder = new HiveShellBuilder();
    hiveShellBuilder.setCommandShellEmulation(config.getCommandShellEmulation());

    HiveShellField shellSetter = loadScriptUnderTest(testCase, hiveShellBuilder);
    hiveShellBuilder.setHiveServerContainer(hiveTestHarness);
    loadAnnotatedResources(testCase, hiveShellBuilder);
    loadAnnotatedProperties(testCase, hiveShellBuilder);
    loadAnnotatedSetupScripts(testCase, hiveShellBuilder);
    System.out.println("running the hive shell");

    HiveShellContainer shell = hiveShellBuilder.buildShell(mutatedScriptList);
    shellSetter.setShell(shell);
    if (shellSetter.isAutoStart()) {
      shell.start();
    }
    return shell;
  }

  // @Override
  // protected List<TestRule> getTestRules(final Object target) {
  // final TemporaryFolder testBaseDir = new TemporaryFolder();
  //
  // TestRule mutantRunnerRule = new TestRule() {
  // @Override
  // public Statement apply(final Statement base, Description description) {
  // Statement statement = new Statement() {
  // @Override
  // public void evaluate() throws Throwable {
  // // try { // <- need for catching mutations
  // System.out.println("evaluating");
  // evaluateStatement(target, testBaseDir, base);
  // // System.out.println("Assertion passed - bad");
  // // } catch (AssertionError e) {
  // // System.out.println("Assertion failed - good");
  // // }
  // }
  // };
  // return statement;
  // }
  // };
  //
  // /*
  // * Note that rules will be executed in reverse order to how they're added.
  // */
  // List<TestRule> rules = new ArrayList<>();
  // rules.addAll(super.getTestRules(target));
  // rules.add(mutantRunnerRule);
  // rules.add(testBaseDir);
  // rules.add(ThrowOnTimeout.create(config, getName()));
  //
  // /*
  // * Make sure hive runner config rule is the first rule on the list to be executed so that any subsequent statements
  // * has access to the final config.
  // */
  // rules.add(getHiveRunnerConfigRule(target));
  // return rules;
  // }

  // *********************
  // if this works, will not need the 'getTestRules' method above
  /**
   * Traverses the test case annotations. Will inject a HiveShell in the test case that envelopes the HiveServer.
   */
  @Override
  protected HiveShellContainer createHiveServerContainer(final Object testCase, TemporaryFolder baseDir)
    throws IOException {

    HiveServerContext context = new StandaloneHiveServerContext(baseDir, config);
    final HiveServerContainer hiveTestHarness = new HiveServerContainer(context);

    HiveShellBuilder hiveShellBuilder = new HiveShellBuilder();
    hiveShellBuilder.setCommandShellEmulation(config.getCommandShellEmulation());

    HiveShellField shellSetter = loadScriptUnderTest(testCase, hiveShellBuilder);
    hiveShellBuilder.setHiveServerContainer(hiveTestHarness);
    loadAnnotatedResources(testCase, hiveShellBuilder);
    loadAnnotatedProperties(testCase, hiveShellBuilder);
    loadAnnotatedSetupScripts(testCase, hiveShellBuilder);
    System.out.println("running the hive shell");

    HiveShellContainer shell = hiveShellBuilder.buildShell();
    shellSetter.setShell(shell);
    if (shellSetter.isAutoStart()) {
      shell.start();
    }

    List<String> scriptsAsString = new ArrayList<>(); // allows mutated verions 'scriptUnderTest'
    List<List<String>> scriptsAsList = new ArrayList<>(); // allows the script statements to be mutated

    for (Path script : scriptsUnderTest) {
      List<String> scriptStatements = readScriptContent(script);
      scriptsAsList.add(scriptStatements);
      StringBuilder scriptString = new StringBuilder();
      for (String statement : scriptStatements) {
        scriptString.append(statement + "; ");
      }
      scriptsAsString.add(scriptString.toString());
      // System.out.println("got the script content - " + scriptString.toString());
    }

    // * adds the original scripts to the list
    // List<List<String>> mutatedScriptsUnderTest = Arrays.asList(scriptsAsString); // add original list at the start
    // mutatedScriptsUnderTest.addAll(generateMutatedScriptList(scriptsAsString, scriptsAsList));

    mutatedScriptsUnderTest = generateMutatedScriptList(scriptsAsString, scriptsAsList);

    return shell;
  }

  private void runMutatedScripts(List<String> mutatedScriptList, final Object testCase) throws IOException {
    System.out.println("running mutated scripts");

    File testBaseDir = new File("../hiverunner/src/test/resources/mutantSwarmTest");
    TemporaryFolder newFolder = new TemporaryFolder(testBaseDir);
    newFolder.create(); // ************** apparently shouldnt do this
    // FileUtil.setPermission(testBaseDir, FsPermission.getDirDefault());

    HiveServerContext context = new StandaloneHiveServerContext(newFolder, config);
    // HiveServerContext context = new StandaloneHiveServerContext(newFolder, config);

    final HiveServerContainer hiveTestHarness = new HiveServerContainer(context);

    HiveShellBuilder hiveShellBuilder = new HiveShellBuilder();
    hiveShellBuilder.setCommandShellEmulation(config.getCommandShellEmulation());

    HiveShellField shellSetter = loadScriptUnderTest(testCase, hiveShellBuilder); // ***** dont rly want to do this

    hiveShellBuilder.setHiveServerContainer(hiveTestHarness);
    loadAnnotatedResources(testCase, hiveShellBuilder);
    loadAnnotatedProperties(testCase, hiveShellBuilder);
    loadAnnotatedSetupScripts(testCase, hiveShellBuilder);

    HiveShellContainer shell = hiveShellBuilder.buildShell(mutatedScriptList); // <- important. sets the scripts
    shellSetter.setShell(shell);
    if (shellSetter.isAutoStart()) {
      shell.start();
      // this calls the start method inside HiveShellBase
      // here it tried to set up the initial values inside the hiveServerContainer
      // so may need to figure something out here wrt the tmp dir
    }
  }

  private List<List<String>> generateMutatedScriptList(
      List<String> scriptString,
      List<List<String>> scriptStatementList) {
    System.out.println("generating mutant scripts");
    List<List<String>> mutatedScriptsUnderTest = new ArrayList<>();
    // System.out.println("size of script content list - " + scriptContentList.size());
    for (int i = 0; i < scriptStatementList.size(); i++) {
      List<String> scriptMutations = generateScriptMutations(scriptStatementList.get(i));
      for (String scriptMutation : scriptMutations) {
        List<String> mutatedScriptList = scriptString;
        mutatedScriptList.set(i, scriptMutation); // replaces the original script with the mutated version
        mutatedScriptsUnderTest.add(mutatedScriptList);
      }
    }
    return mutatedScriptsUnderTest;
  }

  private List<String> readScriptContent(Path script) {
    List<String> scriptStatements = new ArrayList<>();
    try {
      String fileContents = "";
      fileContents = Files.asCharSource(script.toFile(), StandardCharsets.UTF_8).read();
      scriptStatements = StatementsSplitter.splitStatementsRemoveComments(fileContents);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return scriptStatements;
  }

  private List<String> generateScriptMutations(List<String> queryStatements) {
    List<String> scriptMutations = new ArrayList<>();
    StringBuilder scriptBuilder = new StringBuilder();
    for (int i = 0; i < queryStatements.size(); i++) {
      String query = queryStatements.get(i);
      if (canMutate(query)) {
        // System.out.println("can mutate");
        List<String> mutations = mutateQuery(query);
        for (String mutant : mutations) {
          // System.out.println("mutant - " + mutant);
          scriptMutations
              .add(buildMutatedScript(scriptBuilder, mutant, queryStatements.subList(i, queryStatements.size())));
        }
        scriptBuilder.append(query); // add query to string builder after generating mutated scripts
      }
    }
    return scriptMutations;
  }

  private String buildMutatedScript(StringBuilder mutatedScript, String mutant, List<String> restOfScript) {
    mutatedScript.append(mutant + "; ");
    for (String statement : restOfScript) {
      mutatedScript.append(statement + "; ");
    }
    return mutatedScript.toString();
  }

  private List<String> mutateQuery(String query) {
    query = checkForVariableSubstitution(query);
    ASTNode tree;
    try {
      tree = ParseUtils.parse(query);
      Mutator mutator = new Mutator();
      return mutator.generateMutations(query);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
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

  // only want to deal with 'update table', 'create table', 'select', 'insert', 'delete'
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

  @Override
  protected HiveShellField loadScriptUnderTest(final Object testCaseInstance, HiveShellBuilder hiveShellBuilder) {
    try {
      Set<Field> fields = ReflectionUtils.getAllFields(testCaseInstance.getClass(), withAnnotation(HiveSQL.class));

      Preconditions.checkState(fields.size() == 1, "Exact one field should to be annotated with @HiveSQL");

      final Field field = fields.iterator().next();
      HiveSQL annotation = field.getAnnotation(HiveSQL.class);
      for (String scriptFilePath : annotation.files()) {
        Path file = Paths.get(Resources.getResource(scriptFilePath).toURI());
        assertFileExists(file);
        scriptsUnderTest.add(file);
      }

      Charset charset = annotation.encoding().equals("") ? Charset.defaultCharset()
          : Charset.forName(annotation.encoding());

      final boolean isAutoStart = annotation.autoStart();

      hiveShellBuilder.setScriptsUnderTest(scriptsUnderTest, charset);

      return new HiveShellField() {
        @Override
        public void setShell(HiveShell shell) {
          ReflectionUtils.setField(testCaseInstance, field.getName(), shell);
        }

        @Override
        public boolean isAutoStart() {
          return isAutoStart;
        }
      };
    } catch (Throwable t) {
      throw new IllegalArgumentException("Failed to init field annotated with @HiveSQL: " + t.getMessage(), t);
    }
  }

}
