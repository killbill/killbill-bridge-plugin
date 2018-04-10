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

import java.util.UUID;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.core.config.YAMLPluginTenantConfigurationHandler;

public class BridgeConfigConfigurationHandlerForTest extends YAMLPluginTenantConfigurationHandler<BridgeConfig, BridgeConfig> {

    public BridgeConfigConfigurationHandlerForTest(final String region) {
        super(null, null, null, region);
    }

    @Override
    public String getTenantConfigurationAsString(@Nullable final UUID kbTenantId) {
        return "local: !!org.killbill.billing.plugin.bridge.BridgeConfig\n" +
               "  killbillClientConfig:\n" +
               "    &killbillClientConfigDefault\n" +
               "    username: admin\n" +
               "    password: password\n" +
               "    apiKey: bob\n" +
               "    apiSecret: lazar\n" +
               "    strictSSL: false\n" +
               "  paymentConfig:\n" +
               "    &paymentConfigDefault\n" +
               "    pluginProperties:\n" +
               "      skipGw: true\n" +
               "      deposit: \"{\\\"system\\\":[{\\\"type\\\":\\\"default\\\",\\\"corporation\\\":\\\"acme\\\"}]}\"\n" +
               "\n" +
               "eu-central-1: !!org.killbill.billing.plugin.bridge.BridgeConfig\n" +
               "  killbillClientConfig:\n" +
               "    <<: *killbillClientConfigDefault\n" +
               "    serverUrl: http://kb.eu:8080\n" +
               "  paymentConfig:\n" +
               "    <<: *paymentConfigDefault\n" +
               "\n" +
               "ap-northeast-2: !!org.killbill.billing.plugin.bridge.BridgeConfig\n" +
               "  killbillClientConfig:\n" +
               "    <<: *killbillClientConfigDefault\n" +
               "    serverUrl: http://kb.apac:8080\n" +
               "  paymentConfig:\n" +
               "    <<: *paymentConfigDefault\n";
    }
}
