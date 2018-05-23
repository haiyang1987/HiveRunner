package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

public class QueryStatement {

  private String text;
  private List<Line> lines = new ArrayList<>();
  
  QueryStatement(String text) {
    super();
    this.text = text;
  }
  
  public void addLine(Line line){
    lines.add(line);
  }
  
  public String getText(){
    return text;
  }
  
  public Line getLine(int lineNum){
    return lines.get(lineNum);
  }
}
