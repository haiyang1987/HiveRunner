package com.klarna.hiverunner.mutantswarm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.parse.ASTNode;

import com.klarna.hiverunner.sql.ASTConverter;

public class MutationReport {
  private final static File reportFile = new File("../hiverunner/target", "mutation-reports");

  public static void generateHtmlMutantReport(List<String> originalScripts, List<List<String>> activeMutations) {
    reportFile.mkdir();
    File htmlFile = new File(reportFile, (new SimpleDateFormat("yyyyMMdd-HH:mm")).format(new Date()) + ".html");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile))){
//      setUpFile(bw);

      for (int i = 0; i < originalScripts.size(); i++) {
        String originalScript = normaliseQuery(originalScripts.get(i)); // check this ****
        System.out.println("normalised original script; " + originalScript);
        bw.write("<p><b>List of Survived Mutations for <i>" + originalScript + "</i></b></p>");

        for (List<String> scriptList : activeMutations) {
          String mutatedScript = normaliseQuery(scriptList.get(i));
          System.out.println("normalised mutated script; " + mutatedScript);

          List<String> mutatedQueryArray = splitQueryByMutant(originalScript, mutatedScript); // check this ****

//          System.out.println("difference; " + mutatedQueryArray.get(1));
//          writeToFile(mutatedQueryArray, bw);
        }
      }
//      endFile(bw);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void setUpFile(BufferedWriter bw) {
    try {
      bw.write("<!DOCTYPE HTML>");
      bw.newLine();
      bw.write("<html>");
      bw.newLine();
      bw.write("<head>");
      bw.newLine();
      bw.write("<title>Mutation Report</title>");
      bw.write("<p style=\"font-size:36px;\">Mutation Report</p>");
      bw.write("</head>");
      bw.newLine();
      bw.write("<body>");
      bw.newLine();

      // <div style="color:lightblue">some</div> // changes the colour of line
      // <span style="background-color:yellow">some</span> // changes colour of what ever is contained within 'span'
      bw.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void endFile(BufferedWriter bw) {
    try {
      bw.write("<p>- - End of Mutations - -</p>");
      bw.newLine();
      bw.write("</body>");
      bw.newLine();
      bw.write("</html>");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeToFile(List<String> mutatedQueryArray, BufferedWriter bw) {
    try {
      bw.write("<p>"
          + "<span style=\"background-color:lightgreen\">"
          + mutatedQueryArray.get(0)
          + "</span>"
          + "<span style=\"background-color:pink\">"
          + mutatedQueryArray.get(1)
          + "</span>"
          + "<span style=\"background-color:lightgreen\">"
          + mutatedQueryArray.get(2)
          + "</span>"
          + "</p>");
      bw.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<String> splitQueryByMutant(String originalQuery, String outputQuery) {
    List<String> str = new ArrayList<>();
    int diffIndex = StringUtils.indexOfDifference(originalQuery, outputQuery);
    str.add(outputQuery.substring(0, diffIndex));
    String mutatedString = getMutatedString(originalQuery, outputQuery);
    str.add(mutatedString);
    str.add(outputQuery.substring(diffIndex + mutatedString.length()));
    return str;
  }

  private static String getMutatedString(String originalQuery, String outputQuery) {
    int differenceIndex = StringUtils.indexOfDifference(originalQuery, outputQuery);
    if (differenceIndex == -1) { // the strings are equal
      return "";
    }
    String outputQueryEnd = outputQuery.substring(differenceIndex, outputQuery.length());
    String originalQueryEnd = originalQuery.substring(differenceIndex, originalQuery.length());

    if (outputQuery.equals("")) {
      return originalQuery;
    }
    StringBuilder difference = new StringBuilder(outputQueryEnd);
    int lengthDiff;
    if (originalQueryEnd.length() > outputQueryEnd.length()) {
      // if original query is longer it means something was removed
      lengthDiff = originalQueryEnd.length() - outputQueryEnd.length();
      difference = new StringBuilder(originalQueryEnd); // want the longer string

      // want to loop through the shorter string
      for (int i = outputQueryEnd.length() - 1; i > -1; i--) {
        if (originalQueryEnd.charAt(i + lengthDiff) == outputQueryEnd.charAt(i)) {
          difference.deleteCharAt(i + lengthDiff);
        }
      }
    } else {
      // output query is longer - something was added
      lengthDiff = outputQueryEnd.length() - originalQueryEnd.length();
      for (int i = originalQueryEnd.length() - 1; i > -1; i--) {
        if (originalQueryEnd.charAt(i) == outputQueryEnd.charAt(i + lengthDiff)) {
          difference.deleteCharAt(i + lengthDiff);
        }
      }
    }
    return difference.toString();
  }

  private static String normaliseQuery(String query) {
    query = query.trim().toLowerCase();
    query = query.replaceAll("!=", "<>"); // equivalent operator
    query = query.replaceAll("\n", " ");
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
