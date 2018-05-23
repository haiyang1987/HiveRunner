package com.klarna.hiverunner.mutantswarm;

public class Word {

  private String text;
  private boolean killed = true;
  private Status status = Status.not_mutated;
  
  enum Status{
    not_mutated, survived, killed;
  }
  
  Word(String text){
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public boolean hasBeenKilled() {
    return killed;
  }

  public void setKilled() {
      this.status = Status.killed;
  }
  
  public void setSurvived(){
    this.status = Status.survived;
  }
  
  public Status getStatus() {
    return status;
  }

}
