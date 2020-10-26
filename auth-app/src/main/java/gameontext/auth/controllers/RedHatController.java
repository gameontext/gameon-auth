package gameontext.auth.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gameontext.auth.common.JWTSigner;

@RestController
public class RedHatController {
  private final String authURL;
  private final String callbackSuccess;
  private final String callbackFailure;
  private final JWTSigner jwtSigner;
  private final Map<String,GroupInfo> groupByName;
  
  private static final class GroupInfo {
    private final String groupName;
    private final String groupPwd;
    private final String playerPrefix;
    public GroupInfo(String seed) {
      if(!seed.contains(":")) {
        throw new IllegalStateException("Bad config string, expected groupname:groupinfo:playeridprefix, got "+seed);
      }
      String[] parts = seed.split(":");
      if(parts.length!=3) {
        throw new IllegalStateException("Bad config string, expected groupname:groupinfo:playeridprefix, got "+seed);
      }
      groupName = parts[0];
      groupPwd = parts[1];
      playerPrefix = parts[2];
    }
    public String getGroupName() {
      return groupName;
    }
    public String getGroupPwd() {
      return groupPwd;
    }
    public String getPlayerPrefix() {
      return playerPrefix;
    }
  }

  @Autowired
    public RedHatController( @Value("${frontend.auth.url}") String authUrl,
                             @Value("${frontend.success.callback}")String callbackSuccess,
                             @Value("${frontend.failure.callback}")String callbackFailure,
                             @Value("${gameon.redhatauth}")String redhatauth, 
                             @Autowired JWTSigner jwtSigner) {
    this.authURL = authUrl;
    this.callbackSuccess = callbackSuccess;
    this.callbackFailure = callbackFailure;
    this.jwtSigner = jwtSigner;
    this.groupByName = new HashMap<>();
    
    //config oddities.. need to remove quotes from config string if present.
    if(redhatauth.startsWith("\"")&&redhatauth.endsWith("\"")) {
      redhatauth = redhatauth.substring(1,redhatauth.length()-2);
    }
    if(redhatauth.startsWith("'")&&redhatauth.endsWith("'")) {
      redhatauth = redhatauth.substring(1,redhatauth.length()-2);
    }
    //parse config string into group/password/prefix infos.
    String ginfos[] = redhatauth.split(",");
    for(String g: ginfos) {
      GroupInfo gi = new GroupInfo(g);
      System.out.println("Adding group "+gi.getGroupName()+" "+gi.getGroupPwd()+" "+gi.playerPrefix);
      this.groupByName.put(gi.getGroupName(), gi);
    }
    
  }
  
  @GetMapping("/auth/RedHatAuth")
  public ResponseEntity<Object> callback(@RequestParam String groupid, @RequestParam String grouppwd, @RequestParam String name) throws IOException, URISyntaxException {
    //System.out.println("Auth called for "+groupid+" "+grouppwd+" "+name);
    
    String redirectUrl = "";
    try {
      if(!groupByName.containsKey(groupid)) {
        System.out.println("Unknown group id "+groupid);
        redirectUrl = callbackFailure;
      }else {
        GroupInfo gi = groupByName.get(groupid);
        if(!grouppwd.equals(gi.getGroupPwd()) || !name.startsWith(gi.getPlayerPrefix())) {
          redirectUrl = callbackFailure;
        } else {
          String id = "redhat:" + groupid + ":" + name;
          String newJwt = jwtSigner.createJwt(id, name, "redhat.colab.breakingthecode", "guided");
          //System.out.println("Login succeeded, sending user to success " + callbackSuccess + "/newJwt");
          redirectUrl = callbackSuccess + "/" + newJwt;
        }
      }
      System.out.println("Building redirect for " + redirectUrl);
      URI redirect = new URI(redirectUrl);
      HttpHeaders headers = new HttpHeaders();
      headers.setLocation(redirect);
      return new ResponseEntity<>(headers, HttpStatus.FOUND);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Unable to forward to redirect url");
    }
  }
}