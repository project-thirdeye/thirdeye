package org.apache.pinot.thirdeye.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomConfigReader {
	public String readEnv(String inputStr) {
		final String regex = "\\?\\$[A-Z0-9_]*";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(inputStr);

		while (matcher.find()) {
			String matchStr = matcher.group(0);
			String env = matchStr.substring(2);
			String value = System.getenv(env);
			if (value != null) {
				// replace original string with configured env variables
				inputStr = inputStr.replace(matchStr, value);
			} else {
				System.out.format("%s is" + " not assigned.%n", env);
			}
		}

		return inputStr;
	}
}
