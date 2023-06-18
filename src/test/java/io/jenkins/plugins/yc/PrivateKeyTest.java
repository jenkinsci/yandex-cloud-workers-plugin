package io.jenkins.plugins.yc;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.Security;

import static org.junit.Assert.assertEquals;

public class PrivateKeyTest {

    @Before
    public void before() {
        // Add provider manually to avoid requiring jenkinsrule
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private YCPrivateKey getPrivateKey() {
        return new YCPrivateKey("-----BEGIN RSA PRIVATE KEY-----\n" +
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
                "-----END RSA PRIVATE KEY-----\n", "test");
    }

    @Test
    public void testPublicFingerprint() throws IOException {
        YCPrivateKey k = getPrivateKey();
        assertEquals("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDbArzg2mV2zxyiP7YZKjKO/8C0tftE3llzvfCkRRHDLp+9pXNJARotI1zqJ8dmO/IzuaTPuDwlFsu9inQwebKhynVqS7Eotejsdkl+wYmngSb72zttkWTORsx7qUZkBtRYGnAnCCI+rnvRFXlFADvOkJSWzu+vc0x174u7RCuA8LoDGuLh8hFvrxJpsIs37A7TewHty1osb7xMTNTx9UHjxTxxJ94tWukAPUTFY5no3WZOzIK9nK31FVXx87qeN2JY/aIjwQMkgsmhKXpbWH440QjZ5+oLTXo9hXQvLPzjrCb60PkNlyJpwScXccK7FWQp6EHemPx/n2wAeaFju53o1RC/LnNW6ZZw0pXx2HYHimH/M8lEdTSEW5FhDXiClC0mFxNCTJu5I1Ci0SkeH2WMEDKs31uvXpuHS9ymyM/dekNwnbn03iNe1wx5VXzevIGvVe6llnlDm2R5iboyZtl+crtfVdMhws15YI3SWeIuBU8TAz47lND8CzTSJiSZcGc", k.getPublicFingerprint());
    }
}
