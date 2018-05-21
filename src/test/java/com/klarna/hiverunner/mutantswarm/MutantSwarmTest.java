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

  @Test
  public void test() {
    List<String> result = hiveShell.executeQuery("SELECT * FROM bar");
    List<String> expected = Arrays.asList("1\ttrue", "3\ttrue", "3\tfalse", "5\tfalse");
    assertEquals(expected, result);
  }

  @Test
  public void test2() {
    List<String> result = hiveShell.executeQuery("SELECT * FROM foobar");
    List<String> expected = Arrays.asList("true", "false");
    assertEquals(expected, result);
  }

}
