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

package org.killbill.billing.plugin.bridge.core;

import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.plugin.bridge.KillbillClientConfigurationHandler;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.api.AuditLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * For the healthcheck to work with the main Kill Bill healthcheck, make sure to set in your global killbill.properties file:
 *
 *    org.killbill.billing.plugin.bridge.serverUrl=http://127.0.0.1:8080
 *    org.killbill.billing.plugin.bridge.username=admin
 *    org.killbill.billing.plugin.bridge.password=password
 *    org.killbill.billing.plugin.bridge.apiKey=bob
 *    org.killbill.billing.plugin.bridge.apiSecret=lazar
 */
public class BridgeHealthcheck implements Healthcheck {

    private static final Logger logger = LoggerFactory.getLogger(BridgeHealthcheck.class);

    private final KillbillClientConfigurationHandler configurationHandler;

    public BridgeHealthcheck(final KillbillClientConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public HealthStatus getHealthStatus(@Nullable final Tenant tenant, @Nullable final Map properties) {
        final KillBillClient client;
        try {
            client = configurationHandler.getConfigurable(tenant == null ? null : tenant.getId());
        } catch (final NullPointerException e) {
            return HealthStatus.healthy("Healthcheck not configured");
        }

        try {
            client.getAccounts(0L, 1L, AuditLevel.NONE, RequestOptions.builder()
                                                                      .withCreatedBy("BridgeHealthcheck")
                                                                      .build());
            return HealthStatus.healthy("KB_P OK");
        } catch (final Exception exception) {
            logger.warn("Healthcheck failed", exception);
            return HealthStatus.unHealthy("KB_P " + (exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()));
        }
    }
}
