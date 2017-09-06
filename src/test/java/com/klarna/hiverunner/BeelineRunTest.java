/*
 * Copyright 2015 Klarna AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.klarna.hiverunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.klarna.hiverunner.annotations.HiveRunnerSetup;
import com.klarna.hiverunner.annotations.HiveSQL;
import com.klarna.hiverunner.config.HiveRunnerConfig;

@RunWith(StandaloneHiveRunner.class)
public class BeelineRunTest {

	private static final String TEST_DB = "test_db";

	@HiveRunnerSetup
	public final static HiveRunnerConfig CONFIG = new HiveRunnerConfig() {
		{
			setCommandShellEmulation(CommandShellEmulation.BEELINE);
		}
	};

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@HiveSQL(files = {}, encoding = "UTF-8", autoStart = false)
	private HiveShell hiveCliShell;

	@Test
	public void testNestedSource() throws Exception {
		File a = new File(temp.getRoot(), "a.hql");
		try (PrintStream out = new PrintStream(a)) {
			out.println("create view ${db}.a as select * from ${db}.src where c1 <> 'z'");
		}

		File b = new File(temp.getRoot(), "b.hql");
		try (PrintStream out = new PrintStream(b)) {
			out.println("!run a.hql");
			out.println("create view ${db}.b as select c0, count(*) as c1_cnt from ${db}.a group by c0");
		}

		File c = new File(temp.getRoot(), "c.hql");
		try (PrintStream out = new PrintStream(c)) {
			out.println("create view ${db}.c as select * from ${db}.b where c1_cnt > 1");
		}

		File main = new File(temp.getRoot(), "main.hql");
		try (PrintStream out = new PrintStream(main)) {
			out.println("!run b.hql");
			out.println("!run c.hql");
		}

		hiveCliShell.setHiveVarValue("db", TEST_DB);
		System.setProperty("user.dir", temp.getRoot().getAbsolutePath());;
		hiveCliShell.start();
		hiveCliShell.execute(new StringBuilder()
			.append("create database ${db};")
			.append("create table ${db}.src (")
			.append("c0 string, ")
			.append("c1 string")
			.append(");")
			.toString());
		hiveCliShell.insertInto(TEST_DB, "src")
			.addRow("A", "x")
			.addRow("A", "y")
			.addRow("B", "z")
			.addRow("B", "y")
			.addRow("C", "z")
			.commit();
		
		
		hiveCliShell.execute(main);

		List<String> results = hiveCliShell.executeQuery("select * from ${db}.c");
		assertThat(results.size(), is(1));
		assertThat(results.get(0), is("A\t2"));
	}

}
