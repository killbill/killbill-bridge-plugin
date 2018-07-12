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

import java.util.List;
import java.util.Map;

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;

public class PaymentConfig {

    public PaymentProxyModel proxyModel;
    public String internalPaymentMethodIdName;
    public List<String> controlPlugins;
    public Map pluginProperties;

    public PaymentProxyModel getProxyModel() {
        return proxyModel;
    }

    public String getInternalPaymentMethodIdName() {
        return internalPaymentMethodIdName;
    }

    public List<String> getControlPlugins() {
        return controlPlugins;
    }

    public List<PluginProperty> getPluginProperties() {
        return PluginProperties.buildPluginProperties(pluginProperties);
    }
}
