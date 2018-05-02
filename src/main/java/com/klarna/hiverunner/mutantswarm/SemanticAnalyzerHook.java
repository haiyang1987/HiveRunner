package com.klarna.hiverunner.mutantswarm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.HiveSemanticAnalyzerHook;
import org.apache.hadoop.hive.ql.parse.HiveSemanticAnalyzerHookContext;
import org.apache.hadoop.hive.ql.parse.SemanticException;

public class SemanticAnalyzerHook implements HiveSemanticAnalyzerHook {

  private static int index = -1;
  private static String originalQuery;
  private static List<String> mutations = new ArrayList<>();
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");

  public SemanticAnalyzerHook() {}

  @Override
  public ASTNode preAnalyze(HiveSemanticAnalyzerHookContext context, ASTNode ast) throws SemanticException {
    return ast;
    // String command = context.getCommand();
    //
    // if (canMutate(command)) {
    // originalQuery = command;
    // Mutator mutator = new Mutator();
    // mutations = mutator.generateMutations(command);
    // if (mutations == null || index == -1) {
    // return ast;
    // } else if (index < mutations.size()) {
    // return mutations.get(index);
    // }
    // }
    // return ast;
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

  public static String getMutation(int index) {
    return mutations.get(index);
  }

  public static int getMutationListSize() {
    if (mutations == null) {
      return 0;
    }
    return mutations.size();
  }

  public static void incrementIndex() {
    index++;
  }

  public static void resetIndex() {
    index = -1;
  }

  public static String getOriginalQuery() {
    return originalQuery;
  }

  @Override
  public void postAnalyze(HiveSemanticAnalyzerHookContext context, List<Task<? extends Serializable>> rootTasks)
    throws SemanticException {
    // TODO Auto-generated method stub
  }

}
