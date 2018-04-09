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

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.killbill.billing.payment.api.PluginProperty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class PaymentConfig {

    private static final Splitter PIPE_SPLITTER = Splitter.on("|");
    private static final Splitter HASH_SPLITTER = Splitter.on("#");
    private static final Joiner HASH_JOINER = Joiner.on("#");

    private final PaymentProxyModel proxyModel;
    private final String internalPaymentMethodIdName;
    private final List<String> controlPlugins;
    private final List<PluginProperty> pluginProperties = new LinkedList<PluginProperty>();

    public PaymentConfig(final String proxyModel, final String internalPaymentMethodIdName, @Nullable final String controlPluginList, @Nullable final String pluginPropertiesList) {
        this.internalPaymentMethodIdName = internalPaymentMethodIdName;
        this.proxyModel = PaymentProxyModel.findPaymentProxyModel(proxyModel);
        this.controlPlugins = controlPluginList != null ? ImmutableList.copyOf(controlPluginList.split(",\\s*")) : ImmutableList.of();
        if (pluginPropertiesList != null) {
            for (final String pluginPropertyPair : PIPE_SPLITTER.split(pluginPropertiesList)) {
                final List<String> pluginPropertyParts = ImmutableList.<String>copyOf(HASH_SPLITTER.split(pluginPropertyPair));
                if (pluginPropertyParts.size() > 1) {
                    pluginProperties.add(new PluginProperty(pluginPropertyParts.get(0), HASH_JOINER.join(pluginPropertyParts.subList(1, pluginPropertyParts.size())), false));
                }
            }
        }
    }

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
        return pluginProperties;
    }
}
