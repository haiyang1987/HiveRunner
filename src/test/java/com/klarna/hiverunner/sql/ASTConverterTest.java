package com.klarna.hiverunner.sql;

import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ASTConverterTest {

  @Test
  public void checkSimpleString() throws ParseException {
    String expected = "SELECT a FROM b WHERE a = 2";
    ASTConverter converter = new ASTConverter(false);
    ASTNode tree = ParseUtils.parse(expected);
    String actual = converter.treeToQuery(tree);
    actual = actual.replaceAll("  ", " ");
    System.out.println(actual);
    
    assertEquals(expected, actual);
  }

}
