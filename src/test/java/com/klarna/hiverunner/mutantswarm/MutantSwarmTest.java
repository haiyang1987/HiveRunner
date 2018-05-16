package com.klarna.hiverunner.mutantswarm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.klarna.hiverunner.HiveShell;
import com.klarna.hiverunner.annotations.HiveSQL;
import com.klarna.hiverunner.annotations.HiveSetupScript;

@RunWith(MutantSwarmRunner.class)
public class MutantSwarmTest {

  @HiveSetupScript
  private final File setup = new File("src/test/resources/mutantSwarmTest/setup.hql");
  @HiveSetupScript
  private final File insert = new File("src/test/resources/mutantSwarmTest/insert2.hql");

  @HiveSQL(files = { "mutantSwarmTest/select.hql", "mutantSwarmTest/select2.hql" })
  public HiveShell hiveShell;

  // private static int counter = 0;

  // @After
  // public void after() {
  // counter++;
  // }

  @Test
  public void test() {
    // System.out.println("RUNNING TEST BLOCK 1 - #" + counter);
    List<String> result = hiveShell.executeQuery("SELECT * FROM bar");
    // System.out.println("Result: " + result);
    List<String> expected = Arrays.asList("1\ttrue", "3\ttrue", "3\tfalse", "5\tfalse");
    assertEquals(expected, result);
  }

  @Test
  public void test2() {
    // System.out.println("RUNNING TEST BLOCK 2 - #" + counter);
    List<String> result = hiveShell.executeQuery("SELECT * FROM foobar");
    // System.out.println("Result: " + result);
    List<String> expected = Arrays.asList("true", "false");
    assertEquals(expected, result);
  }

}
