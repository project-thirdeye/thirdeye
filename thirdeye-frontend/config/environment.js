/* eslint-env node */
'use strict';

module.exports = function(environment) {
  const DEP_MODE = process.env.DEP_MODE;

  let ENV = {

    appName: 'ThirdEye',

    modulePrefix: 'thirdeye-frontend',

    environment,

    podModulePrefix: 'thirdeye-frontend/pods',

    email: 'thirdeye@thirdeye.com',

    devEmail: 'thirdeye@thirdeye.com',

    rootURL: '/app/',

    locationType: 'hash', 'ember-cli-mirage': {
      directory: 'app/mirage'
    },

    https_only: false,

    // timeZone: "America/Los_Angeles",
    timeZone: "Asia/Calcutta",

    moment: {
      includeTimezone: 'all'
    },

    piwik: {
      sid: 123,
      url: 'https://your-piwik.endpoint.com'
    },

    'ember-d3': {
      bundle: true
    },

    docs: {
      createAlert: "/link/to/create/alert/wiki",
      detectionConfig: "/link/to/DetectionConfiguration/wiki",
      subscriptionConfig: "/link/to/NotificationConfiguration/wiki",
      cubeWiki: "/link/to/cubeAlgorithm/wiki",
    },

    torii: {
      sessionServiceName: "session",
      allowUnsafeRedirect: true,
      providers: {
        "google-oauth2": {
          apiKey:
            "google-apikey",
          redirectUri: "thirdeye-redirecturl",
          scope: "email profile"
        },
     },
    },

    // used to split username if needed.
    userNameSplitToken: ' ',

    EmberENV: {

      FEATURES: {
        // Here you can enable experimental features on an ember canary build
        // e.g. 'with-controller': true
      },

      EXTEND_PROTOTYPES: {
        // Prevent Ember Data from overriding Date.parse.
        Date: false
      }
    },

    'ember-simple-auth':  {
      baseURL: '/app/#/rca'
    },

    APP: {
      // you can pass flags/options to your application instance
      // when it is created
    }
  };

  // TODO: Fix the api key to be loaded from environment secrets and not from here
  if (DEP_MODE === "prod"){
    ENV.torii.providers["google-oauth2"].apiKey="1007721360592-7e4kc7ohjtrtqb24hecstad3bcqbolnn.apps.googleusercontent.com";
    ENV.torii.providers["google-oauth2"].redirectUri="https://thirdeye.razorpay.com";
  }

  if (DEP_MODE === "stage"){
    ENV.torii.providers["google-oauth2"].apiKey="391316638671-qbubq6ip7icqovomv38fpdh4jkbm2c93.apps.googleusercontent.com";
    ENV.torii.providers["google-oauth2"].redirectUri="https://thirdeye.concierge.stage.razorpay.in";
  }
  if (environment === 'development') {
    ENV.rootURL = '/';
    ENV['ember-simple-auth'] = {
      baseURL: '/#/rca'
    };
    // necessary for local development
    // ENV.APP.LOG_ACTIVE_GENERATION = true;
    // ENV.APP.LOG_TRANSITIONS = true;
    // ENV.APP.LOG_TRANSITIONS_INTERNAL = true;
    // ENV.APP.LOG_VIEW_LOOKUPS = true;
  }

  if (environment === 'test') {
    ENV.rootURL = '/';

    // Testem prefers this...
    ENV.locationType = 'none';
    ENV.APP.autoboot = false;

    // keep test console output quieter
    ENV.APP.LOG_ACTIVE_GENERATION = false;
    ENV.APP.LOG_VIEW_LOOKUPS = false;

    ENV.APP.rootElement = '#ember-testing';
    ENV['ember-cli-mirage'] = {
      directory: 'app/mirage',
      autostart: true,
      enabled: true
    };
  }

  if (environment === 'production') {}

  return ENV;
};
