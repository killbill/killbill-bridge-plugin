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

package org.killbill.billing.plugin.bridge.api.converter;

import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;

public class PaymentMethodPluginResultConverter implements ResultConverter<org.killbill.billing.client.model.PaymentMethod, PaymentMethodPlugin> {
    @Override
    public PaymentMethodPlugin convertModelToApi(final org.killbill.billing.client.model.PaymentMethod paymentMethod) {
        if (paymentMethod != null) {
            return new PluginPaymentMethodPlugin(paymentMethod.getPaymentMethodId(),
                    paymentMethod.getPluginInfo().getExternalPaymentMethodId(),
                    paymentMethod.getIsDefault(),
                    ConverterHelper.convertToApiPluginProperties(paymentMethod.getPluginInfo().getProperties()));
        } else {
            return null;
        }
    }
}
