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

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestPaymentConfigurationHandler {

    @Test(groups = "fast")
    public void testParseYAML() {
        final PaymentConfigurationHandler paymentConfigurationHandler = new PaymentConfigurationHandlerForTest("local");
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(UUID.randomUUID());
        Assert.assertEquals(paymentConfig.getProxyModel(), PaymentProxyModel.PROXY_SIMPLE);
        Assert.assertTrue(paymentConfig.getPluginProperties().isEmpty());
    }

    private static final class PaymentConfigurationHandlerForTest extends PaymentConfigurationHandler {

        public PaymentConfigurationHandlerForTest(final String region) {
            super(BridgeActivator.PLUGIN_NAME,
                  Mockito.mock(OSGIKillbillAPI.class),
                  Mockito.mock(OSGIKillbillLogService.class),
                  region);
        }

        @Override
        public String getTenantConfigurationAsString(@Nullable final UUID kbTenantId) {
            return "local: !!org.killbill.billing.plugin.bridge.BridgeConfig\n" +
                   "  killbillClientConfig:\n" +
                   "    username: admin\n" +
                   "    password: password\n" +
                   "    apiKey: bob\n" +
                   "    apiSecret: lazar\n" +
                   "    serverUrl: http://127.0.0.1:8080\n" +
                   "  paymentConfig:\n" +
                   "    proxyModel: PROXY_SIMPLE";
        }
    }
}
