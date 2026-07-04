package com.emoniph.witchery.ai;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class LlmEndpointValidator {
    private LlmEndpointValidator() {
    }

    public record Validation(boolean allowed, String message) {
    }

    public static Validation validate(String endpoint, boolean allowLanEndpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return new Validation(false, "Endpoint is blank.");
        }
        URI uri;
        try {
            uri = new URI(endpoint.trim());
        } catch (URISyntaxException ex) {
            return new Validation(false, "Endpoint is not a valid URI.");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return new Validation(false, "Endpoint must use http or https.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return new Validation(false, "Endpoint must include a host.");
        }
        if (!allowLanEndpoint && !isLoopbackHost(host)) {
            return new Validation(false, "Endpoint must be localhost/loopback unless llm.allowLanEndpoint is true.");
        }
        return new Validation(true, "OK");
    }

    public static boolean isLoopbackHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.equals("::1")
                || normalized.equals("[::1]")
                || normalized.equals("0:0:0:0:0:0:0:1")
                || normalized.startsWith("127.");
    }

    public static URI appendPath(String endpoint, String suffix) {
        String normalizedSuffix = suffix.startsWith("/") ? suffix : "/" + suffix;
        URI uri = URI.create(endpoint.trim());
        String path = uri.getPath();
        String nextPath;
        if (path == null || path.isBlank() || path.equals("/")) {
            nextPath = normalizedSuffix;
        } else if (path.endsWith(normalizedSuffix)) {
            nextPath = path;
        } else {
            nextPath = trimTrailingSlash(path) + normalizedSuffix;
        }
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), nextPath, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Endpoint path could not be appended.", ex);
        }
    }

    private static String trimTrailingSlash(String path) {
        String trimmed = path;
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
