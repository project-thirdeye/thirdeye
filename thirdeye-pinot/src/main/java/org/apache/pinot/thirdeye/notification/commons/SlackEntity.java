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
import java.util.List;

/**
 * ThirdEye's Slack Settings Holder
 */
public class SlackEntity {
	private String url;
	private String defaultChannel;
	private String slackToken;
	private String summary;
	private String description;
	// Place holder for configuring non-standard customized slack fields
	private Map<String, Object> customFieldsMap;
	private List<String> channels;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, Object> getCustomFieldsMap() {
		return customFieldsMap;
	}

	public void setCustomFieldsMap(Map<String, Object> customFieldsMap) {
		this.customFieldsMap = customFieldsMap;
	}
	
	public String getDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(String defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	public String getSlackToken() {
		return slackToken;
	}

	public void setSlackToken(String slackToken) {
		this.slackToken = slackToken;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Slack{");
		sb.append("slackUrl='").append(url).append('\'');
		sb.append(", summary='").append(summary).append('\'');
		sb.append(", custom='").append(customFieldsMap).append('\'');
		sb.append('}');
		return sb.toString();
	}

	public List<String> getChannels() {
		return channels;
	}

	public void setChannels(List<String> channels) {
		this.channels = channels;
	}

}
