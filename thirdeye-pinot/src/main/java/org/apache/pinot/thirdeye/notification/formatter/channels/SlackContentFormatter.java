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

package org.apache.pinot.thirdeye.notification.formatter.channels;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.MapUtils;
import org.apache.pinot.thirdeye.anomaly.ThirdEyeAnomalyConfiguration;
import org.apache.pinot.thirdeye.anomalydetection.context.AnomalyResult;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionAlertConfigDTO;
import org.apache.pinot.thirdeye.detection.ConfigUtils;
import org.apache.pinot.thirdeye.notification.commons.SlackConfiguration;
import org.apache.pinot.thirdeye.notification.commons.SlackEntity;
import org.apache.pinot.thirdeye.notification.content.BaseNotificationContent;
import org.apache.pinot.thirdeye.notification.content.templates.MetricAnomaliesContent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * This class formats the content for slack alerts
 */
public class SlackContentFormatter extends AlertContentFormatter {

	private SlackConfiguration slackAdminConfig;

	private static final String CHARSET = "UTF-8";

	private static final Map<String, String> alertContentToTemplateMap;

	static {
		Map<String, String> aMap = new HashMap<>();
		aMap.put(MetricAnomaliesContent.class.getSimpleName(), "slack-metric-anomalies-template.ftl");
		alertContentToTemplateMap = Collections.unmodifiableMap(aMap);
	}

	public SlackContentFormatter(SlackConfiguration slackAdminConfig, Properties slackClientConfig,
			BaseNotificationContent content, ThirdEyeAnomalyConfiguration teConfig,
			DetectionAlertConfigDTO subsConfig) {
		super(slackClientConfig, content, teConfig, subsConfig);

		this.slackAdminConfig = slackAdminConfig;
		validateSlackConfigs(slackAdminConfig);
	}

	/**
	 * Make sure the slack default channel is configured before proceeding
	 */
	private void validateSlackConfigs(SlackConfiguration slackAdminConfig) {
		Preconditions.checkNotNull(slackAdminConfig.getDefaultChannel());
	}

	/**
	 * Format and construct a {@link SlackEntity} by rendering the anomalies and
	 * properties
	 *
	 * @param dimensionFilters dimensions configured in the multi-dimensions alerter
	 * @param anomalies        anomalies to be reported to recipients configured in
	 *                         (@link #slackClientConfig}
	 */
	public SlackEntity getSlackEntity(Multimap<String, String> dimensionFilters, Collection<AnomalyResult> anomalies) {
		Map<String, Object> templateData = notificationContent.format(anomalies, this.subsConfig);
		templateData.put("dashboardHost", teConfig.getDashboardHost());
		return buildSlackEntity(alertContentToTemplateMap.get(notificationContent.getTemplate()), templateData,
				dimensionFilters);
	}

	private String buildSummary(Map<String, Object> templateValues, Multimap<String, String> dimensionFilters) {
		String issueSummary = BaseNotificationContent.makeSubject(super.getSubjectType(alertClientConfig),
				this.subsConfig, templateValues);

		// Append dimensional info to summary
		StringBuilder dimensions = new StringBuilder();
		for (Map.Entry<String, Collection<String>> dimFilter : dimensionFilters.asMap().entrySet()) {
			dimensions.append(", ").append(dimFilter.getKey()).append("=")
					.append(String.join(",", dimFilter.getValue()));
		}
		issueSummary = issueSummary + dimensions.toString();
		return issueSummary;
	}

	private String buildDescription(String slackTemplate, Map<String, Object> templateValues) {
		String description;

		// Render the values in templateValues map to the slack ftl template file
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (Writer out = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
			Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_21);
			freemarkerConfig.setClassForTemplateLoading(getClass(), "/org/apache/pinot/thirdeye/detector");
			freemarkerConfig.setDefaultEncoding(CHARSET);
			freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			Template template = freemarkerConfig.getTemplate(slackTemplate);
			template.process(templateValues, out);

			description = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			description = "Found an exception while constructing the description content. Pls report & reach out"
					+ " to the Thirdeye team. Exception = " + e.getMessage();
		}

		return description;
	}

	/**
	 * Apply the parameter map to given slack template, and format it as SlackEntity
	 */
	private SlackEntity buildSlackEntity(String slackTemplate, Map<String, Object> templateValues,
			Multimap<String, String> dimensionFilters) {
		String token = System.getenv(SlackConfiguration.SLACK_TOKEN);
		String defaultChannel = MapUtils.getString(alertClientConfig, SlackConfiguration.DEFAULT_CHANNEL,
				this.slackAdminConfig.getDefaultChannel());
		SlackEntity slackEntity = new SlackEntity();
		slackEntity.setSlackToken(token);
		slackEntity.setDefaultChannel(defaultChannel);
		slackEntity.setDescription(buildDescription(slackTemplate, templateValues));
		slackEntity.setSummary(buildSummary(templateValues, dimensionFilters));
		List<String> channels = ConfigUtils.getList(alertClientConfig.get(SlackConfiguration.CHANNELS));
		if (!channels.isEmpty()) {
			slackEntity.setChannels(channels);
		}
		return slackEntity;
	}
}