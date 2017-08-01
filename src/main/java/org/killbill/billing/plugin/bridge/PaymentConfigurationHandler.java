package org.killbill.billing.plugin.bridge;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

import java.util.Properties;

import static org.killbill.billing.plugin.bridge.PaymentProxyModel.PROXY_ROUTING;

public class PaymentConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<PaymentConfig> {

    public PaymentConfigurationHandler(final String pluginName, final OSGIKillbillAPI osgiKillbillAPI, final OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
    }

    @Override
    protected PaymentConfig createConfigurable(final Properties properties) {
        final String internalPaymentMethodIdName = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "internalPaymentMethodIdName", "internalPaymentMethodId");
        final String proxyModel = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "proxyModel", PROXY_ROUTING.getName());
        final String controlPluginList = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "controlPlugins", null);
        return new PaymentConfig(proxyModel, internalPaymentMethodIdName, controlPluginList);
    }
}
