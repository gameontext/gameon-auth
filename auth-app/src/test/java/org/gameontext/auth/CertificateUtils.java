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

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.junit.Test;

import com.google.api.client.util.PemReader;
import com.google.api.client.util.PemReader.Section;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

public class CertificateUtils {

    //An RSA PrivateKey/Certificate for use during testing.
    //the key isn't encrypted, as that would be a little pointless
    //since the private key, and all credentials for it are in this
    //one file.
    //We ONLY use this key pair during unit test.
    final static String certString =
            "Bag Attributes\n"+
                    "    localKeyID: 31 34 35 36 39 32 38 39 39 31 35 35 30 \n"+
                    "    friendlyName: default\n"+
                    "Key Attributes: <No Attributes>\n"+
                    "-----BEGIN PRIVATE KEY-----\n"+
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC2IJ0THYPvm669\n"+
                    "AuyCHP8GUmQB0r9MeW1sFQIRWdkQRijNIKa60sqN58b7S11Mc8ch+CXQ3bMZM4CR\n"+
                    "qX8GAc/0Gd/kkQhen7Q8JGnM+AzV51vqgaYV4A3A2i/ruToZpUD2VCl4nozhoqjw\n"+
                    "AzPivRBwzCi44dR2AnFAfEEH8dPvizP11nuS5fCBMQy8uhm2Y4JXg1uW0szDkbj5\n"+
                    "E6r3JKKs79d0iDfWNogmCXxs95fFyx3avU/n7X7uYku0QXpmuXtNjOe+92Sq7kq/\n"+
                    "0BWmJFfhks2GJi7ea0MIKd4AxiYuJCQahLx9L8DLdxlD5qV42YKPog1n3ETYCpIX\n"+
                    "q3MZ++5tAgMBAAECggEAUfgruca28shmxLrkJ0tVnErIp+lqH8km7lYmMBj4ENMC\n"+
                    "2g+v+rWUZHnEnKU2wIn7Pdapbm/Zg6YiX2yhttpp9bsPgZek5LGMNOVOmOmrHTqb\n"+
                    "q9feIEpO5lVM7BLZi2FM85C9eYQidAr5bcyDNbFSDPJWAZ/iN5qxzgweWK0GbfC8\n"+
                    "4GlzJHJz3Jj/7y2s2n22vxYvqKPfDI3hWNQcr6tXs8QxHtUX1xl0JKQU62itUMI1\n"+
                    "vWgeiMQ56rxBi9w+h3YBRVFgxOjNsPncEesNe0IlHXWEWu2GSopYoHVck08iezxK\n"+
                    "q8ULPfPcJR5IUsUQQM0F1NSgWi8VD5BBMJfIlsVOIQKBgQD1jkFkSLHdsvj7DwrB\n"+
                    "RoTElRQ8nNTI1/Pw9URm+29vx+rPybu9Rm+kCJKCkoZqH275Fa6/ZWWF6vGGmtsA\n"+
                    "EosHlzz+cf7uV2eig8bpvO35jvX9cmIvj4krcHM/JdSveyHm3tvXXEraUiBKkZxm\n"+
                    "LH/Y0OPvkO4cMPWXTpUhafZ/AwKBgQC937dcL4/chF6Qq42iqNo3pHcvppKJFuZb\n"+
                    "bsI706UGviw3noZLGjci5jGrDRRoy182M2ec7kR13P2LDVaOkjcf8msTh+fYWbWe\n"+
                    "nrU7aMOpYiSJ8iTGyfuL8DBLCyrD42oPVlMVkRsb/5oKeCCzM2DB0oJ/eQ/FYAF2\n"+
                    "Xn+1VjFpzwKBgQClL7BPvRNiF36krWbHxB+Wes8lQz9laNjidKwyNtytLqiIZaYU\n"+
                    "2uhJSbb9fYJMq56kk3B9ssFMCFO4AD5o2xCJ57SRWrBrN4Mw8UMDhCP2qLRUbfkd\n"+
                    "E4rsHPZ6OYHNFqEkxTDQvHZiTbMJVtEGbtMGUOe1BiMX9duQkL2Dv9uhbwKBgAG0\n"+
                    "HCULmDLWTTLnFyI6eZq+MwOObwoj1nVDjSKUR4rD8gmdtn6+AXiisBdkyqYWDQij\n"+
                    "dW6HBL45+VxiBkDJNw1mU2eddIsQYvzFV8LssbS3WLSUI5hU/5jF0ukZdIzFYZI5\n"+
                    "qA0tfBzIMk2dvk1dTKTwipMyNt4CeoDhYCv0VgUpAoGAMtoOF+t6iRDpNpjq4tCH\n"+
                    "2sdfqOnjGIeLQ5ciJoT5Xryz/Ww9yt8YWDXr5hZOPEMeNONtWXk5WxafsVLJ0LZf\n"+
                    "ko8KiqP6058lxHHtiJadIUyAVX4KpLmojEMH20EXW3mvxRf9ch4ITkHa9AGkW+Hl\n"+
                    "qAI9LIpl9U2dsMskXaAc8NM=\n"+
                    "-----END PRIVATE KEY-----\n"+
                    "Bag Attributes\n"+
                    "    localKeyID: 31 34 35 36 39 32 38 39 39 31 35 35 30 \n"+
                    "    friendlyName: default\n"+
                    "subject=/C=CA/ST=Test/L=Test/O=Test/OU=Test/CN=Test\n"+
                    "issuer=/C=CA/ST=Test/L=Test/O=Test/OU=Test/CN=Test\n"+
                    "-----BEGIN CERTIFICATE-----\n"+
                    "MIIDUTCCAjmgAwIBAgIEd+noizANBgkqhkiG9w0BAQUFADBYMQswCQYDVQQGEwJD\n"+
                    "QTENMAsGA1UECBMEVGVzdDENMAsGA1UEBxMEVGVzdDENMAsGA1UEChMEVGVzdDEN\n"+
                    "MAsGA1UECxMEVGVzdDENMAsGA1UEAxMEVGVzdDAgFw0xNjAzMDIxNDI5MjRaGA8\nY"+
                    "MTE2MDIwNzE0MjkyNFowWDELMAkGA1UEBhMCQ0ExDTALBgNVBAgTBFRlc3QxDTAL\n"+
                    "BgNVBAcTBFRlc3QxDTALBgNVBAoTBFRlc3QxDTALBgNVBAsTBFRlc3QxDTALBgNV\n"+
                    "BAMTBFRlc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC2IJ0THYPv\n"+
                    "m669AuyCHP8GUmQB0r9MeW1sFQIRWdkQRijNIKa60sqN58b7S11Mc8ch+CXQ3bMZ\n"+
                    "M4CRqX8GAc/0Gd/kkQhen7Q8JGnM+AzV51vqgaYV4A3A2i/ruToZpUD2VCl4nozh\n"+
                    "oqjwAzPivRBwzCi44dR2AnFAfEEH8dPvizP11nuS5fCBMQy8uhm2Y4JXg1uW0szD\n"+
                    "kbj5E6r3JKKs79d0iDfWNogmCXxs95fFyx3avU/n7X7uYku0QXpmuXtNjOe+92Sq\n"+
                    "7kq/0BWmJFfhks2GJi7ea0MIKd4AxiYuJCQahLx9L8DLdxlD5qV42YKPog1n3ET\nY"+
                    "CpIXq3MZ++5tAgMBAAGjITAfMB0GA1UdDgQWBBQbKbM4oe9VhqO6xs2pPi0DuDdT\n"+
                    "2DANBgkqhkiG9w0BAQUFAAOCAQEAtUnEyi1ZZJq0hn0KJc//G60Wj6RY9ZqRYLx6\n"+
                    "UHq7Od/B6z4Nc44eMVXETTWaU2Jw+zJLHycNzVJjKYamkrGAU3nrXljZiiXYrDfa\n"+
                    "i88m6+T6sSN97sbOzBUEOUKWwScE0VIpNxjmdp4CtQHC4GRZ7hwFjtwYt3ocFfE4\n"+
                    "3YZ6z75kNipVgwne9SwWff0Fj2L/5h2TKm9yxcOGKBayGY663fUeDPmNLscMoqkf\n"+
                    "pzYnTJ+amsLvGoeTl4SzDpMx7xbJn/JzgrG2udCA8+nRdQtflcmW2HP5yxNKuLsp\n"+
                    "ejtMxJUwPDsxA41ug2efaCC9dSlZIa3VSibZjjmUD5EcHCO4NA==\n"+
                    "-----END CERTIFICATE-----\n";

