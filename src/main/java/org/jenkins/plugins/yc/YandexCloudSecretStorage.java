package org.jenkins.plugins.yc;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.User;
import yandex.cloud.sdk.auth.provider.CredentialProvider;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YandexCloudSecretStorage {

    /**
     * Logger for debugging purposes.
     */
    private static final Logger LOGGER = Logger.getLogger(YandexCloudSecretStorage.class.getName());

    public YandexCloudSecretStorage() {
    }

    public static void put(@NonNull User user, @NonNull String credentialId) {
        LOGGER.log(Level.INFO, "Populating the cache for username: " + user.getId());
        try {
            user.addProperty(new YandexCloudAccessTokenProperty(credentialId));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Received an exception when trying to add the GitHub access token to the user: " + user.getId(), e);
        }
    }
}