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
import java.util.Arrays;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

@WebServlet("/GoogleAuth")
public class GoogleAuth extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "googleOAuthConsumerKey")
    String key;
    @Resource(lookup = "googleOAuthConsumerSecret")
    String secret;
    @Resource(lookup = "authURL")
    String authURL;

    public GoogleAuth() {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport httpTransport = new NetHttpTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow(
                httpTransport,
                jsonFactory,
                key,
                secret,
                Arrays.asList("https://www.googleapis.com/auth/userinfo.profile","https://www.googleapis.com/auth/userinfo.email"));

        try {
            // google will tell the users browser to go to this address once
            // they are done authing.
            String callbackURL = authURL + "/GoogleCallback";
            request.getSession().setAttribute("google", flow);

            String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(callbackURL).build();
            // send the user to google to be authenticated.
            response.sendRedirect(authorizationUrl);

        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}
