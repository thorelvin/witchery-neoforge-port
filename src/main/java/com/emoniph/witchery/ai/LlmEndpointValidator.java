package com.emoniph.witchery.ai;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class LlmEndpointValidator {
    private LlmEndpointValidator() {
    }

    public record Validation(boolean allowed, String message) {
    }

    public enum Policy {
        LOCAL_ONLY,
        PRIVATE_LAN,
        REMOTE_ALLOWED;

        public static Policy parse(String value) {
            if (value == null || value.isBlank()) {
                return LOCAL_ONLY;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                return LOCAL_ONLY;
            }
        }
    }

    /** Legacy migration shim: the old LAN toggle never implied arbitrary remote access. */
    public static Validation validate(String endpoint, boolean allowLanEndpoint) {
        return validate(endpoint, allowLanEndpoint ? Policy.PRIVATE_LAN : Policy.LOCAL_ONLY);
    }

    public static Validation validate(String endpoint, Policy policy) {
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
        if (uri.getUserInfo() != null) {
            return new Validation(false, "Endpoint user information is not supported.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return new Validation(false, "Endpoint must include a host.");
        }
        int port = uri.getPort();
        if (port == 0 || port > 65535) {
            return new Validation(false, "Endpoint port must be between 1 and 65535.");
        }

        Policy effectivePolicy = policy == null ? Policy.LOCAL_ONLY : policy;
        if (effectivePolicy == Policy.LOCAL_ONLY && !isLoopbackHost(host)) {
            return new Validation(false, "Endpoint policy LOCAL_ONLY permits only localhost or literal loopback addresses.");
        }
        if (effectivePolicy == Policy.PRIVATE_LAN && !isPrivateLanHost(host)) {
            return new Validation(false, "Endpoint policy PRIVATE_LAN permits only literal private or loopback addresses.");
        }
        return new Validation(true, "OK");
    }

    public static boolean isLoopbackHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.equals("localhost")) {
            return true;
        }
        InetAddress literal = parseLiteralAddress(normalized);
        return literal != null && literal.isLoopbackAddress();
    }

    public static boolean isPrivateLanHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.equals("localhost")) {
            return true;
        }
        InetAddress literal = parseLiteralAddress(normalized);
        if (literal == null) {
            return false;
        }
        byte[] address = literal.getAddress();
        if (literal.isLoopbackAddress() || literal.isSiteLocalAddress() || literal.isLinkLocalAddress()) {
            return true;
        }
        return address.length == 16 && (address[0] & 0xFE) == 0xFC;
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

    private static String normalizeHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static InetAddress parseLiteralAddress(String host) {
        if (isIpv4Literal(host)) {
            try {
                return InetAddress.getByAddress(parseIpv4Bytes(host));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (!host.contains(":") || !host.matches("[0-9a-f:.%]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(host);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isIpv4Literal(String host) {
        if (!host.matches("[0-9.]+")) {
            return false;
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            try {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private static byte[] parseIpv4Bytes(String host) {
        String[] parts = host.split("\\.");
        byte[] bytes = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i]);
        }
        return bytes;
    }
}
