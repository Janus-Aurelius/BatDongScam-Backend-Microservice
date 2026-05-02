package com.se100.bds.services.payment.payway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies Payway webhook signature.
 * <p>
 * Contract:
 * <ul>
 *     <li>HMAC-SHA256 with verifyKey as key</li>
 *     <li>Hex-encoded lowercase output</li>
 *     <li>Input is raw request body bytes</li>
 *     <li>No timestamp / nonce</li>
 * </ul>
 * <p>
 */
public final class PaywayWebhookSignatureVerifier {

    private PaywayWebhookSignatureVerifier() {
    }

    public static boolean verify(String verifyKey, byte[] rawBody, String providedHexSignature) {
        if (verifyKey == null || rawBody == null || providedHexSignature == null) {
            return false;
        }
        String expected = hmacSha256Hex(verifyKey, rawBody);
        return constantTimeEqualsAscii(expected, providedHexSignature);
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

    /**
     * Constant-time compare for ASCII hex strings.
     * Accepts either uppercase or lowercase provided signature.
     */
    private static boolean constantTimeEqualsAscii(String expectedLowerHex, String providedHex) {
        int expectedLen = expectedLowerHex.length();
        int providedLen = providedHex.length();
        int max = Math.max(expectedLen, providedLen);

        int result = expectedLen ^ providedLen;
        for (int i = 0; i < max; i++) {
            char ec = i < expectedLen ? expectedLowerHex.charAt(i) : 0;
            char pc = i < providedLen ? Character.toLowerCase(providedHex.charAt(i)) : 0;
            result |= ec ^ pc;
        }
        return result == 0;
    }
}

