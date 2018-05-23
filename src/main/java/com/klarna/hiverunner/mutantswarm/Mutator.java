package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.lib.Node;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;

import com.klarna.hiverunner.sql.ASTConverter;
import com.klarna.hiverunner.sql.StatementsSplitter;

public class Mutator {

  private List<Mutant> mutantList;
  // private String script;
  private Script script;
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");
  private ASTConverter converter;

  // private int queryIndex;
  // private int scriptNumber;
  private int statementNumber = 0;

  // private List<String> queryList;
  StringBuilder scriptBuilder = new StringBuilder();

  public Mutator(Script script) {
    mutantList = new ArrayList<>();
    this.script = script;
    converter = new ASTConverter(false);
  }
  // public Mutator(String script, int scriptNumber) {
  // mutantList = new ArrayList<>();
  // this.script = script;
  // this.scriptNumber = scriptNumber;
  // converter = new ASTConverter(false);
  // }

  // pass in the original script
  // split the script into statements
  // for each statement generate mutants
  // - turn into tree
  // - for each token in tree
  // - - create a new mutant
  // - - add all mutants to a list
  // return the list of mutants

  // public List<Mutant> mutateScript() {
  // queryList = StatementsSplitter.splitStatementsRemoveComments(script);
  // for (queryIndex = 0; queryIndex < queryList.size(); queryIndex++) {
  // String statement = queryList.get(queryIndex);
  // if (canMutate(statement)) {
  // statement = checkForVariableSubstitution(statement);
  // mutateTreeNode(getAST(statement)); // mutate each statement in the script
  // }
  // scriptBuilder.append(statement);
  // }
  // return mutantList;
  // }

