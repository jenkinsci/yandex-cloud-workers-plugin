package org.jenkins.plugins.yc;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.security.ACL;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YandexCloud extends AbstractCloud {

    private static final Logger LOGGER = Logger.getLogger(YandexCloud.class.getName());

    @DataBoundConstructor
    public YandexCloud(String name,
                       List<? extends YandexTemplate> templates, String credentialsId,
                       String folderId, String sshKeysCredentialsId,
                       String initVMTemplate) {
        super(name, templates, credentialsId, folderId, sshKeysCredentialsId, initVMTemplate);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(CloudState state, int excessWorkload) {
        Label label = state.getLabel();
        final Collection<YandexTemplate> matchingTemplates = getTemplates(label);
        List<NodeProvisioner.PlannedNode> plannedNodes = new ArrayList<>();

        Jenkins jenkinsInstance = Jenkins.get();
        if (jenkinsInstance.isQuietingDown()) {
            LOGGER.log(Level.INFO, "Not provisioning nodes, Jenkins instance is quieting down");
            return Collections.emptyList();
        } else if (jenkinsInstance.isTerminating()) {
            LOGGER.log(Level.INFO, "Not provisioning nodes, Jenkins instance is terminating");
            return Collections.emptyList();
        }
        for (YandexTemplate t : matchingTemplates) {
            try {
                LOGGER.log(Level.INFO, "{0}. Attempting to provision slave needed by excess workload of " + excessWorkload + " units", t);
                int number = Math.max(excessWorkload / t.getNumExecutors(), 1);
                final List<YCAbstractSlave> slaves = getNewOrExistingAvailableSlave(t, number,false);

                if (slaves == null || slaves.isEmpty()) {
                    LOGGER.log(Level.WARNING, "Can't raise nodes for " + t);
                    continue;
                }
                for (final YCAbstractSlave slave : slaves) {
                    if (slave == null) {
                        LOGGER.log(Level.WARNING, "Can't raise node for " + t);
                        continue;
                    }

                    plannedNodes.add(createPlannedNode(t, slave));
                    excessWorkload -= t.getNumExecutors();
                }
                LOGGER.log(Level.INFO, "{0}. Attempting provision finished, excess workload: " + excessWorkload, t);
                if (excessWorkload == 0) break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, t + ". Exception during provisioning", e);
            }
        }
        LOGGER.log(Level.INFO, "We have now {0} computers, waiting for {1} more",
                new Object[]{jenkinsInstance.getComputers().length, plannedNodes.size()});
        return plannedNodes;
    }

    @Extension
    public static final class YandexDescriptor extends AbstractCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Yandex Cloud";
        }

        @RequirePOST
        public ListBoxModel doFillSshKeysCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String sshKeysCredentialsId) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            StandardListBoxModel result = new StandardListBoxModel();

            return result
                    .includeMatchingAs(Jenkins.getAuthentication(), context, SSHUserPrivateKey.class, Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always())
                    .includeMatchingAs(ACL.SYSTEM, context, SSHUserPrivateKey.class, Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always())
                    .includeCurrentValue(sshKeysCredentialsId);
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems() {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(FileCredentialsImpl.class,
                                    Jenkins.get(),
                                    ACL.SYSTEM,
                                    Collections.emptyList()));
        }


        @RequirePOST
        public FormValidation doTestConnection(@AncestorInPath ItemGroup context, @QueryParameter String credentialsId) {
            return super.doTestConnection(context, credentialsId);
        }

        @RequirePOST
        public FormValidation doCheckCredentialsId(@AncestorInPath ItemGroup context, @QueryParameter String value) throws IOException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()){
                return FormValidation.error("No credentials selected");
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public FormValidation doCheckSshKeysCredentialsId(@AncestorInPath ItemGroup context, @QueryParameter String value) throws IOException {
            return super.doCheckSshKeysCredentialsId(context, value);
        }

        @RequirePOST
        public FormValidation doCheckInitVMTemplate(@AncestorInPath ItemGroup context, @QueryParameter String value) throws IOException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()){
                return FormValidation.error("Init VM script empty");
            }
            return FormValidation.ok();
        }
    }

}
