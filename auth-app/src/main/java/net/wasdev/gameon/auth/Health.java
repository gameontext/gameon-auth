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
package net.wasdev.gameon.auth;

import java.io.IOException;

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
     * @see HttpServlet#HttpServlet()
     */
    public Health() {
        super();
        try {
            //grab the strings we didn't use @Resource for.
            InitialContext i = new InitialContext();
            facebookAppId = (String)i.lookup("facebookAppID");
            facebookSecretKey = (String)i.lookup("facebookSecret");
            gitHubKey = (String)i.lookup("gitHubOAuthKey");
            gitHubSecret = (String)i.lookup("gitHubOAuthSecret");
            googleKey = (String)i.lookup("googleOAuthConsumerKey");
            googleSecret = (String)i.lookup("googleOAuthConsumerSecret");
            twitterKey = (String)i.lookup("twitterOAuthConsumerKey");
            twitterSecret = (String)i.lookup("twitterOAuthConsumerSecret");
            //this property is only set when running locally, else it will be null.
            devMode = (String)i.lookup("developmentMode");
        }catch (NamingException e) {
            //ignore.. the strings will be null, and the health check will fail.
        }
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
              response.setStatus(503);
              response.getWriter().append("Service Unavailable");
          }
	}
}
