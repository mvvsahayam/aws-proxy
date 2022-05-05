package io.github.mvvs.proxy;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.log4j.xml.DOMConfigurator;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.ClientDetails;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;
import org.littleshoot.proxy.mitm.RootCertificateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

public class ProxyLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyLauncher.class);
    private static Properties prop = new Properties();

    public static void main(final String... args) throws IOException {
        pollLog4JConfigurationFileIfAvailable();
        LOG.info("Running AWS Forward Proxy with args: {}", Arrays.asList(args));

        FileInputStream fis = new FileInputStream("proxy.properties");
        prop.load(fis);

        try {
            final int port = Integer.parseInt(prop.getProperty("port"));
            LOG.info("About to start server on port: {}", port);
            List<String> whitelistedAccounts = Arrays.asList(prop.getProperty("whitelisted_accounts").split("\\s*,\\s*"));
            BlockingHttpsFilterSource blockingHttpsFilterSource = new BlockingHttpsFilterSource(whitelistedAccounts);

            Authority authority = new Authority(new File("."),
                    prop.getProperty("alias"),
                    prop.getProperty("password").toCharArray(),
                    prop.getProperty("common_name"),
                    prop.getProperty("organization"),
                    prop.getProperty("organizational_unit_name"),
                    prop.getProperty("cert_organization"),
                    prop.getProperty("cert_organizational_unit_name"));

            HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer
                    .bootstrap()
                    .withName("AWSProxy")
                    .withFiltersSource(blockingHttpsFilterSource)
                    .withPort(port)
                    .withAllowLocalOnly(false)
//                    .withChainProxyManager(chainedProxyManager(prop.getProperty("chained_proxy_endpoint"),Integer.parseInt(prop.getProperty("chained_proxy_port"))))
                    .withManInTheMiddle(new CertificateSniffingMitmManager(authority));

            LOG.info("About to start..");
            bootstrap.start();
        } catch (RootCertificateException e) {
            throw new RuntimeException(e);
        }
    }

    protected static ChainedProxyManager chainedProxyManager(final String host, final Integer port) {
        return new ChainedProxyManager() {
            @Override
            public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies, ClientDetails clientDetails) {
                chainedProxies.add(new BaseChainedProxy(host, port));
            }
        };
    }

    protected static class BaseChainedProxy extends ChainedProxyAdapter {
        String host;
        Integer port;

        public BaseChainedProxy(String host, Integer port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public InetSocketAddress getChainedProxyAddress() {
            try {
                return new InetSocketAddress(InetAddress.getByName(this.host), this.port);
            } catch (UnknownHostException uhe) {
                throw new RuntimeException(
                        "Unable to resolve " + this.host);
            }
        }
    }

    private static void pollLog4JConfigurationFileIfAvailable() {
        File log4jConfigurationFile = new File("src/main/resources/log4j.xml");
        if (log4jConfigurationFile.exists()) {
            DOMConfigurator.configureAndWatch(
                    log4jConfigurationFile.getAbsolutePath(), 15);
        }
    }
}
