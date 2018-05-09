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
  private final File insert = new File("src/test/resources/mutantSwarmTest/insert.hql");

  @HiveSQL(files = { "mutantSwarmTest/select2.hql" })
  public HiveShell hiveShell;

  private static int counter = 0;

  // @Before
  // public void before(){
  // System.out.println("test before()");
  // }
  
  @After
  public void after() {
    counter++;
    System.out.println("incrementing counter");
  }

  @Test
  public void test() {

    System.out.println("RUNNING TEST " + counter);
    final List<String> result = hiveShell.executeQuery("SELECT * FROM foobar");
    System.out.println("Result: " + result);
    System.out.println(result.equals(Arrays.asList("GREEN")));
    List<String> expected = Arrays.asList("GREEN");
    assertEquals("Test: " + counter, expected, result);
  }

}
