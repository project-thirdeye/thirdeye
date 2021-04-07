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

package org.apache.pinot.thirdeye.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.pinot.thirdeye.datalayer.bao.SessionManager;
import org.apache.pinot.thirdeye.datalayer.dto.SessionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

public class ThirdEyeGoogleAuthenticator implements Authenticator<ThirdEyeCredentials, ThirdEyePrincipal> {
	private static final Logger LOG = LoggerFactory.getLogger(ThirdEyeGoogleAuthenticator.class);
	private final SessionManager sessionDAO;
	HashMap<String, String> configMap = new HashMap<String, String>();

	// ThirdEyeGoogleAuthenticator :
	public ThirdEyeGoogleAuthenticator(SessionManager sessionDAO, HashMap<String, String> configMap) {
		this.sessionDAO = sessionDAO;
		this.configMap = configMap;
	}

	private Optional<ThirdEyePrincipal> googleAuthenticate(String authCode) {
		LOG.info("Authenticating via Google Login...");
		String tokenURL = configMap.get("tokenURL");
		String clientId = configMap.get("clientId");
		String clientSecret = configMap.get("clientSecret");
		String redirectURL = configMap.get("redirectURL");
		GoogleTokenResponse tokenResponse = null;
		try {
			tokenResponse = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(),
					JacksonFactory.getDefaultInstance(), tokenURL, clientId, clientSecret, authCode, redirectURL)
							.execute();
		} catch (IOException e) {
			LOG.error("Invalid google token response in googleAuthenticate. Check secrets");
			e.printStackTrace();
		}

		if (tokenResponse != null) {
			try {
				GoogleIdToken idToken = tokenResponse.parseIdToken();
				GoogleIdToken.Payload payload = idToken.getPayload();
				String userId = payload.getSubject();
				String email = payload.getEmail();
				// userId as name and sessionKey
				ThirdEyePrincipal principal = new ThirdEyePrincipal(email, userId);
				principal.setEmailId(email);
				return Optional.of(principal);
			} catch (IOException e) {
				LOG.error("Invalid google token id");
				e.printStackTrace();
			}
		}
		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ThirdEyePrincipal> authenticate(ThirdEyeCredentials credentials) throws AuthenticationException {
		try {
			if (StringUtils.isNotBlank(credentials.getToken())) {
				SessionDTO sessionDTO = this.sessionDAO.findBySessionKey(credentials.getToken());
				if (sessionDTO != null && System.currentTimeMillis() < sessionDTO.getExpirationTime()) {
					return Optional.of(new ThirdEyePrincipal(credentials.getPrincipal(), credentials.getToken()));
				}
			}
			// getCode() is authorization code from google authorization API. First time call.
			String authCode = credentials.getCode(); 
			if (StringUtils.isBlank(authCode)) {
				LOG.error("Unable to authenticate google. Empty authorization code");
				return Optional.empty();
			} else {
				return googleAuthenticate(authCode);
			}
		} catch (Exception e) {
			throw new AuthenticationException(e);
		}
	}
}
