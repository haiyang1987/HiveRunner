/**
 * Copyright (C) 2015-2018 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.klarna.hiverunner.mutantswarm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.io.Files;
import com.klarna.hiverunner.sql.ASTConverter;
import com.klarna.hiverunner.sql.StatementsSplitter;

public class HQLParseReport {

  private String parseError = "";
  File tsvParseSuccessFile;
  File tsvParseFailureFile;
  private final List<String[]> reportContents = new ArrayList<>();
  private String reportName = "";
  private final List<String> queriesToIgnore = Arrays.asList("set", "drop", "use", "grant");

  /*
   * Generate report for a folder of files
   */
  public void generateHQLParseReport(File folder) {
    reportName = folder.getParent() + "/HQLParseReport.csv";
    initialiseReport();
    for (File filename : folder.listFiles()) {
      if (!filename.getName().startsWith(".")) {
        generateReportContentsForFile(filename);
      }
    }
    generateReport();
  }

  /*
   * Generate report for a single file
   */
  public void generateHQLFileReport(File filename) {
    reportName = filename.getParent() + "/HQLFileReport.csv";
    initialiseReport();
    generateReportContentsForFile(filename);
    generateReport();
  }

  private void generateReportContentsForFile(File filename) {
    List<String> queryStatements = readQuery(filename); // will get the query statements from the file
    for (int index = 0; index < queryStatements.size(); index++) {
      String query = queryStatements.get(index);
      String statementNum = index + 1 + "";
      ASTNode tree;
      try {
        tree = ParseUtils.parse(query);
        String queryOut = unparseQuery(tree);
        generateReportContents(filename.getName(), statementNum, query, parseError, queryOut);
      } catch (ParseException e) {
        parseError = e.getMessage();
        generateReportContents(filename.getName(), statementNum, query, parseError, "");
        parseError = "";
      }
    }
  }

  private void generateReportContents(String filename, String num, String queryIn, String parseError, String queryOut) {
    String[] content;
    String normQueryIn = " "; // space string to stop the cells from spilling over into the next column
    String normQueryOut = " ";
    String trimmedParseError = " ";
    String difference = ".";

    if (!queryOut.equals("")) { // parse success
      normQueryIn = normaliseQuery(queryIn);
      normQueryOut = normaliseQuery(queryOut);
      difference = getDifference(normQueryIn, normQueryOut);
    } else {
      trimmedParseError = trimErrorStatement(parseError);
    }
    content = new String[] {
        filename,
        num,
        queryIn,
        parseError,
        trimmedParseError,
        normQueryIn,
        queryOut,
        normQueryOut,
        difference };
    reportContents.add(content);
  }

  private void initialiseReport() {
    String[] headers = {
        "Filename",
        "Statement Num",
        "Query In",
        "Parse Error",
        "Trimmed Parse Error",
        "Normalised Query In",
        "Query Out",
        "Normalised Query Out",
        "Difference" };
    reportContents.add(headers);
  }

  private void generateReport() {
    try {
      FileOutputStream fos = new FileOutputStream(reportName);
      OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
      CSVWriter writer = new CSVWriter(osw);
      writer.writeAll(reportContents);
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String trimErrorStatement(String parseError) {
    // removes 'line 1:[num] ' from the error message to allow for sorting
    Pattern p = Pattern.compile("line\\s([0-9]*):([0-9]*)\\s*");
    Matcher m = p.matcher(parseError);
    if (m.find()) {
      parseError = parseError.replace(m.group(0), "");
    }
    return parseError;
  }

  // reads in a query from a file
  private List<String> readQuery(File filename) {
    List<String> queryStatements = new ArrayList<>();
    String fileContents = "";
    List<String> mutatableQueries = new ArrayList<>();
    try {
      fileContents = Files.asCharSource(filename, StandardCharsets.UTF_8).read();
      queryStatements = StatementsSplitter.splitStatementsRemoveComments(fileContents);

      for (String query : queryStatements) {
        query = checkForVariableSubstitution(query);
        if (canMutate(query)) {
          mutatableQueries.add(query);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return mutatableQueries;
  }

  private String checkForVariableSubstitution(String query) {
    Pattern pattern = Pattern.compile("(.*)(\\$\\{.*\\})(.*)");
    Matcher matcher = pattern.matcher(query);
    while (matcher.find()) {
      query = query.replace(matcher.group(2), "varSub");
      matcher = pattern.matcher(query);
    }
    return query;
  }

  // only want to deal with 'update table', 'create table', 'select', 'insert', 'delete'
  private boolean canMutate(String query) {
    if (query.equals(" ") || query.equals("")) {
      return false;
    }
    String startsWith = query;
    if (query.contains(" ")) { // get the first word of the query
      startsWith = query.substring(0, query.indexOf(" "));
    }
    return !queriesToIgnore.contains(startsWith.toLowerCase());
  }

  private String unparseQuery(ASTNode tree) {
    ASTConverter converter = new ASTConverter(false);
    return converter.treeToQuery(tree);
  }

  private String normaliseQuery(String query) {
    query = query.trim().toLowerCase();
    query = query.replaceAll("!=", "<>"); // equivalent operator
    query = query.replaceAll("\n", " ");
    query = normaliseCharacters(query);
    query = query.replaceAll("\\s\\s+", " "); // removing excess whitespaces
    return query;
  }

  private String normaliseCharacters(String query) {
    // adds spaces around every character
    Matcher matcher = Pattern.compile("([\\'\"\\)\\(\\}\\{\\<\\>\\=\\+\\-\\,\\*])").matcher(query);
    while (matcher.find()) {
      String character = matcher.group(1);
      query = query.replaceAll("\\" + character, " \\" + character + " ");
    }
    return query;
  }

  private String getDifference(String queryIn, String queryOut) {
    int differenceIndex = StringUtils.indexOfDifference(queryIn, queryOut);
    if (differenceIndex == -1) { // the strings are equal
      return ".";
    }
    String outputQueryEnd = queryOut.substring(differenceIndex, queryOut.length());
    String originalQueryEnd = queryIn.substring(differenceIndex, queryIn.length());

    if (queryOut.equals("")) {
      return queryIn;
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

}
