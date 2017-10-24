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

import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentTransactionInfoPluginListResultConverter implements ResultConverter<org.killbill.billing.client.model.Payment, List<PaymentTransactionInfoPlugin>> {

    @Override
    public List<PaymentTransactionInfoPlugin> convertModelToApi(final org.killbill.billing.client.model.Payment payment) {
        return payment.getTransactions()
                .stream()
                .map(targetTransaction -> new PaymentTransactionInfoPluginResultConverter().convertModelToApi(targetTransaction))
                .collect(Collectors.toList());
    }
}
