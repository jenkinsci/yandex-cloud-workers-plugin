package io.jenkins.plugins.yc;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import lombok.ToString;
import yandex.cloud.sdk.auth.Auth;
import yandex.cloud.sdk.auth.jwt.ServiceAccountKey;
import yandex.cloud.sdk.auth.provider.CredentialProvider;


@ToString
public class ServiceAccount extends BaseStandardCredentials {

    private final String createdAt;
    private final String keyAlgorithm;
    private final String serviceAccountId;
    private final String privateKey;
    private final String publicKey;

    public ServiceAccount(@CheckForNull CredentialsScope scope,
                          @CheckForNull String id, @CheckForNull String description,
                          String createdAt, String keyAlgorithm, String serviceAccountId,
                          String privateKey, String publicKey) {
        super(scope, id, description);
        this.createdAt = createdAt;
        this.keyAlgorithm = keyAlgorithm;
        this.serviceAccountId = serviceAccountId;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public CredentialProvider buildCredentialProvider() {
        return Auth.apiKeyBuilder()
                .serviceAccountKey(new ServiceAccountKey(this.getId(),
                        serviceAccountId,
                        createdAt,
                        keyAlgorithm,
                        publicKey,
                        privateKey))
                .build();
    }
}
