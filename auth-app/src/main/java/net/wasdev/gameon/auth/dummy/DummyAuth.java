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
package net.wasdev.gameon.auth.dummy;

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

import net.wasdev.gameon.auth.JwtAuth;
import net.wasdev.gameon.auth.Log;

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
    String callbackSuccess;

    public DummyAuth() {
        super();
    }

    @PostConstruct
    private void verifyInit() {
        if (callbackSuccess == null) {
            System.err.println("Error finding auth callback url; please set this in your environment variables!");
        }
        
        // Convert https://127.0.0.1/#/login/callback to /#/login/callback
        callbackSuccess = callbackSuccess.substring(callbackSuccess.indexOf('#'));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String s = request.getParameter("dummyUserName");

        if (s == null) {
            s = "AnonymousUser";
        }

        Map<String, String> claims = new HashMap<String, String>();

        claims.put("id", "dummy." + s);
        claims.put("name", s);
        claims.put("email", s+"@DUMMYEMAIL.DUMMY");

        String newJwt = createJwt(claims);
        String callbackHost = request.getParameter("callbackHost");

        // debug.
        Log.log(Level.FINEST, this, "New User Authed: {0}", claims.get("id"));
        Log.log(Level.FINEST, this, "Remote callbackHost: {0}", callbackHost);
        Log.log(Level.FINEST, this, "Remote callbackSuccess: {0}", callbackSuccess);
        Log.log(Level.FINEST, this, "Result url: {0}", callbackHost + '/' + callbackSuccess);
        
        // Append /#/login/callback to the end of the original referer
        response.sendRedirect(callbackHost + '/' + callbackSuccess + '/' + newJwt);
        // 
    }

}
