/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.bridge;

import java.util.Properties;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

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
        final String pluginPropertiesList = properties.getProperty(BridgeActivator.PROPERTY_PREFIX + "pluginProperties", null);
        return new PaymentConfig(proxyModel, internalPaymentMethodIdName, controlPluginList, pluginPropertiesList);
    }
}
