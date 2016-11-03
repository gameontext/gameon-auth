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
package org.gameontext.auth.github;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/GitHubAuth")
public class GitHubAuth extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "gitHubOAuthKey")
    String key;
    @Resource(lookup = "authURL")
    String authURL;

    private final static String url = "https://github.com/login/oauth/authorize";

    public GitHubAuth() {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            UUID stateUUID = UUID.randomUUID();
            String state=stateUUID.toString();
            request.getSession().setAttribute("github", state);

            // google will tell the users browser to go to this address once
            // they are done authing.
            String callbackURL = authURL + "/GitHubCallback";

            String newUrl = url + "?client_id="+key+"&redirect_url="+callbackURL+"&scope=user:email&state="+state;

            // send the user to google to be authenticated.
            response.sendRedirect(newUrl);

        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}
