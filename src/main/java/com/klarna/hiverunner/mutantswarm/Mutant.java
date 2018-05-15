package com.klarna.hiverunner.mutantswarm;

public class Mutant {
  
  private String text;
  private String originalText;
  private String mutatedScript;
  private String originalScript;
  private boolean survived = true;
  
  Mutant(String text, String originalText, String mutatedScript, String script) {
    this.text = text;
    this.originalText = originalText;
    this.mutatedScript = mutatedScript;
    this.originalScript = script;
  }

  public String getText() {
    return text;
  }
  public String getOriginalText() {
    return originalText;
  }
  public String getMutatedScript() {
    return mutatedScript;
  }

  public String getOriginalScript() {
    return originalScript;
  }
  
  public void setSurvived(boolean survived){
    this.survived = survived;
  }
  
  public boolean hasSurvived(){
    return survived;
  }
  
  public String toString(){
    return text;
  }
  

}
