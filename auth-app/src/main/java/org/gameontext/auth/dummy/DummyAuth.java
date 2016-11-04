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
package org.gameontext.auth.dummy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gameontext.auth.JwtAuth;
import org.gameontext.auth.Log;

/**
 * A backend-less auth impl for testing.
 *
 * Accepts the username as a parameter, and returns a signed jwt for that
 * username.
 */
@WebServlet("/DummyAuth")
public class DummyAuth extends JwtAuth {
    private static final long serialVersionUID = 1L;

    /** Something like https://127.0.0.1/#/login/callback provided by the environment */
    @Resource(lookup = "authCallbackURLSuccess")
    private String callbackSuccess;
    
    /** Something like https://127.0.0.1/#/login/callback provided by the environment */
    @Resource(lookup = "developmentMode")
    private String developmentMode;

    private String callbackFragment;

    public DummyAuth() {
        super();
    }

    @PostConstruct
    private void verifyInit() {
        if ( developmentMode == null ) {
            developmentMode = "production";
        }
        
        // Convert https://127.0.0.1/#/login/callback to /#/login/callback
        callbackFragment = callbackSuccess.substring(callbackSuccess.indexOf('#'));
        Log.log(Level.FINEST, this, "Remote callbackFragment: {0}", callbackFragment);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String s = request.getParameter("dummyUserName");
        String callbackURL;

        if (s == null) {
            s = "AnonymousUser";
        }

        Map<String, String> claims = new HashMap<String, String>();

        claims.put("id", "dummy." + s);
        claims.put("name", s);
        claims.put("email", s+"@DUMMYEMAIL.DUMMY");
        Log.log(Level.FINEST, this, "New User Authed: {0}", claims.get("id"));

        String newJwt = createJwt(claims);
        
        if ( "development".equals(developmentMode) ) {
            String callbackHost = request.getParameter("callbackHost");
            callbackURL = callbackHost + '/' + callbackFragment + '/' + newJwt;

            Log.log(Level.FINEST, this, "Remote callbackHost: {0}", callbackHost);
            Log.log(Level.FINEST, this, "Result url: {0}", callbackURL);
        } else {
            callbackURL = callbackSuccess + '/' + newJwt;
            Log.log(Level.FINEST, this, "Remote callbackSuccess: {0}", callbackSuccess);
            Log.log(Level.FINEST, this, "Result url: {0}", callbackSuccess);
        }
        
        response.sendRedirect(callbackURL);
    }

}
