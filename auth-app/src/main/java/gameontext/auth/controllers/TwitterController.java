package gameontext.auth.controllers;

import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import gameontext.auth.common.JWTSigner;

@RestController
public class TwitterController {

    @Value("${gameon.twitterOAuthConsumerKey}")
    private String key;
    @Value("${gameon.twitterOAuthConsumerSecret}")
    private String secret;
    @Value("${frontend.auth.url}")
    private String authURL;

    @Value("${frontend.success.callback}")
	private String callbackSuccess;
    @Value("${frontend.failure.callback}")
    private String callbackFailure;

    @Autowired
    JWTSigner jwtSigner;   

    @GetMapping("/auth/TwitterAuth")
    public ResponseEntity<Object> tauth(HttpServletRequest httpServletRequest)
      throws IOException, URISyntaxException{

        ConfigurationBuilder c = new ConfigurationBuilder();
        c.setOAuthConsumerKey(key).setOAuthConsumerSecret(secret);

        Twitter twitter = new TwitterFactory(c.build()).getInstance();
        httpServletRequest.getSession().setAttribute("twitter", twitter);

        try {
            // twitter will tell the users browser to go to this address once
            // they are done authing.
            String callbackURL = authURL + "/TwitterCallback";

            // to initiate an auth request, twitter needs us to have a request
            // token.
            RequestToken requestToken = twitter.getOAuthRequestToken(callbackURL);

            // stash the request token in the session.
            httpServletRequest.getSession().setAttribute("requestToken", requestToken);

            // send the user to twitter to be authenticated
            URI redirect = new URI(requestToken.getAuthenticationURL());
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirect);

            return new ResponseEntity<>(headers,HttpStatus.FOUND);

        } catch (Exception e) {
            //don't return the real cause, in case it goes back to the end user.
            //instead dump it to the log
            e.printStackTrace();
            throw new IOException("Unable to invoke twitter authentication");
        }
    }   
    

    /**
     * Method that performs introspection on an AUTH string, and returns data as
     * a String->String hashmap.
     *
     * @param auth
     *            the authstring to query, as built by an auth impl.
     * @return the data from the introspect, in a map.
     * @throws IOException
     *             if anything goes wrong.
     */
    public Map<String, String> introspectAuth(String token, String tokensecret) throws IOException {

        Map<String, String> results = new HashMap<String, String>();

        ConfigurationBuilder c = new ConfigurationBuilder();
        c.setOAuthConsumerKey(key)
         .setOAuthConsumerSecret(secret)
         .setOAuthAccessToken(token)
         .setOAuthAccessTokenSecret(tokensecret)
         .setIncludeEmailEnabled(false)
         .setJSONStoreEnabled(true);

        Twitter twitter = new TwitterFactory(c.build()).getInstance();

        try {
            // ask twitter to verify the token & tokensecret from the auth string
            // if invalid, it'll throw a TwitterException
            User verified = twitter.verifyCredentials();

            // if it's valid, lets grab a little more info about the user.
            String name = verified.getName();

            results.put("valid", "true");
            results.put("id", "twitter:" + twitter.getId());
            results.put("name", name);

        } catch (TwitterException e) {
            results.put("valid", "false");
        }

        return results;
    }

    @GetMapping("/auth/TwitterCallback")
    public ResponseEntity<Object> callback(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException, URISyntaxException { 
        // twitter calls us back at this app when a user has finished authing with them.
        // when it calls us back here, it passes an oauth_verifier token that we can exchange
        // for a twitter access token.

        // we stashed our twitter & request token into the session, we'll need
        // those to do the exchange
        Twitter twitter = (Twitter) httpServletRequest.getSession().getAttribute("twitter");
        RequestToken requestToken = (RequestToken) httpServletRequest.getSession().getAttribute("requestToken");

        String redirectUrl="";

        // grab the verifier token from the request parms.
        String verifier = httpServletRequest.getParameter("oauth_verifier");
        if(verifier == null){
            //user elected to decline auth? redirect to fail url.
            redirectUrl=callbackFailure;
        }else{
            try {
                // clean up the session as we go (can leave twitter there if we need
                // it again).
                httpServletRequest.getSession().removeAttribute("requestToken");

                // swap the verifier token for an access token
                AccessToken token = twitter.getOAuthAccessToken(requestToken, verifier);
                Map<String, String> claims = introspectAuth(token.getToken(), token.getTokenSecret());

                // if auth key was no longer valid, we won't build a jwt. redirect
                // to failure url.
                if (!"true".equals(claims.get("valid"))) {
                    System.out.println("Login fail, sending user to failure "+callbackFailure);
                    redirectUrl=callbackFailure;
                } else {
                    String id = claims.get("id");
                    String name = claims.get("name");

                    String newJwt = jwtSigner.createJwt(id, name);                    

                    System.out.println("Login succeeded, sending user to success "+callbackSuccess+"/newJwt");
                    redirectUrl=callbackSuccess + "/" + newJwt;
                }

            } catch (TwitterException | JOSEException e) {
                e.printStackTrace();
                throw new IOException("Unable to process twitter callback");
            }
        }  
        
        try{
            System.out.println("Building redirect for "+redirectUrl);

            // send the user to twitter to be authenticated
            URI redirect = new URI(redirectUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirect);

            return new ResponseEntity<>(headers,HttpStatus.FOUND);
        } catch (Exception e){
            e.printStackTrace();
            throw new IOException("Unable to forward to redirect url");
        }
    }
}