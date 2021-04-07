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

import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;

/**
 * A client to communicate with Slack
 */
public class ThirdEyeSlackClient {
	private static final Logger LOG = LoggerFactory.getLogger(ThirdEyeSlackClient.class);
	private Slack slack;

	public ThirdEyeSlackClient() {
		this.slack = Slack.getInstance();
	}

	public static final String PROP_CHANNELS = "channels";

	/**
	 * Creates a new slack message with specified settings
	 */
	public String createMessage(SlackEntity slackEntity) {
		String defaultChannel = slackEntity.getDefaultChannel();
		String token = slackEntity.getSlackToken();
		List<LayoutBlock> message = new ArrayList<>();
		message.add(HeaderBlock.builder().text(plainText("New Anomaly Alert")).build());
		message.add(DividerBlock.builder().build());
		message.add(SectionBlock.builder().text(MarkdownTextObject.builder().text(slackEntity.getDescription()).build())
				.build());
		ChatPostMessageResponse response = null;
		try {
			List<String> alertChannels = slackEntity.getChannels();
			if (!alertChannels.isEmpty()) {
				for (String channel : alertChannels) {
					response = slack.methods(token)
							.chatPostMessage(req -> req.channel(channel).text("Anomaly Alert").blocks(message));
				}
			} else {
				response = slack.methods(token)
						.chatPostMessage(req -> req.channel(defaultChannel).text("Anomaly Alert").blocks(message));
			}
		} catch (IOException | SlackApiException e) {
			LOG.error(Arrays.toString(e.getStackTrace()));
		}
		if (response != null) {
			return response.getError();
		}
		return null;
	}

}
