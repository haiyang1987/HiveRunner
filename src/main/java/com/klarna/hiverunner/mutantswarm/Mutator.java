package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.lib.Node;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;

import com.klarna.hiverunner.sql.ASTConverter;

public class Mutator {

  private final List<ASTNode> mutations;
  private final List<String> mutatedStrings;

  public Mutator() {
    mutations = new ArrayList<ASTNode>();
    mutatedStrings = new ArrayList<String>();
  }

  public List<String> generateMutations(String query) {
    mutantList(getAST(query));
    ASTConverter converter = new ASTConverter(false);
    for (ASTNode mutant : mutations) {
      mutatedStrings.add(converter.treeToQuery(mutant));
    }
    return mutatedStrings;
  }

  public void mutantList(ASTNode ast) {
    generateMultipleMutants(ast);
    ArrayList<Node> children = ast.getChildren();
    if (children != null) {
      for (Node child : children) {
        mutantList((ASTNode) child);
      }
    }
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

  private void generateMultipleMutants(ASTNode ast) {
    switch (ast.getToken().getType()) { // returns an int value

    case HiveParser.EQUAL:
      // mutations.add(mutateTree(ast, HiveParser.NOTEQUAL, "<>"));
      // mutations.add(mutateTree(ast, HiveParser.GREATERTHAN, ">"));
//      mutations.add(mutateTree(ast, HiveParser.GREATERTHANOREQUALTO, ">="));
       mutations.add(mutateTree(ast, HiveParser.LESSTHAN, "<"));
      mutations.add(mutateTree(ast, HiveParser.LESSTHANOREQUALTO, "<="));

      // mutations.add(mutateTree(ast, HiveParser.LESSTHAN, "<"));
//      mutations.add(mutateTree(ast, HiveParser.GREATERTHAN, ">"));
    }
  }

  private ASTNode mutateTree(ASTNode oldNode, int newTokenType, String newTokenText) {
    ASTNode tree = copyAST(getRoot(oldNode));
    Token token = new CommonToken(newTokenType, newTokenText);
    ASTNode newNode = new ASTNode(token);
    return replaceNode(tree, oldNode, newNode);
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
