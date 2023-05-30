package org.jenkins.plugins.yc;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class ServiceAccount extends BaseStandardCredentials {

    private String createdAt;
    private String keyAlgorithm;
    private String serviceAccountId;
    private String privateKey;
    private String publicKey;

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
}
