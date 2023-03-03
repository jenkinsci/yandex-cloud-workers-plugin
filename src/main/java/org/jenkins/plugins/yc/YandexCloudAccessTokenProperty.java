package org.jenkins.plugins.yc;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.UserProperty;
import yandex.cloud.sdk.auth.provider.CredentialProvider;

public class YandexCloudAccessTokenProperty extends UserProperty {
    private final String credentialId;

    public YandexCloudAccessTokenProperty(String credentialId) {
        this.credentialId = credentialId;
    }

    public @NonNull String getCredentialId() {
        return credentialId;
    }

}
