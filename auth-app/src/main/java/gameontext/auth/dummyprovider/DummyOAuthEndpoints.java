package gameontext.auth.dummyprovider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@ConditionalOnProperty( prefix="gameon.", value="mode", havingValue="development")
@RestController
public class DummyOAuthEndpoints {

    public final static boolean debugPrint=true;

    //static passphrase all our auth codes & access tokens will be secured with. 
    //remember this is just a dummy auth impl =)
    public final byte[] sharedSecret = "THISISALONGSECRETFORUSEBYDUMMYAUTHPROVIDERSHH".getBytes();

    public static class TokenReply {
        private String access_token;
        private String token_type;

        @JsonProperty("access_token")
        public String getAccess_Token(){ return access_token; }
        @JsonProperty("token_type")
        public String getToken_Type(){ return token_type; }

        public void setAccess_Token(String p){ this.access_token = p; }
        public void setToken_Type(String p){ this.token_type = p; }

    }

    //Called when Spring redirects from /oauth2/authorization/dummy to this endpoint, because this endpoint is configured as
    //the dummy-provider.authorization-uri for the dummy provider.
    @GetMapping(value="/auth/dummy/fake/auth")
    public ResponseEntity<Object> auth(@RequestParam Map<String,String> allRequestParams, HttpServletResponse httpServletResponse)
      throws IOException, URISyntaxException, KeyLengthException, JOSEException {
        if(debugPrint)System.out.println("***\n***DummyAuth: auth GET Invoked params:: "+allRequestParams);
        String redirectUrl = allRequestParams.get("redirect_uri");

        if(redirectUrl==null){
            return new ResponseEntity<>("Missing redirect URI", HttpStatus.BAD_REQUEST);
        }      

        //TODO: here we could/shoud return a simple html form that posts back to a new endpoint on this controller
        //that would collect the username, and then the rest of this method would become the body of the new endpoint
        //for now, we only need 1 dummy user, so we'll skip the intermediate endpoint, and just send the user back
        //to spring for the oauth2 flow to continue.

        String name = "AnonymousUser";
        String id = "dummy."+name;
        

        Instant issuedAt = Instant.now().minus(12,ChronoUnit.HOURS);
        Instant expiresAt = Instant.now().plus(12,ChronoUnit.HOURS);
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(id)
            .claim("client_id",id)
            .claim("name",name)
            .audience("code")
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .build();
        
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        JWSSigner signer = new MACSigner(sharedSecret);   
        jwt.sign(signer); 

        redirectUrl+="?state="+allRequestParams.get("state");
        redirectUrl+="&code="+jwt.serialize();

        URI redirect = new URI(redirectUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirect);
        if(debugPrint)System.out.println("***\n***DummyAuth: Returning redirect to "+redirectUrl);

        //send the browser 'back' to spring, (here we're being a provider, like facebook/github etc would be)
        return new ResponseEntity<>(headers,HttpStatus.FOUND);

    }

    //Called when Spring redirects the browser having completed the 'auth' step above, and has obtained the opaque 'authentication_code'
    @RequestMapping(value="/auth/dummy/fake/token", method = RequestMethod.POST, produces = "application/json",  consumes = "application/x-www-form-urlencoded")
    public @ResponseBody TokenReply token(@RequestParam Map<String,String> allRequestParams) throws Exception{
        if(debugPrint)System.out.println("***\n***DummyAuth: token POST Invoked "+allRequestParams);
        TokenReply t = new TokenReply();

        String encodedJwt = allRequestParams.get("code");

        //defend a little against invocations made outside of the regular oauth flow.
        if(encodedJwt==null){
            throw new Exception("Missing authentication code");
        }         

        //is the authentication_code valid? ours is handily a jwt, so we can just verify it and move on.
        JWSVerifier verifier = new MACVerifier(sharedSecret);
        SignedJWT jwt = SignedJWT.parse(encodedJwt);
        jwt.verify(verifier);

        //make sure the authentication_code we got was supposed to be used as a code 
        //we're going to reuse the jwt as the access token, but change the audience to prevent it being
        //used incorrectly if replayed.
        if(!jwt.getJWTClaimsSet().getAudience().contains("code")){
            throw new Exception("Bad audience in authentication code");
        }

        //rebuild the jwt with 'access-token' audience.
        JWTClaimsSet newClaims = new JWTClaimsSet.Builder(jwt.getJWTClaimsSet()).audience("access-token").build();
        jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), newClaims);
        JWSSigner signer = new MACSigner(sharedSecret);   
        jwt.sign(signer); 

        //still here? code was valid, re-use it as the access token. Normally 
        t.setAccess_Token(jwt.serialize());
        t.setToken_Type("bearer");
        if(debugPrint)System.out.println("***\n***DummyAuth: Returning token response : "+jwt.serialize());
        return t;
    }

    @GetMapping(value="/auth/dummy/fake/userinfo")
    @ResponseBody
    public Map<String, Object> userinfo(@RequestHeader("Authorization") String authHeaderValue) throws Exception{
        if (authHeaderValue == null || !authHeaderValue.startsWith("Bearer ")) {
            throw new Exception("No JWT token found in request headers");
        }
        String authToken = authHeaderValue.substring(7);

        if(debugPrint)System.out.println("***\n***DummyAuth: userinfo GET invoked : "+authToken);
        
        JWSVerifier verifier = new MACVerifier(sharedSecret);
        SignedJWT jwt = SignedJWT.parse(authToken);
        jwt.verify(verifier);

        if(debugPrint)System.out.println("***\n***DummyAuth: userinfo token verified "+jwt);

        //make sure the access token we got was supposed to be used as a token 
        //we use the jwt as the authentication code too, and set the audience to indicate which
        //use it's intended for. 
        if(!jwt.getJWTClaimsSet().getAudience().contains("access-token")){
            throw new Exception("Bad audience in access token");
        }

        String username = jwt.getJWTClaimsSet().getClaim("name").toString();

        if(debugPrint)System.out.println("***\n***DummyAuth: userinfo token verified, username is : "+username);

        Map<String, Object> users = new LinkedHashMap<>();
        users.put("login", username);
        users.put("id", jwt.getJWTClaimsSet().getSubject());
        if(debugPrint)System.out.println("***\n***DummyAuth: Returning userinfo response : "+users.toString());
        return users;
    }
  

}

