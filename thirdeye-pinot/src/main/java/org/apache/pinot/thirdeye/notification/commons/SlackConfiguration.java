/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pinot.thirdeye.notification.commons;

import java.util.Map;

import com.google.common.base.MoreObjects;

import org.apache.commons.collections4.MapUtils;

public class SlackConfiguration {

	public static final String SLACK_CONF = "slackConfiguration";
	public static final String DEFAULT_CHANNEL = "defaultChannel";
	public static final String SLACK_TOKEN = "SLACK_TOKEN";
	public static final String CHANNELS = "channels";

	private String defaultChannel;


	public String getDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(String defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	public static SlackConfiguration createFromProperties(Map<String, Object> slackConfiguration) {
		SlackConfiguration conf = new SlackConfiguration();
		try {
			conf.setDefaultChannel(MapUtils.getString(slackConfiguration, DEFAULT_CHANNEL));
		} catch (Exception e) {
			throw new RuntimeException("Error occurred while parsing slack configuration (slack default channel) into object.", e);
		}
		return conf;
	}
}
