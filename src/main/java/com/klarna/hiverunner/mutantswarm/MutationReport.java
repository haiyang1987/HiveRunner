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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MutationReport {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MutationReport.class);

  private final static File reportFile = new File("../hiverunner/target", "mutant-swarm-reports");

  private static List<String> originalScripts = new ArrayList<>();
  private static List<String> scriptNames = new ArrayList<>();

  private static List<List<Mutant>> mutants = new ArrayList<>();
  private static boolean addedAllMutants = false;

  private static int popupNum = 0;

  private static BufferedWriter writer;

  private static double totalMutants = 0;
  private static double numMutantsKilled = 0;

  public static void generateMutantReport() {
    reportFile.mkdir();
    File htmlFile = new File(reportFile, (new SimpleDateFormat("yyyyMMdd-HH:mm")).format(new Date()) + ".html");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile))) {
      writer = bw;
      setUpFile();
      writeContent();
      endFile();
      generateMutationInfo();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeContent() {
    try {
      for (int i = 0; i < originalScripts.size(); i++) {
        writer.write("<h3>" + scriptNames.get(i) + "</h3>");
        writer.newLine();
        String script = originalScripts.get(i);
        script = normalise(script);

        String[] words = script.split(" ");
        writer.write("<p>");
        writer.newLine();
        List<Mutant> mutantList = mutants.get(i);
        for (String str : words) {
          if (str.equalsIgnoreCase("newline")) {
            writer.write("<br>");
          } else {
            List<Mutant> strMutations = new ArrayList<>();
            for (Mutant mutant : mutantList) {
              totalMutants++;
              if (str.equalsIgnoreCase(mutant.getOriginalText())) {
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
      LOGGER.warn("Error writing to file. " + e.getMessage());
    }
  }

  /**
   * Writes the string to the file and adds the popup with mutant list.
   */
  private static String writeMutatedStatement(String str, List<Mutant> strMutations) {
    String survivors = "";
    String killed = "";

    boolean covered = true;
    for (Mutant mutant : strMutations) {
      if (mutant.hasSurvived()) {
        covered = false;
        survivors += mutant.getText() + ", ";
      } else {
        numMutantsKilled++;
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
    String functionShow = "show" + popupNum;
    String functionHide = "hide" + popupNum;

    String statement = "<span class=\"popup\" style=\"background-color:"
        + colour
        + "\" onmouseover=\""
        + functionShow
        + "()\""
        + " onmouseout=\""
        + functionHide
        + "()\">"
        + str
        + " "
        + popUpText
        + "</span>";

    popupNum++;
    return statement;
  }

  /**
   * Generates popup text
   */
  private static String indicateMutation(String survivors, String killed) {
    String popUpText = "<span class=\"popuptext\" id=\"popup" + popupNum + "\">";
    if (survivors.equals("")) {
      popUpText += " Killed: " + killed + "</span>";
    } else if (killed.equals("")) {
      popUpText += "\"> Survivors: " + survivors + "</span>";
    } else {
      popUpText += " Killed: " + killed + "<br> Survivors: " + survivors + "</span>";
    }
    return popUpText;
  }

  /**
   * Gives functionality to the popup
   */
  private static void addFunctions() {
    writeFunctions("show");
    writeFunctions("hide");
  }

  private static void writeFunctions(String name) {
    for (int i = 0; i < popupNum; i++) {
      try {
        writer.write("function " + name + i + "() {");
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

  /**
   * Adds opening tags and CSS link to the file
   */
  private static void setUpFile() {
    try {
      writer.write("<!DOCTYPE HTML>");
      writer.newLine();
      writer.write("<html>");
      writer.newLine();
      writer.write("<head>");
      writer.newLine();
      writer.write("<title>Mutation Report</title>");

      File cssFile = new File("../HiveRunner/css", "mutationReportStyling.css");
      if (cssFile.exists()) {
        writer.write("<link rel=\"stylesheet\" href=\"../../css/mutationReportStyling.css\"/>");
      } else {
        LOGGER.warn("CSS file does not exist");
      }

      writer.write("<h1>Mutation Report</h1>");
      writer.write("</head>");
      writer.newLine();
      writer.write("<body>");
      writer.newLine();
      writer.newLine();
    } catch (IOException e) {
      e.printStackTrace();
      LOGGER.warn("Encountered a problem writing to file. " + e.getMessage());
    }
  }

  /**
   * Adds closing tags to the file
   */
  private static void endFile() {
    try {
      writer.newLine();
      writer.write("</body>");
      writer.newLine();
      writer.write("</html>");
    } catch (IOException e) {
      e.printStackTrace();
      LOGGER.warn("Encountered a problem writing to file. " + e.getMessage());
    }
  }

  private static String normalise(String query) {
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

  private static void generateMutationInfo() {
    int percentageKilled = (int) ((numMutantsKilled / totalMutants) * 100);
    String border = "\n================================================================================";
    String message = border
        + "\nMutation Statistics"
        + border
        + "\nGenerated "
        + (int) totalMutants
        + " mutations. Killed "
        + (int) numMutantsKilled
        + ".\nSuccess Rate of "
        + percentageKilled
        + "%."
        + border;

    LOGGER.info(message);

  }

  public static void setOriginalScripts(List<String> scripts) {
    if (scripts != null) {
      originalScripts = scripts;
    }
  }

  public static void addMutants(List<Mutant> scriptMutations) {
    if (!addedAllMutants) {
      mutants.add(scriptMutations);
    }
  }

  public static void addScriptNames(List<Path> names) {
    for (Path script : names) {
      scriptNames.add(script.getFileName().toString());
    }
  }

  /*
   * Called after each script is run with all mutations
   */
  public static void finishedAddingMutants() {
    addedAllMutants = true; // prevents the mutants from being overwritten
  }

}
