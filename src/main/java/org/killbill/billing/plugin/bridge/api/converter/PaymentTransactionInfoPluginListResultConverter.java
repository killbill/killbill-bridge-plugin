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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.bridge.api.resolver.local.LocalResolver;
import org.killbill.billing.util.callcontext.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class PaymentTransactionInfoPluginListResultConverter implements ResultConverter<org.killbill.billing.client.model.Payment, List<PaymentTransactionInfoPlugin>> {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTransactionInfoPluginListResultConverter.class);

    private final OSGIKillbillAPI killbillAPI;
    private final UUID kbPaymentId;
    private final TenantContext context;

    public PaymentTransactionInfoPluginListResultConverter(final OSGIKillbillAPI killbillAPI, final UUID kbPaymentId, final TenantContext context) {
        this.killbillAPI = killbillAPI;
        this.kbPaymentId = kbPaymentId;
        this.context = context;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> convertModelToApi(final org.killbill.billing.client.model.Payment kbPPayment) {
        if (kbPPayment == null || kbPPayment.getTransactions() == null) {
            return ImmutableList.<PaymentTransactionInfoPlugin>of();
        }

            final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
            final Payment kbSPayment;
            try {
                kbSPayment = localResolver.getPayment(kbPaymentId);
            } catch (PaymentPluginApiException e) {
                logger.warn("Failed to retrieve kbPaymentId='{}'", kbPaymentId, e);
                return ImmutableList.<PaymentTransactionInfoPlugin>of();
            }

        return kbPPayment.getTransactions()
                      .stream()
                      .map(kbPTransaction -> new PaymentTransactionInfoPluginResultConverter(kbSPayment).convertModelToApi(kbPTransaction))
                      .collect(Collectors.toList());
    }
}
