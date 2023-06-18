package io.jenkins.plugins.yc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.FileParameterValue;
import hudson.model.Node;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YandexCloudTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Mock
    private YandexTemplate mockedYandexTemplate;

    @Mock
    private YandexCloud mockedCloud;

    private final String credId = UUID.randomUUID().toString();

    private String template = "platform_id: 'standard-v3'\n" +
            "resources_spec: {\n" +
            "    memory: 1073741824\n" +
            "    cores: 2\n" +
            "    core_fraction: 20\n" +
            "}\n" +
            "boot_disk_spec: {\n" +
            "    mode: READ_WRITE\n" +
            "    disk_spec: {\n" +
            "        type_id: 'network-hdd'\n" +
            "        size: 16106127360\n" +
            "        image_id: 'fd87ap2ld09bjiotu5v0'\n" +
            "    }\n" +
            "    auto_delete: true\n" +
            "}\n" +
            "network_interface_specs: {\n" +
            "    subnet_id: 'e2l8m8rsiq7mbsusb9ps'\n" +
            "    primary_v4_address_spec: {\n" +
            "      one_to_one_nat_spec: {\n" +
            "        ip_version: IPV4\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "  scheduling_policy: {\n" +
            "    preemptible: true\n" +
            "  }";

    private final String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIG5AIBAAKCAYEA2wK84Nplds8coj+2GSoyjv/AtLX7RN5Zc73wpEURwy6fvaVz\n" +
            "SQEaLSNc6ifHZjvyM7mkz7g8JRbLvYp0MHmyocp1akuxKLXo7HZJfsGJp4Em+9s7\n" +
            "bZFkzkbMe6lGZAbUWBpwJwgiPq570RV5RQA7zpCUls7vr3NMde+Lu0QrgPC6Axri\n" +
            "4fIRb68SabCLN+wO03sB7ctaLG+8TEzU8fVB48U8cSfeLVrpAD1ExWOZ6N1mTsyC\n" +
            "vZyt9RVV8fO6njdiWP2iI8EDJILJoSl6W1h+ONEI2efqC016PYV0Lyz846wm+tD5\n" +
            "DZciacEnF3HCuxVkKehB3pj8f59sAHmhY7ud6NUQvy5zVumWcNKV8dh2B4ph/zPJ\n" +
            "RHU0hFuRYQ14gpQtJhcTQkybuSNQotEpHh9ljBAyrN9br16bh0vcpsjP3XpDcJ25\n" +
            "9N4jXtcMeVV83ryBr1XupZZ5Q5tkeYm6MmbZfnK7X1XTIcLNeWCN0lniLgVPEwM+\n" +
            "O5TQ/As00iYkmXBnAgMBAAECggGAY1Wpj57fGHU1+o1FLcLK7bsoj45e6kyja/DB\n" +
            "nfBJ7ypNf7n0QS+DzOdWNEpYkZEs+LMCAskAVp4xSMXsjzQThniKquyr7NgdTmsU\n" +
            "SK7FnbjhkqhMGxUwELNh+dC8QZArbc6IAdwZlC6VsC66M0a42acQDhrL8dss2e0N\n" +
            "hqGTzcbhx5jBQVQG/o38nv1ZF8M0skz/gfiS7Ma9NCpBB4GDIikKkhRQHGg4eEhR\n" +
            "0emsHEeXKLRvtErFR/0mXzh581sQpIGVXw+Mmjcm28NWzd4nwpCCROWbh/YFU7yz\n" +
            "gwp4pbdxGSbRfP5s5F02ZXYjjVQRoXafrZe8Okd50v7lnuvL040yF4keWPcCSjXt\n" +
            "k19HpaPBHLkQShxU7ZDzlNb/+5QjyJM1Yca8Nhs4Rb2AI+IDNaceda8FG+aYSZcE\n" +
            "0SH1K+G2ibOonT7emg5u6PPq7Ahthzn3HlQFGbliJJxN8QxqsBlWs3XbkqKw42nH\n" +
            "Nu/anFfl8R1IcDACq1Tjp+HleHT5AoHBAOW4v5X5BfqMpAjgV9zHExtGZ9BNDHDC\n" +
            "8TVSfQHTh5U6V9VVWyssVTrzxG1iAf/qYim+5zWHkF7xu8GtiSPg1OOTa8sGTdBm\n" +
            "3usgKmT9KX0t4WxvcnNRoJEISY55tTJef9cACFphc4RwDWmg1Pl7yN04KwXzeSFG\n" +
            "W7gLiB3sZQCvBAkBJ3q9k72oLQ75kHqsw3Aod13xQsqMuHwazyQN3joSOs1y+MhP\n" +
            "ZxIBRbzqhB05SJhBpdNmsZunFNkaNDNs7QKBwQD0EFNGP5CQqkE7fssAH3bFLV48\n" +
            "uEpEvan1d5YGNO+tOwT6BqpDDDa8mkGhYmVrcoh8QyWOJHue+ue/VE0c+KegswLH\n" +
            "OYkpAQfu5xYHF4qtw1FZGzGgrB3eT/jgJ1yZKcn25nyoNDTeBcYJICfmBspOG6Bv\n" +
            "gYIFTMG3Mc5wq8o13PWjEbAWPxWTuMgMCfPrHcJU/P45H7XMjJfWWM5DHKeexW5m\n" +
            "UgdPVD6xEgG9t+dpnGw7xc+DS97gUCczVGzyPCMCgcEArTMyH9z1iQZo1thu5mKi\n" +
            "ITpgI0k0TABu5Ikg/zOBUh3/AzWr2009pYkNXHs8jrsk79yjblSboToZGdnBQG/P\n" +
            "kxYfe59xI+OrxxMlY6cMlPj2sU8Ft7SfnznkW6uf1i4xrOS1Z496DSx+sFk2ujN+\n" +
            "Kmyi44XCmjf2BzmKkyUcsftCy5c3MO+maKRX9KZtLFD9uZ6JASdZ+GVI4ylDbCeY\n" +
            "magM9cHtmPlk+MpEzj7zBSWfekxkvpL67iR3L4j2Op+xAoHBAKPZ6e45cnaMtqqY\n" +
            "nvSAKx6JLMEbAFFR+65eEIwuMdJc0ySAIQJOStknvnP4ORp/k/3InoflGlFLJRag\n" +
            "I/0VOoR6aZ0SlXC/znGnFnTAa0vo2s41oYW6x3qeufjStCnnkj1906ahFfUQbzll\n" +
            "KyQU3vC9P2Jc0ct5EnxIpc2ip2it5QMLNupYg8Xewf7DF8x6+CJSjuIO1eBzauRu\n" +
            "Tltj+2xuI4XyABJT/pql+iFCO8v9RfSDAqOk5gY2CXs+bL3bfQKBwHy/AOqxQiTr\n" +
            "2ATGPByIoBmd9hI0uEP6ruYYOIVGOwJPvrjouZuaWEm4vo9muwald+GADKEGWnhc\n" +
            "JwhmdjVjlbf1mmhUVttRyaHlehlwk/ws15PfA6/77mDmL/klDuqOfZOhaYpqHey8\n" +
            "Wa7hVHtTvb3sCErQF7RyIinYlclXoz5DHCcPYqUf/Nl1ctKPSNku906K7vAYDE1f\n" +
            "vFGyjltEJ8OI8dFY/p0XpkhvIPcX0j74Has1FLb0e9QiPCyaW+68vQ==\n" +
            "-----END RSA PRIVATE KEY-----\n";

    @Before
    public void setup(){
        j.jenkins.clouds.clear();
        List<YandexTemplate> yandexTemplateList = new ArrayList<>();
        yandexTemplateList.add(new YandexTemplate("testVm", "", "descr", Node.Mode.NORMAL, "testLabels",
                null, "/tmp/hadson", "/tmp",  null, false, null, 0));
        YandexCloud yandexCloud = new YandexCloud("testCloud", yandexTemplateList, credId, credId, 300000);
        mockedCloud = Mockito.spy(yandexCloud);
        mockedYandexTemplate = Mockito.spy(mockedCloud.getTemplates().get(0));
        when(mockedYandexTemplate.getParent()).thenReturn(mockedCloud);
        when(mockedCloud.resolvePrivateKey()).thenReturn(new YCPrivateKey(privateKey, "test"));
        when(mockedYandexTemplate.getInitVMTemplate()).thenReturn(template);
        j.jenkins.clouds.add(mockedCloud);
    }

    @Test
    public void createVmTest() throws IOException {
        assertNotNull(mockedYandexTemplate.createVm());
    }

    @Test
    public void testSshCredentials() throws IOException {
        YandexCloud actual = j.jenkins.clouds.get(YandexCloud.class);
        YandexCloud.YandexDescriptor descriptor = (YandexCloud.YandexDescriptor) actual.getDescriptor();
        assertNotNull(descriptor);
        ListBoxModel m = descriptor.doFillSshKeysCredentialsIdItems(Jenkins.get());
        assertThat(m.size(), is(1));
        BasicSSHUserPrivateKey sshKeyCredentials = new BasicSSHUserPrivateKey(CredentialsScope.SYSTEM, "ghi", "key",
                new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource("somekey"), "", "");
        for (CredentialsStore credentialsStore: CredentialsProvider.lookupStores(j.jenkins)) {
            if (credentialsStore instanceof  SystemCredentialsProvider.StoreImpl) {
                credentialsStore.addCredentials(Domain.global(), sshKeyCredentials);
            }
        }
        //Ensure added credential is displayed
        m = descriptor.doFillSshKeysCredentialsIdItems(Jenkins.get());
        assertThat(m.size(), is(2));
        //Ensure that the cloud can resolve the new key
        assertThat(actual.resolvePrivateKey(), notNullValue());
    }

    @Test
    public void testYCCredentials() throws IOException {
        YandexCloud actual = j.jenkins.clouds.get(YandexCloud.class);
        YandexCloud.YandexDescriptor descriptor = (YandexCloud.YandexDescriptor) actual.getDescriptor();
        assertNotNull(descriptor);
        ListBoxModel m = descriptor.doFillCredentialsIdItems(Jenkins.get());
        assertThat(m.size(), is(1));
        ClassLoader classLoader = getClass().getClassLoader();
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new FileCredentialsImpl(CredentialsScope.SYSTEM, "system_id_1","system_des", new FileParameterValue.FileItemImpl(new File(Objects.requireNonNull(classLoader.getResource("credentials.json")).getFile())), "testFileName", LocalDateTime.now().toString()));
        //Ensure added credential is displayed
        m = descriptor.doFillCredentialsIdItems(Jenkins.get());
        assertThat(m.size(), is(2));
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new FileCredentialsImpl(CredentialsScope.SYSTEM, "system_id_2","system_des", new FileParameterValue.FileItemImpl(new File(Objects.requireNonNull(classLoader.getResource("credentials.json")).getFile())), "testFileName", LocalDateTime.now().toString()));
        m = descriptor.doFillCredentialsIdItems(Jenkins.get());
        assertThat(m.size(), is(3));
    }

}
