package com.se361.financial_service.gateway.payway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
public final class PaywayWebhookSignatureVerifier {

    private PaywayWebhookSignatureVerifier() {}

    public static boolean verify(String verifyKey, byte[] rawBody, String providedHexSignature) {
        if (verifyKey == null || rawBody == null || providedHexSignature == null) return false;
        String expected = hmacSha256Hex(verifyKey, rawBody);
        return constantTimeEquals(expected, providedHexSignature);
    }

    public static String hmacSha256Hex(String verifyKey, byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(verifyKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody);
            return toLowerHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", e);
        }
    }

    private static String toLowerHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        int i = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            out[i++] = Character.toLowerCase(Character.forDigit((v >>> 4) & 0xF, 16));
            out[i++] = Character.toLowerCase(Character.forDigit(v & 0xF, 16));
        }
        return new String(out);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        int result = expected.length() ^ provided.length();
        int max = Math.max(expected.length(), provided.length());
        for (int i = 0; i < max; i++) {
            char ec = i < expected.length() ? expected.charAt(i) : 0;
            char pc = i < provided.length() ? Character.toLowerCase(provided.charAt(i)) : 0;
            result |= ec ^ pc;
        }
        return result == 0;
    }
}
