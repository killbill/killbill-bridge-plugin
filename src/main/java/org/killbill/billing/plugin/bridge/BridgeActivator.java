/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;
import org.killbill.billing.plugin.bridge.core.BridgeHealthcheck;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.osgi.framework.BundleContext;

public class BridgeActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-bridge";

    public static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.bridge.";

    private KillbillClientConfigurationHandler killbillClientConfigurationHandler;
    private PaymentConfigurationHandler paymentConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

        killbillClientConfigurationHandler = new KillbillClientConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);

        final KillBillClient globalKillBillClient = getGlobalKillBillClient();
        killbillClientConfigurationHandler.setDefaultConfigurable(globalKillBillClient);

        paymentConfigurationHandler = new PaymentConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);

        final PaymentPluginApi api = new BridgePaymentPluginApi(killbillAPI, logService, killbillClientConfigurationHandler, paymentConfigurationHandler);
        registerPaymentPluginApi(context, api);

        registerEventHandlers();

        final Healthcheck bridgeHealthcheck = new BridgeHealthcheck(killbillClientConfigurationHandler);
        registerHealthcheck(context, bridgeHealthcheck);
    }

    private KillBillClient getGlobalKillBillClient() throws MalformedURLException {
        final BridgeConfig bridgeConfig = new BridgeConfig();
        bridgeConfig.killbillClientConfig = new KillbillClientConfig();

        final String serverUrlString = configProperties.getString(PROPERTY_PREFIX + "serverUrl");
        if (serverUrlString == null) {
            return null;
        }

        bridgeConfig.killbillClientConfig.serverUrl = new URL(serverUrlString);
        bridgeConfig.killbillClientConfig.username = configProperties.getString(PROPERTY_PREFIX + "username");
        bridgeConfig.killbillClientConfig.password = configProperties.getString(PROPERTY_PREFIX + "password");
        bridgeConfig.killbillClientConfig.apiKey = configProperties.getString(PROPERTY_PREFIX + "apiKey");
        bridgeConfig.killbillClientConfig.apiSecret = configProperties.getString(PROPERTY_PREFIX + "apiSecret");
        return killbillClientConfigurationHandler.createConfigurable(bridgeConfig);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }

    private void registerEventHandlers() {
        final PluginConfigurationEventHandler pluginConfigurationEventHandler = new PluginConfigurationEventHandler(killbillClientConfigurationHandler,
                                                                                                                    paymentConfigurationHandler);
        dispatcher.registerEventHandlers(pluginConfigurationEventHandler);
    }

    private void registerHealthcheck(final BundleContext context, final Healthcheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Healthcheck.class, healthcheck, props);
    }
}
