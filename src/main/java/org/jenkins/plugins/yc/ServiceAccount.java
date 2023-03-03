package org.jenkins.plugins.yc;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;


public class ServiceAccount extends BaseStandardCredentials {

    private String createdAt;
    private String keyAlgorithm;
    private String serviceAccountId;
    private String privateKey;
    private String publicKey;

    public ServiceAccount(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, String createdAt, String keyAlgorithm, String serviceAccountId, String privateKey, String publicKey) {
        super(scope, id, description);
        this.createdAt = createdAt;
        this.keyAlgorithm = keyAlgorithm;
        this.serviceAccountId = serviceAccountId;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "ServiceAccount{" +
                "createdAt='" + createdAt + '\'' +
                ", keyAlgorithm='" + keyAlgorithm + '\'' +
                ", serviceAccountId='" + serviceAccountId + '\'' +
                ", privateKey='" + privateKey + '\'' +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }
}
