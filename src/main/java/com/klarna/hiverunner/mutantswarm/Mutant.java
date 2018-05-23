package com.klarna.hiverunner.mutantswarm;

public class Mutant {

  private String text;
//  private String originalText;
  private String mutatedScript;
  private boolean survived = true;
  private int lineNumber;
//  private int scriptNumber;
  private Word word;

  Mutant(String text, String mutatedScript, int lineNumber, Word word) {
    this.text = text;
    this.mutatedScript = mutatedScript;
    this.lineNumber = lineNumber;
    this.word = word;
    word.setSurvived();
  }

  public String getText() {
    return text;
  }

  public String getOriginalText() {
    return word.getText();
  }

  public String getMutatedScript() {
    return mutatedScript;
  }
  
  public void setKilled(){
    this.survived = false;
    word.setKilled();
  }

  public boolean hasSurvived() {
    return survived;
  }

  public String toString() {
    return text;
  }

  public int getLineNumber() {
    return lineNumber;
  }
  
}