  public List<Mutant> mutateScript() {
    // go through every statement in the script
    // count the number of '\n' chars and create Lines + words accordingly
    // generate mutants for the statement
    // add mutants to lines
    // set up word mutation status
    System.out.println("*Mutator: mutating");
    // statementNumber = 1;
    for (QueryStatement statement : script.getStatements()) {
      String statementText = statement.getText();
      int lineCount = 1 + StringUtils.countMatches(statementText, "\n");
      String[] lines = statementText.split("\n");
      System.out.println("Found " + lines.length + " lines");
      for (String lineText : lines) {
        System.out.println("Generating line object for - " + lineText);
        Line line = new Line(lineCount, lineText);
        String[] words = lineText.split(" ");
        for (String wordText : words) {

          Word word = new Word(wordText);
          line.addWord(word);
          System.out.println("Generating word object - " + wordText);
        }
        statement.addLine(line);
        System.out.println("adding line to statement");
      }

      if (canMutate(statementText)) {
        System.out.println("mutating statement ");
        statementText = checkForVariableSubstitution(statementText);
        mutateTreeNode(getAST(statementText)); // mutate each statement in the script
      }
      scriptBuilder.append(statementText);
      statementNumber++;
    }
    return mutantList;
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

  public void mutateTreeNode(ASTNode ast) {
    generateMutants(ast);
    ArrayList<Node> children = ast.getChildren();
    if (children != null) {
      for (Node child : children) {
        mutateTreeNode((ASTNode) child);
      }
    }
  }

  public void generateMutants(ASTNode ast) {
    switch (ast.getToken().getType()) { // returns an int value

    case HiveParser.EQUAL:
      createMutant("<>", HiveParser.NOTEQUAL, ast, "=");
      createMutant(">", HiveParser.GREATERTHAN, ast, "=");
      // createMutant(">=", HiveParser.GREATERTHANOREQUALTO, ast, "=");
      // createMutant("<", HiveParser.LESSTHAN, ast, "=");
      // createMutant("<=", HiveParser.LESSTHANOREQUALTO, ast, "=");
    }
  }

  private void createMutant(String text, int token, ASTNode astToReplace, String oldNodeText) {
    String mutantStatement = mutateTree(astToReplace, token, text);
    List<QueryStatement> restOfStatements = new ArrayList<>();
    if (statementNumber < script.getStatementCount() - 1) { // counter starts from 0
      restOfStatements = script.getStatements().subList(statementNumber + 1, script.getStatementCount());
    }

    String mutatedScript = buildMutantScript(scriptBuilder.toString(), mutantStatement, restOfStatements);

    // need the rest of the statements to build up the script.

    // get the difference between the two strings
    // then find the number of '\n' chars between 0 and this index to find the line number
    // TEST THIS ********

    String original = normalise(script.getText());
    String mutated = normalise(mutatedScript);
    int indexOfDiff = StringUtils.indexOfDifference(original, mutated);
    // System.out.println("og: "
    // + original
    // + "\nmutated: "
    // + mutated
    // + "\nindex of diff: "
    // + indexOfDiff
    // + " : "
    // + mutated.substring(indexOfDiff, mutated.length()));
    int lineNumber = lineNumber(mutated, indexOfDiff);
    System.out.println("Line " + lineNumber + ": Mutated " + oldNodeText + " to " + text);

    Line line = script.getStatement(statementNumber).getLine(lineNumber);

    System.out.println("line text: " + line.getText() + ". looking for word: " + oldNodeText);

    Word word = line.getWord(oldNodeText); // could throw a null pointer exception *****

    Mutant mutant = new Mutant(text, mutatedScript, lineNumber, word);
    line.addMutant(mutant);
    System.out.println("Added mutant to line");
    mutantList.add(mutant); // returned to mutantswarmrule
  }

  private int lineNumber(String script, int index) {
    script = script.substring(0, index);
    int lineNum = StringUtils.countMatches(script, "\n");

    return 1 + lineNum;
  }

  private String buildMutantScript(String startOfScript, String mutant, List<QueryStatement> restOfScript) {
    StringBuilder mutantScriptBuilder = new StringBuilder();
    mutantScriptBuilder.append(startOfScript);
    mutantScriptBuilder.append(mutant + "; ");
    for (QueryStatement statement : restOfScript) {
      mutantScriptBuilder.append(statement.getText() + "; ");
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

  private ASTNode getAST(String query) {
    try {
      ASTNode tree = ParseUtils.parse(query);
      return tree;
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String mutateTree(ASTNode oldNode, int newTokenType, String newTokenText) {
    ASTNode tree = copyAST(getRoot(oldNode));
    Token token = new CommonToken(newTokenType, newTokenText);
    ASTNode newNode = new ASTNode(token);

    return converter.treeToQuery(replaceNode(tree, oldNode, newNode));
  }

  private ASTNode getRoot(ASTNode ast) {
    List<? extends Tree> ancestors = ast.getAncestors();
    if (ancestors != null) {
      ASTNode node = (ASTNode) ancestors.get(0);
      if (node.getToken() == null && node.getChildCount() > 0) {
        ast = (ASTNode) node.getChild(0);
      }
    }
    return ast;
  }

  private ASTNode copyAST(ASTNode ast) {
    ASTNode copy = (ASTNode) ast.dupNode();
    copy.setParent(ast.getParent());

    List<Node> children = ast.getChildren();
    if (children != null) {
      for (Node child : children) {
        ASTNode astChild = (ASTNode) child;
        copy.insertChild(astChild.childIndex, copyAST(astChild));
      }
    }
    return copy;
  }

  private ASTNode replaceNode(ASTNode currentNode, ASTNode nodeToReplace, ASTNode newNode) {
    Token treeToken = currentNode.getToken();
    Token replaceNodeToken = nodeToReplace.getToken();

    if ((treeToken.getType() == replaceNodeToken.getType())
        && treeToken.getTokenIndex() == replaceNodeToken.getTokenIndex()) {

      if (currentNode.getParent() != null) { // if its not the root node
        Tree parentNode = currentNode.getParent();
        int nodeIndex = currentNode.getChildIndex();
        newNode.setParent(parentNode);
        parentNode.replaceChildren(nodeIndex, nodeIndex, newNode);
      }
      List<Node> childNodes = currentNode.getChildren();
      if (childNodes != null) {
        newNode.addChildren(childNodes);
        for (Node child : childNodes) {
          ((ASTNode) child).setParent(newNode);
        }
      }
    } else {
      List<Node> childNodes = currentNode.getChildren();
      if (childNodes != null) {
        for (Node child : childNodes) {
          replaceNode((ASTNode) child, nodeToReplace, newNode);
        }
      }
    }
    return currentNode;
  }

  private static String normalise(String query) {
    query = query.replaceAll("!=", "<>"); // equivalent operator
    query = query.replaceAll("\n ", "\n"); // want to preserve newlines
    query = query.replaceAll(" \n", "\n"); // want to preserve newlines
    query = normaliseCharacters(query);
    query = query.replaceAll("\\s\\s+", " "); // removing excess whitespaces
    return query;
  }

  private static String normaliseCharacters(String query) {
    // adds spaces around every character
    Matcher matcher = Pattern.compile("([\\'\"\\)\\(\\}\\{\\<\\>\\=\\+\\-\\,\\*])").matcher(query);
    while (matcher.find()) {
      String character = matcher.group(1);
      query = query.replaceAll("\\" + character, " \\" + character + " ");
    }
    return query;
  }

}
