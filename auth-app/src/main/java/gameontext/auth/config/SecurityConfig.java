package gameontext.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
 
    @Value("${frontend.success.callback}")
    private String successUrl;

    @Value("${frontend.failure.callback}")
    private String failureUrl;

    @Value("${frontend.auth.url}")
    private String authUrl;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

      // '/login' is allowed for local testing.. unreachable when deployed behind proxy/loadbalancer
      //everything else lives under /auth for compat with old auth paths via proxy/loadbalancers
        http.authorizeRequests()
         .antMatchers("/",                 //root (not used)
                      "/resources/**",     //static resources (not used)
                      "/auth/DummyAuth",   //old auth compat url
                      "/auth/GoogleAuth",  //old auth compat url
                      "/auth/TwitterAuth", //old auth compat url
                      "/auth/GitHubAuth",  //old auth compat url
                      "/auth/FacebookAuth",//old auth compat url
                      "/auth/health",      //health check for docker/k8s
                      "/auth/dummy/**",    //the new dummy auth provider (and callback/token/userinfo endpoints)
                      "/auth/oauth2/**",   //the new oauth2 initiate urls (per provider)
                      "/login/**"          //built-in login select page (handy during local debug)
                      ).permitAll()        
         .anyRequest().authenticated()
         .and()
           .csrf().ignoringAntMatchers("/auth/dummy/fake/**")         
         .and()
           .oauth2Login()          
             .authorizationEndpoint()
               .baseUri("/auth/oauth2/authorization")
             .and()
             .redirectionEndpoint()
               .baseUri("/auth/oauth2/code/*")
             .and()
              .failureUrl(failureUrl)
              .defaultSuccessUrl(authUrl+"/token", true);            
    }
}
