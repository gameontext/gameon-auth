/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package org.gameontext.auth;

import java.io.IOException;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Health
 */
@WebServlet("/health")
public class Health extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private String callbackSuccess;
    private String callbackFailure;
    private String developmentMode;
    private String authURL;

    //dont use resource injection on these, or
    //init of the entire class will fail, and
    //then we won't work in local development mode.
    private String facebookAppId=null;
    private String facebookSecretKey=null;
    private String gitHubKey=null;
    private String gitHubSecret=null;
    private String googleKey=null;
    private String googleSecret=null;
    private String twitterKey=null;
    private String twitterSecret=null;

    @Inject
    private Kafka kafka;

    private boolean allIsWell = false;

    /**
     * Lookup a string from jndi, and return null if it couldn't be found for any reason.
     * @param name
     * @return
     */
    private String lookup(String name){
        try{
            InitialContext i = new InitialContext();
            return (String)i.lookup(name);
        }catch (NamingException e) {
        }
        return null;
    }

    /**
     * @see HttpServlet#HttpServlet()
     */
    public Health() {
        super();
        authURL = lookup("authURL");
        developmentMode = lookup("developmentMode");
        callbackFailure = lookup("authCallbackURLFailure");
        callbackSuccess = lookup("authCallbackURLSuccess");
        //grab the strings we didn't use @Resource for.
        facebookAppId = lookup("facebookAppID");
        facebookSecretKey = lookup("facebookSecret");
        gitHubKey = lookup("gitHubOAuthKey");
        gitHubSecret = lookup("gitHubOAuthSecret");
        googleKey = lookup("googleOAuthConsumerKey");
        googleSecret = lookup("googleOAuthConsumerSecret");
        twitterKey = lookup("twitterOAuthConsumerKey");
        twitterSecret = lookup("twitterOAuthConsumerSecret");
    }

    @PostConstruct
    private void verifyInit() {
        boolean badness = false;

        if ( developmentMode == null ) {
            developmentMode = "production";
        }
        Log.log(Level.INFO, this, "Development mode: {0}", developmentMode);

        // Rather than exiting early, we'll list all the things that are wrong in one shot.

        if (authURL == null ) {
            Log.log(Level.SEVERE, this, "Error identifying base authentication URL");
            badness = true;
        }

        if (callbackSuccess == null || callbackFailure == null) {
            Log.log(Level.SEVERE, this, "Error identifying callback URLs, please check environment variables");
            badness = true;
        }

        if (kafka == null ) {
            Log.log(Level.SEVERE, this, "Required kafka service not initialized");
            badness = true;
        }

        if ( !"development".equals(developmentMode) ) {
            badness |= facebookAppId == null ||
                    facebookSecretKey == null ||
                    gitHubKey == null ||
                    gitHubSecret == null ||
                    googleKey == null ||
                    googleSecret == null ||
                    twitterKey == null ||
                    twitterSecret == null;
            if ( badness ) {
                Log.log(Level.WARNING, this, "Error establishing social sign-on credentials. Please check environment variables: {0}", this.toString());
            }
        }

        allIsWell = !badness;
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         if ( allIsWell ) {
              response.setStatus(200);
              response.getWriter().append("{\"status\":\"UP\"}");
          } else {
              response.setStatus(503);
              response.getWriter().append("{\"status\":\"DOWN\"}");
          }
	}

	public String toString() {
	    // Format of %b translates null to false and not-null to true
	    return String.format("allIsWell=%b, authCallbackURLSuccess=%b, authCallbackURLFailure=%b, "
	            + "developmentMode=%b, "
	            + "facebookAppID=%b, facebookSecret=%b, "
	            + "gitHubOAuthKey=%b, gitHubOAuthSecret=%b, "
	            + "googleOAuthConsumerKey=%b, googleOAuthConsumerSecret=%b, "
	            + "twitterOAuthConsumerKey=%b, twitterOAuthConsumerSecret=%b, "
	            + "kafka=%b",
	            allIsWell, callbackSuccess, callbackFailure,
	            developmentMode,
	            facebookAppId, facebookSecretKey,
	            gitHubKey, gitHubSecret,
	            googleKey, googleSecret,
	            twitterKey, twitterSecret,
	            kafka
	            );
 	}

}
