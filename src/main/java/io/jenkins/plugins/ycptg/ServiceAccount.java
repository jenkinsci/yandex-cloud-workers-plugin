package io.jenkins.plugins.ycptg;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import lombok.ToString;
import yandex.cloud.sdk.auth.Auth;
import yandex.cloud.sdk.auth.jwt.ServiceAccountKey;
import yandex.cloud.sdk.auth.provider.CredentialProvider;

@ToString(of = {"serviceAccountId", "createdAt", "alg", "pub"})
public class ServiceAccount extends BaseStandardCredentials {

    private final String createdAt;
    private final String alg;
    private final String serviceAccountId;
    private final String pk;
    private final String pub;

    public ServiceAccount(@CheckForNull CredentialsScope scope,
                          @CheckForNull String id, @CheckForNull String description,
                          String createdAt, String alg, String serviceAccountId,
                          String pk, String pub) {
        super(scope, id, description);
        this.createdAt = createdAt;
        this.alg = alg;
        this.serviceAccountId = serviceAccountId;
        this.pk = pk;
        this.pub = pub;
    }

    public CredentialProvider buildCredentialProvider() {
        return Auth.apiKeyBuilder()
                .serviceAccountKey(new ServiceAccountKey(this.getId(),
                        serviceAccountId,
                        createdAt,
                        alg,
                        pub,
                        pk))
                .build();
    }
}
