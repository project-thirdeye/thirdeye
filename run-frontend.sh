#!/bin/bash
echo "*******************************************************"
echo "Launching ThirdEye Dashboard in demo mode"
echo "*******************************************************"

java -Dlog4j.configurationFile=log4j2.xml -cp "thirdeye-dist/target/thirdeye-dist-1.0.0-SNAPSHOT-dist/thirdeye-dist-1.0.0-SNAPSHOT/lib/*" org.apache.pinot.thirdeye.dashboard.ThirdEyeDashboardApplication "thirdeye-dashboard/config"
