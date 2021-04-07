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

package org.apache.pinot.thirdeye.detection.alert.scheme;

import static org.apache.pinot.thirdeye.notification.commons.SlackConfiguration.SLACK_CONF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Preconditions;

import org.apache.pinot.thirdeye.anomaly.ThirdEyeAnomalyConfiguration;
import org.apache.pinot.thirdeye.anomaly.utils.ThirdeyeMetricsUtil;
import org.apache.pinot.thirdeye.anomalydetection.context.AnomalyResult;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionAlertConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.detection.ConfigUtils;
import org.apache.pinot.thirdeye.detection.alert.DetectionAlertFilterNotification;
import org.apache.pinot.thirdeye.detection.alert.DetectionAlertFilterResult;
import org.apache.pinot.thirdeye.detection.annotation.AlertScheme;
import org.apache.pinot.thirdeye.notification.commons.SlackConfiguration;
import org.apache.pinot.thirdeye.notification.commons.SlackEntity;
import org.apache.pinot.thirdeye.notification.commons.ThirdEyeSlackClient;
import org.apache.pinot.thirdeye.notification.content.BaseNotificationContent;
import org.apache.pinot.thirdeye.notification.formatter.channels.SlackContentFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for creating the slack alerts
 */
@AlertScheme(type = "SLACK")
public class DetectionSlackAlerter extends DetectionAlertScheme {
	private static final Logger LOG = LoggerFactory.getLogger(DetectionSlackAlerter.class);

	private ThirdEyeAnomalyConfiguration teConfig;
	private ThirdEyeSlackClient slackClient;
	private SlackConfiguration slackAdminConfig;

	public static final String PROP_SLACK_SCHEME = "slackScheme";
	public static final int SLACK_DESCRIPTION_MAX_LENGTH = 100000;

	public DetectionSlackAlerter(DetectionAlertConfigDTO subsConfig, ThirdEyeAnomalyConfiguration thirdeyeConfig,
			DetectionAlertFilterResult result, ThirdEyeSlackClient slackClient) {
		super(subsConfig, result);
		this.teConfig = thirdeyeConfig;
		this.slackAdminConfig = SlackConfiguration
				.createFromProperties(this.teConfig.getAlerterConfiguration().get(SLACK_CONF));
		this.slackClient = slackClient;
	}

	public DetectionSlackAlerter(DetectionAlertConfigDTO subsConfig, ThirdEyeAnomalyConfiguration thirdeyeConfig,
			DetectionAlertFilterResult result) throws Exception {
		this(subsConfig, thirdeyeConfig, result, new ThirdEyeSlackClient());
	}

	private SlackEntity buildSlackEntity(DetectionAlertFilterNotification notification,
			Set<MergedAnomalyResultDTO> anomalies) {
		DetectionAlertConfigDTO subsetSubsConfig = notification.getSubscriptionConfig();
		if (subsetSubsConfig.getAlertSchemes().get(PROP_SLACK_SCHEME) == null) {
			throw new IllegalArgumentException("Slack not configured in subscription group " + this.subsConfig.getId());
		}

		Properties slackClientConfig = new Properties();
		slackClientConfig.putAll(ConfigUtils.getMap(subsetSubsConfig.getAlertSchemes().get(PROP_SLACK_SCHEME)));

		List<AnomalyResult> anomalyResultListOfGroup = new ArrayList<>(anomalies);
		anomalyResultListOfGroup.sort(COMPARATOR_DESC);

		BaseNotificationContent content = getNotificationContent(slackClientConfig);

		return new SlackContentFormatter(this.slackAdminConfig, slackClientConfig, content, this.teConfig,
				subsetSubsConfig).getSlackEntity(notification.getDimensionFilters(), anomalyResultListOfGroup);
	}

	private void createSlackMsgs(DetectionAlertFilterResult results) throws Exception {
		LOG.info("Preparing a slack alert for subscription group id {}", this.subsConfig.getId());
		Preconditions.checkNotNull(results.getResult());
		for (Map.Entry<DetectionAlertFilterNotification, Set<MergedAnomalyResultDTO>> result : results.getResult()
				.entrySet()) {
			try {
				SlackEntity slackEntity = buildSlackEntity(result.getKey(), result.getValue());
				String response = slackClient.createMessage(slackEntity);
				if (response != null && response.equals("200")) {
					LOG.error("Slack alert failed for {} anomalies", result.getValue().size());
					ThirdeyeMetricsUtil.slackAlertsFailedCounter.inc();
				} else {
					ThirdeyeMetricsUtil.slackAlertsSuccessCounter.inc();
					ThirdeyeMetricsUtil.slackAlertsNumMessageCounter.inc();
					LOG.info("Slack alert created for {} anomalies", result.getValue().size());
				}

			} catch (Exception e) {
				ThirdeyeMetricsUtil.slackAlertsFailedCounter.inc();
				super.handleAlertFailure(result.getValue().size(), e);
			}
		}
	}

	@Override
	public void run() throws Exception {
		Preconditions.checkNotNull(result);
		if (result.getAllAnomalies().isEmpty()) {
			LOG.info("Zero anomalies found, skipping creation of slack alert for {}", this.subsConfig.getId());
			return;
		}

		createSlackMsgs(result);
	}
}
