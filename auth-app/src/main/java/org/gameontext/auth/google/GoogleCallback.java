/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.auth.google;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gameontext.auth.JwtAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

/**
 * Servlet implementation class googleCallback
 */
@WebServlet("/GoogleCallback")
public class GoogleCallback extends JwtAuth {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "googleOAuthConsumerKey")
    String key;
    @Resource(lookup = "googleOAuthConsumerSecret")
    String secret;
    @Resource(lookup = "authCallbackURLSuccess")
    String callbackSuccess;
    @Resource(lookup = "authCallbackURLFailure")
    String callbackFailure;

    private GoogleAuthorizationCodeFlow flow = null;

    public GoogleCallback() {
        super();
    }

    @PostConstruct
    private void verifyInit() {
        if (callbackSuccess == null) {
            System.err.println("Error finding webapp base URL; please set this in your environment variables!");
        }
    }

    /**
     * Method that performs introspection on an AUTH string, and returns data as
     * a String->String hashmap.
     *
     * @param auth
     *            the authstring to query, as built by an auth impl.
     * @return the data from the introspect, in a map.
     * @throws IOException
     *             if anything goes wrong.
     */
    public Map<String, String> introspectAuth(GoogleTokenResponse gResponse) throws IOException {
        Map<String, String> results = new HashMap<String, String>();

        Credential credential = flow.createAndStoreCredential(gResponse, null);

        try {
            // ask google to verify the response from the auth string
            // if invalid, it'll throw an exception
            HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(credential);
            GenericUrl url = new GenericUrl("https://www.googleapis.com/oauth2/v1/userinfo");
            HttpRequest infoRequest = requestFactory.buildGetRequest(url);

            infoRequest.getHeaders().setContentType("application/json");
            String jsonIdentity = infoRequest.execute().parseAsString();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode user = objectMapper.readTree(jsonIdentity);
            String id = user.get("id").asText();
            String name = user.get("name").asText();
            String email = user.get("email").asText();

            results.put("valid", "true");
            results.put("id", "google:" + id);
            results.put("name", name);
            results.put("email", email);

        } catch (Exception e) {
            results.put("valid", "false");
        }

        return results;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // google calls us back at this app when a user has finished authing
        // with them.
        // when it calls us back here, it passes an oauth_verifier token that we
        // can exchange
        // for a google access token.

        flow = (GoogleAuthorizationCodeFlow) request.getSession().getAttribute("google");
        String code = request.getParameter("code");

        StringBuffer callbackURL = request.getRequestURL();
        int index = callbackURL.lastIndexOf("/");
        callbackURL.replace(index, callbackURL.length(), "").append("/GoogleCallback");

        GoogleTokenResponse gResponse = flow.newTokenRequest(code).setRedirectUri(callbackURL.toString()).execute();
        Map<String, String> claims = introspectAuth(gResponse);

        // if auth key was no longer valid, we won't build a jwt. redirect back
        // to start.
        if (!"true".equals(claims.get("valid"))) {
            response.sendRedirect(callbackFailure);
        } else {
            String newJwt = createJwt(claims);

            System.out.println("New User Authed: " + claims.get("id"));
            response.sendRedirect(callbackSuccess + "/" + newJwt);
        }
    }
}
