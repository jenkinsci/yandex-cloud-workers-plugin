/*
 * The MIT License
 *
 * Copyright (c) 2004-, Kohsuke Kawaguchi, Sun Microsystems, Inc., and a number of other of contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.plugins.yc;

import hudson.util.Secret;
import jenkins.bouncycastle.api.PEMEncodable;
import org.bouncycastle.crypto.CryptoException;
import org.jenkins.plugins.yc.util.KeyFingerprinter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.security.KeyPair;
import java.security.UnrecoverableKeyException;

/**
 * RSA private key (the one that you generate with yc-add-keypair.)
 *
 * Starts with "----- BEGIN RSA PRIVATE KEY------\n".
 *
 * @author Kohsuke Kawaguchi
 */
public class YCPrivateKey {

    private final Secret privateKey;

    YCPrivateKey(String privateKey) {
        this.privateKey = Secret.fromString(privateKey.trim());
    }

    public String getPrivateKey() {
        return privateKey.getPlainText();
    }

    @SuppressWarnings("unused") // used by config-entries.jelly
    @Restricted(NoExternalUse.class)
    public Secret getPrivateKeySecret() {
        return privateKey;
    }

    /**
     * Obtains the fingerprint of the key in the "ab:cd:ef:...:12" format.
     *
     * @throws IOException if the underlying private key is invalid: empty or password protected
     *    (password protected private keys are not yet supported)
     */
    public String getFingerprint() throws IOException {
        String pemData = privateKey.getPlainText();
        if (pemData.isEmpty()) {
            throw new IOException("This private key cannot be empty");
        }
        try {
            return PEMEncodable.decode(pemData).getPrivateKeyFingerprint();
        } catch (UnrecoverableKeyException e) {
            throw new IOException("This private key is password protected, which isn't supported yet");
        }
    }

    public String getPublicFingerprint() throws IOException {
        try {
            PEMEncodable decode = PEMEncodable.decode(privateKey.getPlainText());
            KeyPair keyPair = decode.toKeyPair();
            if (keyPair == null) {
                throw new UnrecoverableKeyException("private key is null");
            }
            return KeyFingerprinter.rsaFingerprint(keyPair);
        } catch (UnrecoverableKeyException | CryptoException e) {
            throw new IOException("This private key is password protected, which isn't supported yet");
        }
    }

    /*public String decryptWindowsPassword(String encodedPassword) throws AmazonClientException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, PEMEncodable.decode(privateKey.getPlainText()).toPrivateKey());
            byte[] cipherText = Base64.getDecoder().decode(StringUtils.deleteWhitespace(encodedPassword));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, Charset.forName("ASCII"));
        } catch (Exception e) {
            throw new AmazonClientException("Unable to decode password:\n" + e.toString());
        }
    }*/

    @Override
    public int hashCode() {
        return privateKey.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that != null && this.getClass() == that.getClass())
            return this.privateKey.equals(((YCPrivateKey) that).privateKey);
        return false;
    }

    @Override
    public String toString() {
        return privateKey.getPlainText();
    }
}
