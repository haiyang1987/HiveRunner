package com.klarna.hiverunner.mutantswarm;

import java.util.ArrayList;
import java.util.List;

public class Line {

  private int number;
  private String text;
  private int mutantCount = 0;
  private List<Mutant> mutants = new ArrayList<>();
  private List<Word> words = new ArrayList<>();
  
  Line(int number, String text) {
    this.number = number;
    this.text = text;
  }
  
  public void addMutant(Mutant mutant){
    mutants.add(mutant);
    incrementMutantCounter();
  }
  
  public List<Mutant> getMutants() {
    return mutants;
  }
  
  public int getNumber() {
    return number;
  }

  public String getText() {
    return text;
  }

  private void incrementMutantCounter(){
    mutantCount++;
  }
  
  public int getMutantCount() {
    return mutantCount;
  }
  
  public void addWord(Word word){
    words.add(word);
  }
  
  public Word getWord(String text){
    Word word = null;
    for (Word w : words){
      if (w.getText().equals(text)){
        word = w;
      }
    }
    return word;
  }
  
  public List<Word> getWords(){
    return words;
  }
  
}