<#if anomalyCount == 1>
  ThirdEye has detected <${dashboardHost}/app/#/anomalies?anomalyIds=${anomalyIds}|an anomaly> on the metric <#list metricsMap?keys as id>*${metricsMap[id].name}*</#list> between *${startTime}* and *${endTime}* (${timeZone})
<#else>
  ThirdEye has detected <${dashboardHost}/app/#/anomalies?anomalyIds=${anomalyIds}|${anomalyCount} anomalies> on the metrics listed below between *${startTime}* and *${endTime}* (${timeZone})
</#if>
<#list metricToAnomalyDetailsMap?keys as metric>
<#list detectionToAnomalyDetailsMap?keys as detectionName>
<#assign newTable = false>
<#list detectionToAnomalyDetailsMap[detectionName] as anomaly>
<#if anomaly.metric==metric>
  <#assign newTable=true>
  <#assign description=anomaly.funcDescription>
<#if newTable>
  <#if description?has_content><#rt>${'\n'}>`${description}`</#if><#rt>${'\n'}>Metric: _${metric}_
  <#rt>${'\n'}>Detection Name: <${dashboardHost}/app/#/manage/explore/${functionToId[detectionName]?string.computer}| _${detectionName}_ ><#rt>${'\n'}>Type: <${anomaly.anomalyURL}${anomaly.anomalyId}|${anomaly.anomalyType}>
  <#rt>${'\n'}>Start: ${anomaly.startDateTime} ${anomaly.timezone}
  <#rt>${'\n'}>Duration: ${anomaly.duration}
  <#if anomaly.dimensions?has_content>
  <#list anomaly.dimensions as dimension><#rt>${'\n'}>Dimension: ${dimension} </#list>
  </#if>
  <#rt>${'\n'}>Current: ${anomaly.currentVal}
  <#if anomaly.baselineVal == "+"><#rt>${'\n'}>Predicted: _Higher_ <#else><#rt>${'\n'}>Predicted: _Lower_ </#if>
</#if>
</#if>
</#list>
</#list>
</#list>
<#--  <#if alertConfigName?has_content>
:memo: ThirdEye Alert Service for *${alertConfigName}*.
</#if>  -->