/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.core.config.YAMLPluginTenantConfigurationHandler;

import com.google.common.base.Preconditions;

public class KillbillClientConfigurationHandler extends YAMLPluginTenantConfigurationHandler<BridgeConfig, KillBillClientOnOff> {

    public KillbillClientConfigurationHandler(final String pluginName,
                                              final OSGIKillbillAPI osgiKillbillAPI,
                                              final OSGIKillbillLogService osgiKillbillLogService,
                                              final String region) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService,region);
    }

    @Override
    protected KillBillClientOnOff createConfigurable(final BridgeConfig config) {
        final KillbillClientConfig killbillClientConfig = config.killbillClientConfig;
        Preconditions.checkNotNull(killbillClientConfig, "Plugin misconfigured: killbillClientConfig == null");

        final String username = killbillClientConfig.username;
        final String password = killbillClientConfig.password;
        final String apiKey = killbillClientConfig.apiKey;
        final String apiSecret = killbillClientConfig.apiSecret;
        final String proxyHost = killbillClientConfig.proxyUrl != null ? killbillClientConfig.proxyUrl.getHost() : null;
        Integer proxyPort = killbillClientConfig.proxyUrl != null ? killbillClientConfig.proxyUrl.getPort() : null;
        if (proxyHost != null && proxyPort == -1) {
            proxyPort = 80;
        }
        final Integer connectTimeOut = killbillClientConfig.connectTimeOut;
        final Integer readTimeOut = killbillClientConfig.readTimeOut;
        final Integer requestTimeout = killbillClientConfig.requestTimeout;
        final Boolean strictSSL = killbillClientConfig.strictSSL;
        final String SSLProtocol = killbillClientConfig.SSLProtocol;
        final Boolean isActive = killbillClientConfig.isActive;

        final KillBillHttpClient httpClient = new KillBillHttpClient(killbillClientConfig.serverUrl.toString(),
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
        return new KillBillClientOnOff(httpClient, isActive);
    }
}
