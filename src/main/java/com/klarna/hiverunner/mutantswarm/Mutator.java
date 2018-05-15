package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.lib.Node;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;

import com.klarna.hiverunner.sql.ASTConverter;
import com.klarna.hiverunner.sql.StatementsSplitter;

public class Mutator {

  private List<Mutant> mutantList;
  private String script;
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");
  private ASTConverter converter;

  private int statementIndex;
  private List<String> statementList;
  StringBuilder scriptBuilder = new StringBuilder();

  public Mutator(String script) {
    mutantList = new ArrayList<>();
    this.script = script;
    converter = new ASTConverter(false);
  }

  // pass in the original script
  // split the script into statements
  // for each statement generate mutants
  // - turn into tree
  // - for each token in tree
  // - - create a new mutant
  // - - add all mutants to a list
  // return the list of mutants

  public List<Mutant> mutateScript() {
    statementList = StatementsSplitter.splitStatementsRemoveComments(script);
    for (statementIndex = 0; statementIndex < statementList.size(); statementIndex++) {
      String statement = statementList.get(statementIndex);
      if (canMutate(statement)) {
        statement = checkForVariableSubstitution(statement);
        mutateTreeNode(getAST(statement)); // mutate each statement in the script
      }
      scriptBuilder.append(statement);
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
    String mutant = mutateTree(astToReplace, token, text);
    String mutatedScript = buildMutantScript(scriptBuilder.toString(), mutant,
        statementList.subList(statementIndex + 1, statementList.size()));

    mutantList.add(new Mutant(text, oldNodeText, mutatedScript, script));
  }

  private String buildMutantScript(String startOfScript, String mutant, List<String> restOfScript) {
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

}
