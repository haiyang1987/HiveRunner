package com.klarna.hiverunner.sql.split;

import java.util.Collections;
import java.util.Set;

public enum CloseStatementRule implements TokenRule {
	INSTANCE;
	
	@Override
	public Set<String> triggers() {
		return Collections.singleton(";");
	}

	@Override
	public void handle(String token, Context context) {
		// Only add statement that is not empty
		context.flush();
	}

}
