package com.klarna.hiverunner.mutantswarm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class MutationReport {
  private final static File reportFile = new File("../hiverunner/target", "mutation-reports");

  private static List<String> originalScripts = new ArrayList<>();
  private static List<String> scriptNames = new ArrayList<>();

  private static List<List<Mutant>> mutants = new ArrayList<>();
  private static boolean addedAllMutants = false;

  private static int popupsNeeded = 0;
  private static int functionsNeeded = 0;

  private static BufferedWriter writer;

  public static void generateMutantReport() {
    reportFile.mkdir();
    File htmlFile = new File(reportFile, (new SimpleDateFormat("yyyyMMdd-HH:mm")).format(new Date()) + ".html");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile))) {
      writer = bw;
      setUpFile();
      writeContent();
      endFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeContent() {

    try {
      for (int i = 0; i < originalScripts.size(); i++) {
        writer.write("<h3><i>" + scriptNames.get(i) + "</i></h3>");
        writer.newLine();
        String script = originalScripts.get(i);
        script = normaliseQuery(script);
        // might be best to not normalise out the '\n'
        // just separate out all characters and do equals ignore case instead

        String[] words = script.split(" ");
        // need to do a check here to see how this is gonna work with '\n' '\t' ' ' etc

        writer.write("<p>");
        writer.newLine();
        // System.out.println("Script - " + scriptNames.get(i));

        List<Mutant> mutantList = mutants.get(i);
        for (String str : words) {
          if (str.equalsIgnoreCase("newline")) {
            writer.write("<br>");
          } else {
            List<Mutant> strMutations = new ArrayList<>();
            for (Mutant mutant : mutantList) {

              if (str.equals(mutant.getOriginalText())) {
                // maybe do equals ignore case ?
                // depends if gonna normalise query to remove capitals

                strMutations.add(mutant);
              }
            }
            if (strMutations.isEmpty()) {
              writer.write(str + " ");
              writer.newLine();
            } else {
              writer.write(writeMutatedStatement(str, mutantList));
              writer.newLine();
            }
          }
        }
        writer.write("</p>");
        writer.newLine();
      }
      writer.newLine();
      writer.write("<script>");
      writer.newLine();
      addFunctions();
      writer.write("</script>");

    } catch (IOException e) {
      e.printStackTrace();
      // System.out.println("Couldnt write to file. " + e.getMessage());
    }
  }

  private static String writeMutatedStatement(String str, List<Mutant> strMutations) {
    String survivors = "";
    String killed = "";

    boolean covered = true;
    for (Mutant mutant : strMutations) {
      // need to separate them into lists
      if (mutant.hasSurvived()) {
        covered = false;
        // survivors.add(mutant);
        survivors += mutant.getText() + ", ";
      } else {
        // killed.add(mutant);
        killed += mutant.getText() + ", ";
      }
    }
    survivors = StringUtils.removeEnd(survivors, ", "); // remove the last comma
    killed = StringUtils.removeEnd(killed, ", ");

    String popUpText = indicateMutation(survivors, killed);
    String colour = "pink";
    if (covered) {
      colour = "lightgreen";
    }
    String functionName = "function" + popupsNeeded;
    functionsNeeded++;
    String statement = "<span class=\"popup\" style=\"background-color:"
        + colour
        + "\" onclick=\""
        + functionName
        + "()\">"
        + str
        + " "
        + popUpText
        + "</span>";

    popupsNeeded++;
    return statement;
  }

  private static String indicateMutation(String survivors, String killed) {
    String popUpText = "<span class=\"popuptext\" id=\"popup" + popupsNeeded + "\">";
    if (survivors.equals("")) {
      popUpText += " Killed: " + killed + "</span>";
    } else if (killed.equals("")) {
      popUpText += "\"> Survivors: " + survivors + "</span>";
    } else {
      popUpText += " Killed: " + killed + "<br> Survivors: " + survivors + "</span>";
    }
    return popUpText;
  }

  private static void addFunctions() {
    for (int i = 0; i < functionsNeeded; i++) {
      try {
        writer.write("function function" + i + "() {");
        writer.newLine();
        writer.write("var popup = document.getElementById(\"popup" + i + "\");");
        writer.newLine();
        writer.write("popup.classList.toggle(\"show\");");
        writer.newLine();
        writer.write("}");
        writer.newLine();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private static void setUpFile() {
    try {
      writer.write("<!DOCTYPE HTML>");
      writer.newLine();
      writer.write("<html>");
      writer.newLine();
      writer.write("<head>");
      writer.newLine();
      writer.write("<title>Mutation Report</title>");

      File cssFile = new File("../hiverunner/target/mutation-reports", "mutationReportStyling.css");
      if (cssFile.exists()) {
        writer.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"mutationReportStyling.css\"/>");
        // } else {
        // System.out.println("cant find css file");
      }

      writer.write("<h1>Mutation Report</h1>");
      writer.write("</head>");
      writer.newLine();
      writer.write("<body>");
      writer.newLine();

      // <div style="color:lightblue">some</div> // changes the colour of line
      // <span style="background-color:yellow">some</span> // changes colour of what ever is contained within 'span'
      writer.newLine();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Couldnt write to file. " + e.getMessage());
    }
  }

  private static void endFile() {
    try {
      writer.newLine();
      writer.write("</body>");
      writer.newLine();
      writer.write("</html>");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Couldnt write to file. " + e.getMessage());
    }
  }

  private static String normaliseQuery(String query) {
    // query = query.trim().toLowerCase();
    query = query.replaceAll("!=", "<>"); // equivalent operator
    query = query.replaceAll("\n", " newline "); // want to preserve newlines
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

  public static void setOriginalScripts(List<String> scripts) {
    if (scripts != null) {
      originalScripts = scripts;
    }
  }

  public static void addMutants(List<Mutant> scriptMutations) {
    if (!addedAllMutants) {
      mutants.add(scriptMutations);
      // } else {
      // System.out.println("added all mutants");
    }
    //
    // for (List<Mutant> mutantlist : mutants) {
    // System.out.println(mutantlist);
    // }
  }

  public static void addScriptNames(List<Path> names) {
    for (Path script : names) {
      scriptNames.add(script.getFileName().toString());
    }
  }

  public static void finishedAddingMutants(boolean bool) {
    addedAllMutants = bool; // should only be set to true
    // System.out.println("finished adding mutants");
  }

}
