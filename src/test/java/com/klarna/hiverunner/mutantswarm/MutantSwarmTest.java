package com.klarna.hiverunner.mutantswarm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
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

  @HiveSQL(files = { "mutantSwarmTest/select.hql" })
  public HiveShell hiveShell;

  // @Before
  // public void setUp() {
  // // hiveShell.execute("CREATE DATABASE IF NOT EXISTS bar");
  // hiveShell.executeQuery("CREATE TABLE IF NOT EXISTS foo (a String, x int)");
  // hiveShell
  // .insertInto("default", "foo")
  // .withColumns("a", "x")
  // .addRow("Green", "2")
  // .addRow("Blue", "3")
  // .addRow("Yellow", "3")
  // .commit();
  // }

  @Before
  public void before(){
    System.out.println("test before()");
  }
  
  @After
  public void after(){
    System.out.println("test after()");
  }

  @Test
  public void test() {
    System.out.println("RUNNING TEST");
    List<String> result = hiveShell.executeQuery("SELECT * FROM bar");
//    assertThat(result.size(), is(1));
//    assertThat(result.get(0), is("GREEN"));
    System.out.println("Result: " + result);
    assertThat(result, is(Arrays.asList("GREEN")));
    // System.out.println("TEST PASSED");
  }

  // @Test
  // public void test2() {
  // System.out.println("RUNNING TEST 2");
  // List<String> result = hiveShell.executeQuery("SELECT a FROM foo WHERE x = 3");
  // assertThat(result.size(), is(1));
  // assertThat(result.get(0), is("BLUE"));
  // // System.out.println("TEST PASSED");
  // }

}
