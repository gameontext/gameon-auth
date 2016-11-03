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
package org.gameontext.auth.twitter;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

@WebServlet("/TwitterAuth")
public class TwitterAuth extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "twitterOAuthConsumerKey")
    String key;
    @Resource(lookup = "twitterOAuthConsumerSecret")
    String secret;
    @Resource(lookup = "authURL")
    String authURL;

    public TwitterAuth() {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ConfigurationBuilder c = new ConfigurationBuilder();
        c.setOAuthConsumerKey(key).setOAuthConsumerSecret(secret);

        Twitter twitter = new TwitterFactory(c.build()).getInstance();
        request.getSession().setAttribute("twitter", twitter);

        try {
            // twitter will tell the users browser to go to this address once
            // they are done authing.
            String callbackURL = authURL + "/TwitterCallback";

            // to initiate an auth request, twitter needs us to have a request
            // token.
            RequestToken requestToken = twitter.getOAuthRequestToken(callbackURL);

            // stash the request token in the session.
            request.getSession().setAttribute("requestToken", requestToken);

            // send the user to twitter to be authenticated.
            response.sendRedirect(requestToken.getAuthenticationURL());

        } catch (TwitterException e) {
            throw new ServletException(e);
        }

    }

}
