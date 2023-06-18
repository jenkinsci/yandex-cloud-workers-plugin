package io.jenkins.plugins.yc.util;


import org.bouncycastle.crypto.CryptoException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Objects;

/**
 * Utiility class for calculating and verifying SSH key fingerprints as defined by OpenSSH.
 */
public final class KeyFingerprinter {

    /**
     * OpenSSH does not pad.
     */
    public final static Base64.Encoder b64Encoder = Base64.getEncoder().withoutPadding();

    @SuppressWarnings("checkstyle:javadocmethod")
    private KeyFingerprinter() {
    }

    public static String fingerPrint(final KeyPair keyPair) throws CryptoException {
        Objects.requireNonNull(keyPair);
        return SshEncoder.encode(keyPair.getPublic());
    }

    /*
     * OpenSSH generally uses its own serialized representation of
     * public keys.  This is different from the ASN.1 or X.509 format
     * that may be returned by Java's getEncoded() method.  Each key
     * type necessarily has its own format as well.  In general the
     * representation includes a name followed by a series of
     * arbitrarily large integers serialized as length+bytes.  This is
     * based on the work in the node-sshpk project.  The specific
     * ordering of the encoded pieces (ie the exponent and modulus for
     * RSA) is also based on node-sshpk.
     */
    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:javadocmethod", "checkstyle:javadoctype"})
    private static class SshEncoder {

        public static String encode(final PublicKey key) throws CryptoException {
            if (key instanceof RSAPublicKey) {
                return encode((RSAPublicKey)key);
            } else if (key instanceof DSAPublicKey) {
                return encode((DSAPublicKey)key);
            } else if (key instanceof ECPublicKey) {
                return encode((ECPublicKey)key);
            }
            else {
                throw new CryptoException("unknown public key type: " + key.getClass().getName());
            }
        }

        // sshpk parts: ['e', 'n']
        public static String encode(final RSAPublicKey key) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] name = "ssh-rsa".getBytes(StandardCharsets.US_ASCII);
            writeArray(name, buf);
            writeArray(key.getPublicExponent().toByteArray(), buf);
            writeArray(key.getModulus().toByteArray(), buf);
            return "ssh-rsa "+b64Encoder.encodeToString(buf.toByteArray());
        }

        // sshpk: ['p', 'q', 'g', 'y']
        public static String encode(final DSAPublicKey key) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] name = "ssh-dss".getBytes(StandardCharsets.US_ASCII);
            writeArray(name, buf);
            writeArray(key.getParams().getP().toByteArray(), buf);
            writeArray(key.getParams().getQ().toByteArray(), buf);
            writeArray(key.getParams().getG().toByteArray(), buf);
            writeArray(key.getY().toByteArray(), buf);
            return "ssh-dss "+b64Encoder.encodeToString(buf.toByteArray());
        }

        /*
         * sshpk parts: ['curve', 'Q']
         *
         * Unfortunately the ECDSA serialization is a bit quirky.
         * Both the "name" and "curve name" strings are are used, and
         * they include the "key size" (ex: nistp256).  A larger
         * complication is that "Q" (the elliptic curve point) is not
         * a simple big int but a compound representation of the
         * coordinates.  The is described in details in RFC 5656 and
         * the "SEC 1: Elliptic Curve Cryptography"
         * <http://www.secg.org/sec1-v2.pdf> paper on which the RFD
         * depends.  Fortunately, the point representation is the same
         * as the ASN.1 representation used by Java, so we can let the
         * standard library do all of the bit twiddling and grab the
         * appropriate bytes at the end.
         *
         * These details are summarized by
         * https://security.stackexchange.com/a/129913
         */
        public static String encode(final ECPublicKey key) throws CryptoException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            int bitLength = key.getW().getAffineX().bitLength();
            String curveName = null;
            int qLen;
            if (bitLength <= 256) {
                curveName = "nistp256";
                qLen = 65;
            } else if (bitLength <= 384) {
                curveName = "nistp384";
                qLen = 97;
            } else if (bitLength <= 521) {
                curveName = "nistp521";
                qLen = 133;
            } else {
                throw new CryptoException("ECDSA bit length unsupported: " + bitLength);
            }

            byte[] name = ("ecdsa-sha2-" + curveName).getBytes(StandardCharsets.US_ASCII);
            byte[] curve = curveName.getBytes(StandardCharsets.US_ASCII);
            writeArray(name, buf);
            writeArray(curve, buf);

            byte[] javaEncoding = key.getEncoded();
            byte[] q = new byte[qLen];

            System.arraycopy(javaEncoding, javaEncoding.length - qLen, q, 0, qLen);
            writeArray(q, buf);

            return "ecdsa-sha2-" + curveName + " " +b64Encoder.encodeToString(buf.toByteArray());
        }

        /*
         * The OpenSSH serialization format can in principle express a
         * variety of types.  Fortunately only byte arrays
         * (representing either strings or big integers) are required
         * to represent public keys.  They are serialized as the
         * length (requiring the unsigned int conversion) followed by
         * the bytes.
         */
        public static void writeArray(final byte[] arr, final ByteArrayOutputStream baos) {
            for (int shift = 24; shift >= 0; shift -= 8) {
                baos.write((arr.length >>> shift) & 0xFF);
            }
            baos.write(arr, 0, arr.length);
        }
    }
}
