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
package gameontext.auth.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Handle generation of JWT auth credentials for the GameOn environment.
 */
@Component
public class JWTSigner {

    @Value("${jwt.keystore.location}")
    protected String keyStore;
    @Value("${jwt.keystore.password}")
    protected String keyStorePW;
    @Value("${jwt.keystore.alias}")
    protected String keyStoreAlias;

    protected static PrivateKey signingKey = null;

    /**
     * Obtain the key we'll use to sign the jwts we issue.
     *
     * @throws IOException
     *             if there are any issues with the keystore processing.
     */
    private synchronized void getKeyStoreInfo() throws IOException {
        try {
            // load up the keystore..
            FileInputStream is = new FileInputStream(keyStore);
            KeyStore signingKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            signingKeystore.load(is, keyStorePW.toCharArray());

            // grab the key we'll use to sign
            signingKey = (PrivateKey) signingKeystore.getKey(keyStoreAlias, keyStorePW.toCharArray());

        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        } catch (UnrecoverableKeyException e) {
            throw new IOException(e);
        }

    }

    /**
     * Obtain a JWT with the details passed, signed appropriately
     *
     * @param id UserID to encode.
     * @param name Human Readable Name for User
     * @return jwt encoded as string, ready to send to http.
     * @throws IOException
     *             if there are keystore issues.
     */
    public String createJwt(String id,
                            String name)throws IOException, JOSEException {
      return createJwt(id, name, null, null);
    }

    public String createJwt(String id,
                            String name,
                            String playerMode,
                            String storyid) throws IOException, JOSEException {
        if (signingKey == null) {
            getKeyStoreInfo();
        }

        Instant issuedAt = Instant.now().minus(12,ChronoUnit.HOURS);
        Instant expiresAt = Instant.now().plus(12,ChronoUnit.HOURS);

        // Spring API to build Jwt, no signing/serializing possible?
        // Jwt jwt = Jwt.withTokenValue("FISH")
        //     .header("kid","playerssl")
        //     .subject(id)
        //     .claim("id",id)
        //     .claim("name",name)
        //     .audience(Collections.singleton("client"))
        //     .issuedAt(issuedAt)
        //     .expiresAt(expiresAt)
        //     .build();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
            .subject(id)
            .claim("id",id)
            .claim("name",name);
        
        if(storyid!=null) {
            claimsBuilder = claimsBuilder.claim("story", storyid);
        }
        if(playerMode!=null) {
          claimsBuilder = claimsBuilder.claim("playerMode", playerMode);
        }
        
        JWTClaimsSet claims = claimsBuilder.audience("client")
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .build();

        SignedJWT jwt = new SignedJWT( new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("playerssl").build(), 
                                       claims);

        JWSSigner signer = new RSASSASigner(signingKey);
        
        jwt.sign(signer);

        return jwt.serialize();
    }

}
