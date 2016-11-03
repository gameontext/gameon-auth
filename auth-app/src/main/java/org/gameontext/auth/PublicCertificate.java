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
package org.gameontext.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

/**
 * Servlet implementation class PublicCertificate
 */
@WebServlet("/PublicCertificate")
public class PublicCertificate extends HttpServlet {
    private static final long serialVersionUID = 1L;

    java.security.cert.Certificate publicCert = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public PublicCertificate() throws IOException {
        super();

        String keyStore = null;
        String keyStorePW = null;
        String keyStoreAlias = null;
        try {
            keyStore = new InitialContext().lookup("jwtKeyStore").toString();
            keyStorePW = new InitialContext().lookup("jwtKeyStorePassword").toString();
            keyStoreAlias = new InitialContext().lookup("jwtKeyStoreAlias").toString();

            // load up the keystore..
            FileInputStream is = new FileInputStream(keyStore);
            KeyStore signingKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            signingKeystore.load(is, keyStorePW.toCharArray());

            // grab the key we'll publish for others to verify our trust.
            publicCert = signingKeystore.getCertificate(keyStoreAlias);

        } catch (NamingException e) {
            throw new IOException(e);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            response.getWriter().append("-----BEGIN CERTIFICATE-----\n")
                    .append(DatatypeConverter.printBase64Binary(publicCert.getEncoded()).replaceAll("(.{64})", "$1\n"))
                    .append("\n-----END CERTIFICATE-----\n");
        } catch (CertificateEncodingException e) {
            throw new IOException(e);
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

}
