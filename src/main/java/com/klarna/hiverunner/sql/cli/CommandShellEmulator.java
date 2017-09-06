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
package com.klarna.hiverunner.sql.cli;

import java.util.List;

import com.klarna.hiverunner.sql.HiveSqlStatementFactory;
import com.klarna.hiverunner.sql.split.TokenRule;

/**
 * Attempt to accurately emulate the behaviours (good and bad) of different Hive
 * shells. Currently the {@code hive} interactive shell (which HiveRunner uses)
 * has an annoying issue where it blows up on some full line comments
 * (HIVE-8396). Beeline does not suffer from this and instead simply removes
 * them. Full line comments are stripped from script files as is the case with
 * both {@code hive -f} and {@code beeline -f}. The implementations provided
 * here replicate these modes of operation.
 */
public interface CommandShellEmulator {
	PreProcessor preProcessor();
	PostProcessor postProcessor(HiveSqlStatementFactory factory);
	String specialCharacters();
	List<TokenRule> splitterRules();
	String getName();
}
