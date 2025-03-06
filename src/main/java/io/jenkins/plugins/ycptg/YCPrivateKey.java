package io.jenkins.plugins.ycptg;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import hudson.util.Secret;
import jenkins.bouncycastle.api.PEMEncodable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.logging.Logger;

public class YCPrivateKey {
    private static final Logger LOGGER = Logger.getLogger(YCPrivateKey.class.getName());
    
    private final Secret privateKey;
    private final String userName;

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

    public String getPublic() throws IOException {
        try {
            String privateKeyContent = privateKey.getPlainText();
            
            // Log the private key format for debugging
            LOGGER.fine("Private key format detection...");
            
            if (privateKeyContent.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")) {
                LOGGER.fine("Detected OpenSSH format");
                return convertFromOpenSSHFormat(privateKeyContent);
            } else if (privateKeyContent.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
                LOGGER.fine("Detected PEM format");
                return convertFromPEMFormat(privateKeyContent);
            } else {
                throw new IOException("Unsupported private key format");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to process private key: " + e.getMessage());
            throw new IOException("Failed to process private key: " + e.getMessage(), e);
        }
    }

    private String convertFromOpenSSHFormat(String privateKeyContent) throws IOException {
        try {
            JSch jsch = new JSch();
            // Правильный метод load для JSch
            KeyPair keyPair = KeyPair.load(jsch, privateKeyContent);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyPair.writePublicKey(baos, userName);
            String publicKey = baos.toString(StandardCharsets.UTF_8.name());
            LOGGER.fine("Generated OpenSSH public key: " + publicKey);
            return publicKey;
        } catch (JSchException e) {
            throw new IOException("Failed to convert OpenSSH private key", e);
        }
    }

    private String convertFromPEMFormat(String privateKeyContent) throws IOException {
        try {
            PEMEncodable decode = PEMEncodable.decode(privateKeyContent);
            java.security.KeyPair keyPair = decode.toKeyPair();
            if (keyPair == null) {
                throw new UnrecoverableKeyException("private key is null");
            }
            PublicKey publicKey = keyPair.getPublic();
            String convertedKey = convertToOpenSSHFormat(publicKey);
            LOGGER.fine("Generated PEM public key: " + convertedKey);
            return convertedKey;
        } catch (UnrecoverableKeyException e) {
            throw new IOException("This private key is password protected, which isn't supported yet");
        }
    }

    private String convertToOpenSSHFormat(PublicKey publicKey) throws IOException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec keySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
    
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Write key type
            byte[] sshRsa = "ssh-rsa".getBytes(StandardCharsets.UTF_8);
            writeLength(out, sshRsa.length);
            out.write(sshRsa);
            
            // Write public exponent
            byte[] exponent = keySpec.getPublicExponent().toByteArray();
            writeLength(out, exponent.length);
            out.write(exponent);
            
            // Write modulus
            byte[] modulus = keySpec.getModulus().toByteArray();
            writeLength(out, modulus.length);
            out.write(modulus);
    
            String encodedKey = "ssh-rsa " + Base64.getEncoder().encodeToString(out.toByteArray());
            LOGGER.fine("Converted to OpenSSH format: " + encodedKey);
            return encodedKey;
        } catch (Exception e) {
            throw new IOException("Failed to convert public key to OpenSSH format", e);
        }
    }
    
    private void writeLength(ByteArrayOutputStream out, int length) throws IOException {
        out.write((length >>> 24) & 0xFF);
        out.write((length >>> 16) & 0xFF);
        out.write((length >>> 8) & 0xFF);
        out.write(length & 0xFF);
    }

    @Override
    public int hashCode() {
        return privateKey.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that != null && this.getClass() == that.getClass()) {
            return this.privateKey.equals(((YCPrivateKey) that).privateKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return privateKey.getPlainText();
    }
}