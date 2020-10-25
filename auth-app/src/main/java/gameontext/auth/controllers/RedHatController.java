package gameontext.auth.controllers;

import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import gameontext.auth.common.JWTSigner;

@RestController
public class RedHatController {

    @Value("${frontend.auth.url}")
    private String authURL;

    @Value("${frontend.success.callback}")
	private String callbackSuccess;
    @Value("${frontend.failure.callback}")
    private String callbackFailure;

    @Autowired
    JWTSigner jwtSigner;   

    @GetMapping("/auth/RedHatAuth")
    public ResponseEntity<Object> auth()
      throws IOException, URISyntaxException { 
        URI redirect = new URI("/auth/RedHatAuth/index.html");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirect);
        return new ResponseEntity<>(headers,HttpStatus.FOUND);          
    }

    @GetMapping("/auth/RedHatCallback")
    public ResponseEntity<Object> callback(@RequestParam String groupid, @RequestParam String grouppwd, @RequestParam String name)
      throws IOException, URISyntaxException { 

        String redirectUrl="";
        try {

            if (!"fish".equals(grouppwd)) {
                    System.out.println("Login fail, sending user to failure "+callbackFailure);
                    redirectUrl=callbackFailure;
            }else{

                String id = "redhat:"+groupid+":"+name;
                String newJwt = jwtSigner.createJwt(id, name);                    

                System.out.println("Login succeeded, sending user to success "+callbackSuccess+"/newJwt");
                redirectUrl=callbackSuccess + "/" + newJwt;
            }    
            System.out.println("Building redirect for "+redirectUrl);
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