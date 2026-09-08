package com.laweact.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppleRootCertificates {

    private static final String CLASSPATH_DIR = "certs/apple/";
    private static final String[] FILES = {
            "AppleRootCA-G3.cer",
            "AppleRootCA-G2.cer",
            "AppleIncRootCertificate.cer"
    };

    private AppleRootCertificates() {
    }

    public static Set<InputStream> open() {
        ClassLoader classLoader = AppleRootCertificates.class.getClassLoader();
        Set<InputStream> streams = new LinkedHashSet<>();
        try {
            for (String file : FILES) {
                InputStream stream = classLoader.getResourceAsStream(CLASSPATH_DIR + file);
                if (stream == null) {
                    throw new IllegalStateException("Certificado raiz da Apple ausente no classpath: " + file);
                }
                streams.add(stream);
            }
            return streams;
        } catch (RuntimeException e) {
            closeQuietly(streams);
            throw e;
        }
    }

    public static void closeQuietly(Set<InputStream> streams) {
        if (streams == null) {
            return;
        }
        for (InputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // best-effort
            }
        }
    }
}
