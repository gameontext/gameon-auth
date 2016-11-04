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
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.gameontext.auth.JwtAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

/**
 * Servlet implementation class googleCallback
 */
@WebServlet("/GitHubCallback")
public class GitHubCallback extends JwtAuth {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "gitHubOAuthKey")
    String key;
    @Resource(lookup = "gitHubOAuthSecret")
    String secret;
    @Resource(lookup = "authCallbackURLSuccess")
    String callbackSuccess;
    @Resource(lookup = "authCallbackURLFailure")
    String callbackFailure;

    public GitHubCallback() {
        super();
    }

    @PostConstruct
    private void verifyInit() {
        if (callbackSuccess == null) {
            System.err.println("Error finding webapp base URL; please set this in your environment variables!");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //ok, we have our code.. so the user has agreed to our app being authed.
        String code = request.getParameter("code");

        String state = (String) request.getSession().getAttribute("github");

        //now we need to invoke the access_token endpoint to swap the code for a token.
        StringBuffer callbackURL = request.getRequestURL();
        int index = callbackURL.lastIndexOf("/");
        callbackURL.replace(index, callbackURL.length(), "").append("/GitHubCallback");

        HttpRequestFactory requestFactory;
        try {
            //we'll ignore the ssl cert of the github server for now..
            //eventually we may add this to the player truststore..
            requestFactory = new NetHttpTransport.Builder().doNotValidateCertificate().build().createRequestFactory();

            //prepare the request..
            GenericUrl url = new GenericUrl("https://github.com/login/oauth/access_token");
            //set the client id & secret from the injected environment.
            url.put("client_id", key);
            url.put("client_secret", secret);
            //add the code we just got given..
            url.put("code", code);
            url.put("redirect_uri", callbackURL );
            url.put("state", state);

            //now place the request to github..
            HttpRequest infoRequest = requestFactory.buildGetRequest(url);
            HttpResponse r = infoRequest.execute();
            String resp = "failed.";
            if(r.isSuccessStatusCode()){

                //response comes back as query param encoded data.. we'll grab the token from that...
                resp = r.parseAsString();

                //http client way to parse query params..
                List<NameValuePair> params = URLEncodedUtils.parse(resp, Charset.forName("UTF-8"));
                String token = null;
                for(NameValuePair param : params){
                    if("access_token".equals(param.getName())){
                        token = param.getValue();
                    }
                }

                if(token!=null){
                    //great, we have a token, now we can use that to request the user profile..
                    GenericUrl query = new GenericUrl("https://api.github.com/user");
                    query.put("access_token", token);

                    HttpRequest userRequest = requestFactory.buildGetRequest(query);
                    HttpResponse u = userRequest.execute();
                    if(u.isSuccessStatusCode()){
                        //user profile comes back as json..
                        resp = u.parseAsString();

                        //use om to parse the json, so we can grab the id & name from it.
                        ObjectMapper om = new ObjectMapper();
                        JsonNode jn = om.readValue(resp,JsonNode.class);

                        Map<String, String> claims = new HashMap<String,String>();
                        claims.put("valid", "true");
                        //github id is a number, but we'll read it as text incase it changes in future..
                        claims.put("id", "github:" + jn.get("id").asText());
                        claims.put("name", jn.get("login").textValue());

                        GenericUrl emailQuery = new GenericUrl("https://api.github.com/user/emails");
                        emailQuery.put("access_token", token);
                        HttpRequest emailRequest = requestFactory.buildGetRequest(emailQuery);
                        HttpResponse er = emailRequest.execute();

                        claims.put("email","unknown");
                        if(er.isSuccessStatusCode()){
                          resp = er.parseAsString();
                          JsonNode en = om.readValue(resp,JsonNode.class);
                          if(en.isArray()){
                            for ( JsonNode email : en) {
                              Boolean primary = Boolean.valueOf(email.get("primary").booleanValue());
                              if(primary){
                                claims.put("email", email.get("email").textValue());
                              }
                            }
                          }
                        }

                        String jwt = createJwt(claims);

                        //log for now, we'll clean this up once it's all working =)
                        System.out.println("New User Authed: " + claims.get("id")+" jwt "+jwt);
                        response.sendRedirect(callbackSuccess + "/" + jwt);

                    }else{
                        System.out.println(u.getStatusCode());
                        response.sendRedirect(callbackFailure);
                    }
                }else{
                    System.out.println("did not find token in github response "+resp);
                    response.sendRedirect(callbackFailure);
                }
            }else{
                response.sendRedirect(callbackFailure);
            }

        } catch (GeneralSecurityException e) {
            throw new ServletException(e);
        }

    }
}
