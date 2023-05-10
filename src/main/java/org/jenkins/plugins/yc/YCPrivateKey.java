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

public class YCPrivateKey {

    private final Secret privateKey;
    private String userName;

    public YCPrivateKey(String privateKey, String userName) {
        this.privateKey = Secret.fromString(privateKey.trim());
        this.userName = userName;
    }

    public String getPrivateKey() {
        return privateKey.getPlainText();
    }

    public String getUserName() {
        return userName;
    }

    @SuppressWarnings("unused") // used by config-entries.jelly
    @Restricted(NoExternalUse.class)
    public Secret getPrivateKeySecret() {
        return privateKey;
    }

    public String getPublicFingerprint() throws IOException {
        try {
            PEMEncodable decode = PEMEncodable.decode(privateKey.getPlainText());
            KeyPair keyPair = decode.toKeyPair();
            if (keyPair == null) {
                throw new UnrecoverableKeyException("private key is null");
            }
            return KeyFingerprinter.fingerPrint(keyPair);
        } catch (UnrecoverableKeyException | CryptoException e) {
            throw new IOException("This private key is password protected, which isn't supported yet");
        }
    }

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
