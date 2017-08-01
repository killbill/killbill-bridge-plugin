package org.killbill.billing.plugin.bridge;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

public class KillbillClientConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<KillBillClient> implements Closeable {


    public KillbillClientConfigurationHandler(final String pluginName, final OSGIKillbillAPI osgiKillbillAPI, final OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
    }

    @Override
    protected KillBillClient createConfigurable(final Properties properties) {

        final String serverHost = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "serverHost");
        final Integer serverPort = getIntegerProperty(properties, BridgeActivator.PROPERTY_PREFIX + "serverPort", 80);
        final String username = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "username");
        final String password = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "password");
        final String apiKey = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "apiKey");
        final String apiSecret = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "apiSecret");
        final String proxyHost = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "proxyHost", null);
        final Integer proxyPort = getIntegerProperty(properties, BridgeActivator.PROPERTY_PREFIX + "proxyPort");
        final Integer connectTimeOut = getIntegerProperty(properties, BridgeActivator.PROPERTY_PREFIX + "connectTimeOut");
        final Integer readTimeOut = getIntegerProperty(properties, BridgeActivator.PROPERTY_PREFIX + "readTimeOut");
        final Integer requestTimeout = getIntegerProperty(properties, BridgeActivator.PROPERTY_PREFIX + "requestTimeout");
        final Boolean strictSSL = getBooleanProperty(properties, BridgeActivator.PROPERTY_PREFIX + "strictSSL");
        final String SSLProtocol = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "SSLProtocol", null);



        final KillBillHttpClient httpClient = new KillBillHttpClient(String.format("%s://%s:%d",
                SSLProtocol != null ? "https" : "http", serverHost, serverPort),
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
        return new KillBillClient(httpClient);
    }


    private Integer getIntegerProperty(final Properties properties, final String targetProperty, final Integer defaultValue) {
        final String value = properties.getProperty(targetProperty);
        return (value != null) ? Integer.valueOf(properties.getProperty(targetProperty)) : defaultValue;
    }

    private Integer getIntegerProperty(final Properties properties, final String targetProperty) {
        return getIntegerProperty(properties, targetProperty, null);
    }

    private Boolean getBooleanProperty(final Properties properties, final String targetProperty) {
        final String value = properties.getProperty(targetProperty);
        return (value != null) ? Boolean.valueOf(properties.getProperty(targetProperty)) : null;
    }

    @Override
    public void close() throws IOException {
        // TODO we need acceess to the private pluginTenantConfigurable
    }
}
