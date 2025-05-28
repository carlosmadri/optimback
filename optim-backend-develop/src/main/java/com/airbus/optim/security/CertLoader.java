package com.airbus.optim.security;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

public class CertLoader {

    private static final String CERT_FILE = "certs/certificado-optim.crt";
    private static final String ERROR_CERT_NOT_FOUND = "Certificado no encontrado en resources/certs/";
    private static final String ERROR_NOT_RSA_KEY = "El certificado no contiene una clave pública RSA.";

    public static RSAPublicKey getPublicKeyFromCert() throws Exception {
        try (var certStream = CertLoader.class.getClassLoader().getResourceAsStream(CERT_FILE)) {
            if (Objects.isNull(certStream)) {
                throw new IllegalArgumentException(ERROR_CERT_NOT_FOUND);
            }

            var certFactory = CertificateFactory.getInstance("X.509");
            var cert = (X509Certificate) certFactory.generateCertificate(certStream);

            if (!(cert.getPublicKey() instanceof RSAPublicKey)) {
                throw new IllegalArgumentException(ERROR_NOT_RSA_KEY);
            }

            return (RSAPublicKey) cert.getPublicKey();
        }
    }
}