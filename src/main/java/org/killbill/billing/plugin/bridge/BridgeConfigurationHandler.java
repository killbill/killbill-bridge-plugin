package org.killbill.billing.plugin.bridge;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

import java.util.Properties;

public class BridgeConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<KillBillClient> {


    public BridgeConfigurationHandler(final String pluginName, final OSGIKillbillAPI osgiKillbillAPI, final OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
    }

    @Override
    protected KillBillClient createConfigurable(final Properties properties) {

        final String serverHost = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "serverHost");
        final String serverPort = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "serverPort");
        final String username = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "username");
        final String password = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "password");
        final String apiKey = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "apiKey");
        final String apiSecret = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "apiSecret");
        final String proxyHost = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "proxyHost", null);
        final Integer proxyPort = Integer.valueOf(properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "proxyPort", null));
        final Integer connectTimeOut = Integer.valueOf(properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "connectTimeOut", null));
        final Integer readTimeOut = Integer.valueOf(properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "readTimeOut", null));
        final Integer requestTimeout = Integer.valueOf(properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "requestTimeout", null));
        final Boolean strictSSL = Boolean.valueOf(properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "strictSSL", null));
        final String SSLProtocol = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "SSLProtocol", null);

        final KillBillHttpClient httpClient = new KillBillHttpClient(String.format("http://%s:%d", serverHost, serverPort),
                username,
                password,
                apiKey,
                apiSecret,
                proxyHost,
                proxyPort,
                connectTimeOut,
                readTimeOut,
                requestTimeout,
                strictSSL,
                SSLProtocol);
        return  new KillBillClient(httpClient);
    }
}
