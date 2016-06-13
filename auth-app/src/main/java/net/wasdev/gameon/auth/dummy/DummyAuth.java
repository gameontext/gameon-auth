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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.wasdev.gameon.auth.JwtAuth;

/**
 * A backend-less auth impl for testing.
 *
 * Accepts the username as a parameter, and returns a signed jwt for that
 * username.
 */
@WebServlet("/DummyAuth")
public class DummyAuth extends JwtAuth {
    private static final long serialVersionUID = 1L;

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

        String newJwt = createJwt(claims);

        // debug.
        System.out.println("New User Authed: " + claims.get("id"));

        response.sendRedirect(callbackSuccess + "/" + newJwt);

    }

}
