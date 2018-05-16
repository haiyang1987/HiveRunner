package com.klarna.hiverunner.sql;

import org.apache.hadoop.hive.ql.lib.Node;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ASTConverterTest {

  private ASTConverter converter = new ASTConverter(false);

  private String normaliseQuery(String query) {
    query = query.trim().toLowerCase();
    query = query.replaceAll("!=", "<>"); // equivalent operator
    query = query.replaceAll("\n", " ");
    query = normaliseCharacters(query);
    query = query.replaceAll("\\s\\s+", " "); // removing excess whitespaces
    return query;
  }

  // TODO - remove this
  private static void printTree(ASTNode node, int indentation) {
    for (int i = 0; i < indentation; i++) {
      System.out.print("\t");
    }
    System.out.print(node.getToken().toString());
    System.out.println();

    ArrayList<Node> children = node.getChildren();
    if (children != null) {
      for (Node child : children) {
        printTree((ASTNode) child, indentation + 1);
      }
    }
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

  @Test
  public void selectFrom() throws ParseException {
    String expected = "SELECT a FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectDistinctFrom() throws ParseException {
    String expected = "SELECT DISTINCT a FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhere() throws ParseException {
    String expected = "SELECT a FROM b WHERE a = 2";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromSubquery() throws ParseException {
    String expected = "SELECT a FROM (SELECT a FROM b) c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromCTE() throws ParseException {
    String expected = "WITH a AS (SELECT * FROM c) SELECT * FROM a";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromMultipleCTE() throws ParseException {
    String expected = "WITH a AS (SELECT * FROM b), b AS (SELECT * FROM c) SELECT * from a";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereIn() throws ParseException {
    String expected = "SELECT a FROM b WHERE a IN (a, c, d)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereInExpression() throws ParseException {
    String expected = "SELECT a FROM b WHERE a IN (SELECT c from d)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereLike() throws ParseException {
    String expected = "SELECT a FROM b WHERE a LIKE \"Test\"";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNotLike() throws ParseException {
    String expected = "SELECT a FROM b WHERE a NOT LIKE \"Test\"";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereBetween() throws ParseException {
    String expected = "SELECT a FROM b WHERE a BETWEEN c AND d";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNotBetween() throws ParseException {
    String expected = "SELECT a FROM b WHERE a NOT BETWEEN c AND d";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereEqual() throws ParseException {
    String expected = "SELECT a FROM b WHERE a = 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereGreaterThan() throws ParseException {
    String expected = "SELECT a FROM b WHERE a > 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereGreaterEqual() throws ParseException {
    String expected = "SELECT a FROM b WHERE a >= 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereLessThan() throws ParseException {
    String expected = "SELECT a FROM b WHERE a < 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereLessThanEqual() throws ParseException {
    String expected = "SELECT a FROM b WHERE a <= 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNotEqual() throws ParseException {
    String expected = "SELECT a FROM b WHERE a != 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNotEqual2() throws ParseException {
    String expected = "SELECT a FROM b WHERE a <> 5";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereAnd() throws ParseException {
    String expected = "SELECT a FROM b WHERE a > 5 AND a <> 3";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereOr() throws ParseException {
    String expected = "SELECT a, c FROM b WHERE a > 5 OR c = true";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNot() throws ParseException {
    String expected = "SELECT a FROM b WHERE NOT a = 3";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereTrue() throws ParseException {
    String expected = "SELECT a FROM b WHERE a = true";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereFalse() throws ParseException {
    String expected = "SELECT a FROM b WHERE a = false";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectAddtion() throws ParseException {
    String expected = "SELECT a + b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectSubtract() throws ParseException {
    String expected = "SELECT a - b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectMultiply() throws ParseException {
    String expected = "SELECT a * b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectDivide() throws ParseException {
    String expected = "SELECT a / b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereNull() throws ParseException {
    String expected = "SELECT a FROM b WHERE a IS NULL";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereIsNotNull() throws ParseException {
    String expected = "SELECT a FROM b WHERE a IS NOT NULL";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFunction() throws ParseException {
    String expected = "SELECT count(*) FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectAll() throws ParseException {
    String expected = "SELECT * FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectLimit() throws ParseException {
    String expected = "SELECT * FROM b LIMIT 10";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromOrderByDesc() throws ParseException {
    String expected = "SELECT * FROM b ORDER BY b DESC";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromOrderByAsc() throws ParseException {
    String expected = "SELECT * FROM b ORDER BY b ASC";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromOrderByDefault() throws ParseException {
    String expected = "SELECT * FROM b ORDER BY b ASC";
    ASTNode tree = ParseUtils.parse("SELECT * FROM b ORDER BY b");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromDistributeBy() throws ParseException {
    String expected = "SELECT * FROM b DISTRIBUTE BY b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromSortByDefault() throws ParseException {
    String expected = "SELECT * FROM b SORT BY b ASC";
    ASTNode tree = ParseUtils.parse("SELECT * FROM b SORT BY b");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromSortByDesc() throws ParseException {
    String expected = "SELECT * FROM b SORT BY b DESC";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromClusterBy() throws ParseException {
    String expected = "SELECT * FROM b CLUSTER BY b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromGroupBy() throws ParseException {
    String expected = "SELECT * FROM b GROUP BY b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectFromWhereHaving() throws ParseException {
    String expected = "SELECT a, b FROM c GROUP BY a HAVING b > 10";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTable() throws ParseException {
    String expected = "CREATE TABLE table1";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void insertIntoTable() throws ParseException {
    String expected = "INSERT INTO TABLE table1 VALUES (a, b, c)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void insertOverwriteTable() throws ParseException {
    String expected = "INSERT OVERWRITE TABLE table1 select a from b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void insertOverwriteDirectory() throws ParseException {
    String expected = "INSERT OVERWRITE DIRECTORY 'dir1' select a from b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createExternalTable() throws ParseException {
    String expected = "CREATE EXTERNAL TABLE table1";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTemporaryTable() throws ParseException {
    String expected = "CREATE TEMPORARY TABLE table1";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableAs() throws ParseException {
    String expected = "CREATE TABLE table1 AS SELECT b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableIfNotExists() throws ParseException {
    String expected = "CREATE TABLE IF NOT EXISTS table1";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionString() throws ParseException {
    String expected = "CREATE TABLE table1 (a String)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionInt() throws ParseException {
    String expected = "CREATE TABLE table1 (a int)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionSmallInt() throws ParseException {
    String expected = "CREATE TABLE table1 (a smallint)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionBigInt() throws ParseException {
    String expected = "CREATE TABLE table1 (a bigint)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionBoolean() throws ParseException {
    String expected = "CREATE TABLE table1 (a boolean)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionDecimal() throws ParseException {
    String expected = "CREATE TABLE table1 (a decimal)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionFloat() throws ParseException {
    String expected = "CREATE TABLE table1 (a float)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionDouble() throws ParseException {
    String expected = "CREATE TABLE table1 (a double)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionTimestamp() throws ParseException {
    String expected = "CREATE TABLE table1 (a timestamp)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionDate() throws ParseException {
    String expected = "CREATE TABLE table1 (a Date)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionVarchar() throws ParseException {
    String expected = "CREATE TABLE table1 (a varchar(255))";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionChar() throws ParseException {
    String expected = "CREATE TABLE table1 (a char(12))";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableDefinitionMultiple() throws ParseException {
    String expected = "CREATE TABLE table1 ( a String, b int, c boolean)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithLocation() throws ParseException {
    String expected = "CREATE TABLE table1 LOCATION \"location\"";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithPartition() throws ParseException {
    String expected = "CREATE TABLE table1 PARTITIONED BY (a String)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithTblProperties() throws ParseException {
    String expected = "CREATE TABLE table1 TBLPROPERTIES ('a'='b')";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithMultipleTblProperties() throws ParseException {
    String expected = "CREATE TABLE table1 TBLPROPERTIES ('a'='b', 'c'='d')";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithRowFormat() throws ParseException {
    String expected = "CREATE TABLE table1 ROW FORMAT DELIMITED FIELDS TERMINATED BY '\\t'";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithFileFormat() throws ParseException {
    String expected = "CREATE TABLE table1 STORED AS TEXTFILE";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithTableFileFormat() throws ParseException {
    String expected = "CREATE TABLE table1 STORED AS INPUTFORMAT 'a' OUTPUTFORMAT 'b'";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithSerde() throws ParseException {
    String expected = "CREATE TABLE table1 ROW FORMAT SERDE 'srde'";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithSerdeProperties() throws ParseException {
    String expected = "CREATE TABLE table1 ROW FORMAT SERDE 'srde' WITH SERDEPROPERTIES (\"a\"=\"b\")";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithMultipleSerdeProperties() throws ParseException {
    String expected = "CREATE TABLE table1 ROW FORMAT SERDE 'srde' WITH SERDEPROPERTIES (\"a\"=\"b\", \"c\"=\"d\")";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectCaseWhen() throws ParseException {
    String expected = "SELECT CASE WHEN a = 1 THEN true END as b from c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectIf() throws ParseException {
    String expected = "SELECT IF(a=0, null, a) AS b FROM c";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithLateralView() throws ParseException {
    String expected = "SELECT a FROM b LATERAL VIEW explode(c) d AS e";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void createTableWithLateralViewOuter() throws ParseException {
    String expected = "SELECT a FROM b LATERAL VIEW OUTER explode(c) d AS e";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectCastDouble() throws ParseException {
    String expected = "SELECT CAST(a AS DOUBLE) FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void selectCastTimestamp() throws ParseException {
    String expected = "SELECT CAST(a AS TIMESTAMP) FROM b";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void join() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void joinOnMultiple() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a JOIN b ON (a.x = b.x, a.y = b.y)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void crossJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a CROSS JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  /*
   * 'OUTER' is an optional keyword, it is implied
   */
  @Test
  public void rightJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a RIGHT OUTER JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse("SELECT a.a, b.b FROM a RIGHT JOIN b ON (a.x = b.x)");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void rightOuterJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a RIGHT OUTER JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void leftJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a LEFT OUTER JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse("SELECT a.a, b.b FROM a LEFT JOIN b ON (a.x = b.x)");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }

  @Test
  public void leftOuterJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a LEFT OUTER JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }
  
  @Test
  public void fullJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a FULL OUTER JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse("SELECT a.a, b.b FROM a FULL JOIN b ON (a.x = b.x)");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }
  
  /*
   * INNER is an optional keywords - doesnt show up in the tree
   */
  @Test
  public void innerJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse("SELECT a.a, b.b FROM a INNER JOIN b ON (a.x = b.x)");
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }
  
  @Test
  public void leftSemiJoin() throws ParseException {
    String expected = "SELECT a.a, b.b FROM a LEFT SEMI JOIN b ON (a.x = b.x)";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }
  
  @Test
  public void unionAll() throws ParseException {
    String expected = "SELECT a FROM b UNION ALL SELECT c FROM d";
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    assertEquals(normaliseQuery(expected), normaliseQuery(actual));
  }
  
}
