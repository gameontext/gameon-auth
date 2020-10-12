package gameontext.auth.controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicCertificateController {
    @Value("${jwt.keystore.location}")
    protected String keyStore;
    @Value("${jwt.keystore.password}")
    protected String keyStorePW;
    @Value("${jwt.keystore.alias}")
    protected String keyStoreAlias;

    private java.security.cert.Certificate publicCert = null;

    private void initPublicCert() throws IOException{
        try {
            System.out.println("Loading "+String.valueOf(keyStoreAlias)+" from "+String.valueOf(keyStore)+" with pw len "+(keyStorePW==null?"null":keyStorePW.length()));
            // load up the keystore..
            FileInputStream is = new FileInputStream(keyStore);
            KeyStore signingKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            signingKeystore.load(is, keyStorePW.toCharArray());

            // grab the key we'll publish for others to verify our trust.
            publicCert = signingKeystore.getCertificate(keyStoreAlias);

        }catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }

    @GetMapping(value="/auth/PublicCertificate")
    public void pubCert(HttpServletResponse response) throws IOException{
        if(publicCert==null){
            initPublicCert();
        }

        try {
            response.getWriter().append("-----BEGIN CERTIFICATE-----\n")
                    .append(Base64.getEncoder().encodeToString(publicCert.getEncoded()).replaceAll("(.{64})", "$1\n"))
                    .append("\n-----END CERTIFICATE-----\n");
        } catch (CertificateEncodingException e) {
            throw new IOException(e);
        }
    }
}
