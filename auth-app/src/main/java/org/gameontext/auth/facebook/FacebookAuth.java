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
package org.gameontext.auth.facebook;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.scope.ExtendedPermissions;
import com.restfb.scope.ScopeBuilder;

@WebServlet("/FacebookAuth")
public class FacebookAuth extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "facebookAppID")
    String facebookAppId;
    @Resource(lookup = "authURL")
    String authURL;

    public FacebookAuth() {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ScopeBuilder scopeBuilder = new ScopeBuilder();
        scopeBuilder.addPermission(ExtendedPermissions.EMAIL);

        // tell facebook to send the user to this address once they have
        // authenticated.
        String callbackURL = authURL + "/FacebookCallback";

        FacebookClient client = new DefaultFacebookClient(Version.VERSION_2_5);
        String loginUrl = client.getLoginDialogUrl(facebookAppId, callbackURL, scopeBuilder);

        // redirect the user to facebook to be authenticated.
        response.sendRedirect(loginUrl);
    }

}
