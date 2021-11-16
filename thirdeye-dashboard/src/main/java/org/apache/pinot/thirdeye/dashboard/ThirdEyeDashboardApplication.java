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

package org.apache.pinot.thirdeye.dashboard;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.bundles.redirect.PathRedirect;
import io.dropwizard.bundles.redirect.RedirectBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import org.apache.pinot.thirdeye.auth.ThirdEyeAuthFilter;
import org.apache.pinot.thirdeye.auth.ThirdEyePrincipal;
import org.apache.pinot.thirdeye.common.BaseThirdEyeApplication;
import org.apache.pinot.thirdeye.common.ThirdEyeSwaggerBundle;
import org.apache.pinot.thirdeye.common.time.TimeGranularity;
import org.apache.pinot.thirdeye.dashboard.configs.ResourceConfiguration;
import org.apache.pinot.thirdeye.dashboard.resources.AnomalyResource;
import org.apache.pinot.thirdeye.dashboard.resources.DashboardResource;
import org.apache.pinot.thirdeye.dashboard.resources.RootResource;
import org.apache.pinot.thirdeye.dashboard.resources.v2.AuthResource;
import org.apache.pinot.thirdeye.dashboard.resources.v2.RootCauseMetricResource;
import org.apache.pinot.thirdeye.dashboard.resources.v2.RootCauseResource;
import org.apache.pinot.thirdeye.dashboard.resources.v2.RootCauseSessionResource;
import org.apache.pinot.thirdeye.dashboard.resources.v2.RootCauseTemplateResource;
import org.apache.pinot.thirdeye.datasource.ThirdEyeCacheRegistry;
import org.apache.pinot.thirdeye.detection.annotation.registry.DetectionAlertRegistry;
import org.apache.pinot.thirdeye.detection.annotation.registry.DetectionRegistry;
import org.apache.pinot.thirdeye.model.download.ModelDownloaderManager;
import org.apache.pinot.thirdeye.tracking.RequestStatisticsLogger;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The type Third eye dashboard application.
 */
class ThirdEyeDashboardBaseApplication
    extends BaseThirdEyeApplication<ThirdEyeDashboardConfiguration> {
  private static final Logger LOG = LoggerFactory.getLogger(ThirdEyeDashboardBaseApplication.class);

  private RequestStatisticsLogger requestStatisticsLogger;
  private ModelDownloaderManager modelDownloaderManager;
  private Injector injector;

  @Override
  public String getName() {
    return "Thirdeye Dashboard";
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(Bootstrap<ThirdEyeDashboardConfiguration> bootstrap) {
    bootstrap.addBundle(new ViewBundle());
    bootstrap.addBundle(new HelperBundle());
    bootstrap.addBundle(new RedirectBundle(new PathRedirect("/", "/app/#/home")));
    bootstrap.addBundle(new AssetsBundle("/app/", "/app", "index.html", "app"));
    bootstrap.addBundle(new AssetsBundle("/assets", "/assets", null, "assets"));
    bootstrap.addBundle(new AssetsBundle("/assets/css", "/assets/css", null, "css"));
    bootstrap.addBundle(new AssetsBundle("/assets/js", "/assets/js", null, "js"));
    bootstrap.addBundle(new AssetsBundle("/assets/lib", "/assets/lib", null, "lib"));
    bootstrap.addBundle(new AssetsBundle("/assets/img", "/assets/img", null, "img"));
    bootstrap.addBundle(new AssetsBundle("/assets/data", "/assets/data", null, "data"));
    bootstrap.addBundle(new ThirdEyeSwaggerBundle());
  }

  @Override
  public void run(ThirdEyeDashboardConfiguration config, Environment env)
      throws Exception {
    LOG.info("isCors value {}", config.isCors());
    if (config.isCors()) {
      FilterRegistration.Dynamic corsFilter = env.servlets().addFilter("CORS", CrossOriginFilter.class);
      corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
      corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
      corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
      corsFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    if (config.getComponentPackageList() != null) {
      DetectionRegistry.setPackageList(config.getComponentPackageList());
      DetectionAlertRegistry.setPackageList(config.getComponentPackageList());
    }
    super.initDAOs();
    try {
      ThirdEyeCacheRegistry.initializeCaches(config);
    } catch (Exception e) {
      LOG.error("Exception while loading caches", e);
    }

    final JerseyEnvironment jersey = env.jersey();
    injector = Guice.createInjector(new ThirdEyeDashboardModule(config, env, DAO_REGISTRY));
    Stream.of(
        AnomalyResource.class,
        DashboardResource.class,
        RootCauseMetricResource.class,
        RootCauseSessionResource.class,
        RootCauseTemplateResource.class,
        RootResource.class
    )
        .map(c -> injector.getInstance(c))
        .forEach(jersey::register);

    env.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    env.getObjectMapper().registerModule(makeMapperModule());

    try {
      // root cause resource
      if (config.getRootCause() != null) {
        jersey.register(injector.getInstance(RootCauseResource.class));
      }

      // Load external resources
      if (config.getResourceConfig() != null) {
        List<ResourceConfiguration> resourceConfigurations = config.getResourceConfig();
        for (ResourceConfiguration resourceConfiguration : resourceConfigurations) {
          jersey.register(Class.forName(resourceConfiguration.getClassName()));
          LOG.info("Registering resource [{}]", resourceConfiguration.getClassName());
        }
      }
    } catch (Exception e) {
      LOG.error("Error loading the resource", e);
    }

    // Authentication
    if (config.getAuthConfig() != null) {
      jersey.register(injector.getInstance(ThirdEyeAuthFilter.class));
      jersey.register(new AuthValueFactoryProvider.Binder<>(ThirdEyePrincipal.class));
      jersey.register(injector.getInstance(AuthResource.class));
    }

    if (config.getModelDownloaderConfig() != null) {
      modelDownloaderManager = injector.getInstance(ModelDownloaderManager.class);
      modelDownloaderManager.start();
    }

    env.lifecycle().manage(lifecycleManager());
  }

  private Managed lifecycleManager() {
    return new Managed() {
      @Override
      public void start() {
        requestStatisticsLogger = new RequestStatisticsLogger(new TimeGranularity(1, TimeUnit.DAYS));
        requestStatisticsLogger.start();
      }

      @Override
      public void stop() {
        if (requestStatisticsLogger != null) {
          requestStatisticsLogger.shutdown();
        }
        if (modelDownloaderManager != null) {
          modelDownloaderManager.shutdown();
        }
      }
    };
  }

}


public class ThirdEyeDashboardApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ThirdEyeDashboardApplication.class);
  /**
   * The entry point of application.
   *
   * @param args the input arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    String thirdEyeConfigDir = "thirdeye-dashboard/config";
    if (args.length >= 1) {
      thirdEyeConfigDir = args[0];
    }
    LOG.info("Using config path '{}'", thirdEyeConfigDir);

    System.setProperty("dw.rootDir", thirdEyeConfigDir);
    String dashboardApplicationConfigFile = thirdEyeConfigDir + "/" + "dashboard.yml";
    new ThirdEyeDashboardBaseApplication().run("server", dashboardApplicationConfigFile);
  }
}
