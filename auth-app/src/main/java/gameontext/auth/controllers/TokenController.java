package gameontext.auth.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import gameontext.auth.common.JWTSigner;

@RestController
@RequestMapping("/")
public class TokenController {

    @Autowired
    JWTSigner jwtSigner;

    @Value("${frontend.success.callback}")
	private String successUrl;

    //needs to be on a path that _does_ require auth, else we don't get the token
    @RequestMapping(value = "/auth/token", method = RequestMethod.GET)
    public ResponseEntity<String> ozzy(OAuth2AuthenticationToken token) throws Exception{

        System.out.println("Token endpoint invoked ");
        System.out.println("Token: "+token);

        String clientId = token.getAuthorizedClientRegistrationId();

        String id="";
        String name="";
        switch(clientId){
            case "dummy" : {
                id = "dummy:"+token.getPrincipal().getName();
                name = token.getPrincipal().getAttribute("login");
                break;
            }            
            case "github" : {
                id = "github:"+token.getPrincipal().getName();
                name = token.getPrincipal().getAttribute("login");
                break;
            }
            case "google" : {
                id = "google:"+token.getPrincipal().getName();
                name = token.getPrincipal().getAttribute("name");
                if(name!=null){
                    name = name.replaceAll(" ","");
                }else{
                    name = "Unknown";
                }
                break;
            }
            case "facebook" : {
                id = "facebook:"+token.getPrincipal().getName();
                name = token.getPrincipal().getAttribute("name");
                if(name!=null){
                    name = name.replaceAll(" ","");
                }else{
                    name = "Unknown";
                }
                break;
            }            
			default: {
				throw new IllegalArgumentException("Unknown Connection Type "+clientId);
			}
        }

        System.out.println("id: "+id);
        System.out.println("name: "+name);
        //String jwt = "id: "+id+"\nname: "+name; /
        //return new ResponseEntity<String>("Done. "+jwt, HttpStatus.OK);

        String jwt = jwtSigner.createJwt(id, name);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Location", successUrl + "/" + jwt);
        return new ResponseEntity<String>(headers,HttpStatus.FOUND);
        
        
    }
}