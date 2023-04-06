package org.jenkins.plugins.yc;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.security.ACL;
import hudson.slaves.NodeProvisioner;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

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
                final List<YCAbstractSlave> slaves = getNewOrExistingAvailableSlave(t, 1,false);

                if (slaves == null || slaves.isEmpty()) {
                    LOGGER.warning("Can't raise nodes for " + t);
                    continue;
                }
                for (final YCAbstractSlave slave : slaves) {
                    if (slave == null) {
                        LOGGER.warning("Can't raise node for " + t);
                        continue;
                    }

                    plannedNodes.add(createPlannedNode(t, slave));
                    excessWorkload -= 1;
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

    public void provision(YandexTemplate t, int number) {
        Jenkins jenkinsInstance = Jenkins.get();
        if (jenkinsInstance.isQuietingDown()) {
            LOGGER.log(Level.INFO, "Not provisioning nodes, Jenkins instance is quieting down");
            return;
        } else if (jenkinsInstance.isTerminating()) {
            LOGGER.log(Level.INFO, "Not provisioning nodes, Jenkins instance is terminating");
            return;
        }
        try {
            LOGGER.log(Level.INFO, "{0}. Attempting to provision {1} slave(s)", new Object[]{t, number});
            final List<YCAbstractSlave> slaves = getNewOrExistingAvailableSlave(t, number, false);

            if (slaves == null || slaves.isEmpty()) {
                LOGGER.warning("Can't raise nodes for " + t);
                return;
            }

            attachSlavesToJenkins(jenkinsInstance, slaves, t);

            LOGGER.log(Level.INFO, "{0}. Attempting provision finished", t);
            LOGGER.log(Level.INFO, "We have now {0} computers, waiting for {1} more",
                    new Object[]{Jenkins.get().getComputers().length, number});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, t + ". Exception during provisioning", e);
        }

    }

    private static void attachSlavesToJenkins(Jenkins jenkins, List<YCAbstractSlave> slaves, YandexTemplate t) throws IOException {
        for (final YCAbstractSlave slave : slaves) {
            if (slave == null) {
                LOGGER.warning("Can't raise node for " + t);
                continue;
            }

            Computer c = slave.toComputer();
            if (slave.getStopOnTerminate() && c != null) {
                c.connect(false);
            }
            jenkins.addNode(slave);
        }
    }

    @Extension
    public static final class YandexDescriptor extends AbstractCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Yandex Cloud";
        }

        @POST
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

    }

}
