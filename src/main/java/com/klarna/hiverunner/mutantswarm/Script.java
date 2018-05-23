package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

public class Script {

  private String name = "";
  private String text = "";
  private List<QueryStatement> statements = new ArrayList<>();
  private static int scriptCount = 0;

  Script(String text) {
    this.text = text;
    this.name = scriptCount + "";
    scriptCount++;
  }
  
  public String getText(){
    return text;
  }

  public void setName(String name) {
    this.name = name;
  }

  // public void setLines(List<Line> lines) {
  // this.lines = lines;
  // }


  public void addStatement(QueryStatement statement){
    statements.add(statement);
  }
  
  public String getName() {
    return name;
  }

  public List<QueryStatement> getStatements() {
    return statements;
  }
  
  public QueryStatement getStatement(int index){
    return statements.get(index);
  }
  
  public int getStatementCount(){
    return statements.size();
  }

}