    /**
     * Since the cert data above is final, we're safe to cache the Cert once we build it
     */
    private static Certificate cert = null;
    /**
     * Since the key data above is final, we're safe to cache the Key once we build it
     */
    private static Key key = null;

    /**
     * Utility method to obtain the test Certficate
     * @return the test Certificate
     * @throws Exception if something fails (it shouldn't)
     */
    public static synchronized Certificate getCertificate() throws CertificateException, IOException {
        if(cert==null){
            Section certSection = PemReader.readFirstSectionAndClose(new StringReader(certString), "CERTIFICATE");
            byte[] certBytes = certSection.getBase64DecodedBytes();
            cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
        }
        return cert;
    }

    /**
     * Utility method to obtain the test private Key.
     * @return the test Key
     * @throws Exception if something fails (it shouldn't)
     */
    public static synchronized Key getKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(key==null){
            Section keySection = PemReader.readFirstSectionAndClose(new StringReader(certString), "PRIVATE KEY");
            byte[] keyBytes = keySection.getBase64DecodedBytes();
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            key = kf.generatePrivate(keySpec);
        }
        return key;
    }

    @Test
    public void testCertRead() throws CertificateException, IOException{
        Certificate c = getCertificate();
        assertNotNull("Unable to build certficate for testing",c);
    }

    @Test
    public void testKeyRead() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        Key k = getKey();
        assertNotNull("Unable to build key for testing",k);
    }

    @Test
    public void testJwt() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException, CertificateException {
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(SignatureAlgorithm.RS256, getKey()).compact();
        assertNotNull("could not build jwt using test certificate",newJwt);
        Jws<Claims> jwt = Jwts.parser().setSigningKey(getCertificate().getPublicKey()).parseClaimsJws(newJwt);
        assertNotNull("could not decode jwt using test certificate",jwt);
    }
}
