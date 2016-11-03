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

    @Resource(lookup = "authCallbackURLSuccess")
    String callbackSuccess;
    @Resource(lookup = "authCallbackURLFailure")
    String callbackFailure;
	
    //dont use resource injection on these, or 
    //init of the entire class will fail, and 
    //then we won't work in local development mode.
    String facebookAppId=null;
    String facebookSecretKey=null;
    String gitHubKey=null;
    String gitHubSecret=null;
    String googleKey=null;
    String googleSecret=null;
    String twitterKey=null;
    String twitterSecret=null;
    String devMode;
    
    @Inject
    Kafka kafka;
    
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
            //ignore.. the string will be null, and the health check will fail appropriately.
            Log.log(Level.WARNING, this, "Auth healthcheck failed to lookup value for {0}",name);
        }
        return null;
    }
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Health() {
        super();
            //grab the strings we didn't use @Resource for.
            facebookAppId = lookup("facebookAppID");
            facebookSecretKey = lookup("facebookSecret");
            gitHubKey = lookup("gitHubOAuthKey");
            gitHubSecret = lookup("gitHubOAuthSecret");
            googleKey = lookup("googleOAuthConsumerKey");
            googleSecret = lookup("googleOAuthConsumerSecret");
            twitterKey = lookup("twitterOAuthConsumerKey");
            twitterSecret = lookup("twitterOAuthConsumerSecret");
            //this property is only set when running locally, else it will be null.
            devMode = lookup("developmentMode");
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         if ( kafka!=null
            && callbackFailure!=null 
            && callbackSuccess!=null
            && ( "development".equals(devMode) || 
                 (
                   facebookAppId!=null &&
                   facebookSecretKey!=null &&
                   gitHubKey!=null &&
                   gitHubSecret!=null &&
                   googleKey!=null &&
                   googleSecret!=null &&
                   twitterKey!=null &&
                   twitterSecret!=null
                  )
                )
            ) {
              response.setStatus(200);
              response.getWriter().append("OK ").append(request.getContextPath());
          } else {
              Log.log(Level.WARNING, this, "Auth Health is Bad. DevMode?{0}, kafka?{1}, cbFail{2}, cbOK{3}, fbApp{4}, fbSec{5}, ghKey{6}, ghSec{7}, gKey{8}, gSec{9}, tKey{10}, tSec{11}",
                      "'"+String.valueOf(devMode)+"'",
                      kafka!=null,
                      callbackFailure!=null,
                      callbackSuccess!=null,
                      facebookAppId!=null,
                      facebookSecretKey!=null,
                      gitHubKey!=null,
                      gitHubSecret!=null,
                      googleKey!=null,
                      googleSecret!=null,
                      twitterKey!=null,
                      twitterSecret!=null
                      );
              response.setStatus(503);
              response.getWriter().append("Service Unavailable");
          }
	}
}
