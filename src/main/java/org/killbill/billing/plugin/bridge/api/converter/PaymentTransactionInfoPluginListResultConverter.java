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

package org.killbill.billing.plugin.bridge.api.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.bridge.api.resolver.local.LocalResolver;
import org.killbill.billing.util.callcontext.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

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
        } catch (final PaymentPluginApiException e) {
            logger.warn("Failed to retrieve kbPaymentId='{}'", kbPaymentId, e);
            return ImmutableList.<PaymentTransactionInfoPlugin>of();
        }

        final LinkedListMultimap<String, org.killbill.billing.payment.api.PaymentTransaction> kbSPaymentTransactionsByExternalKey = LinkedListMultimap.create();
        for (final org.killbill.billing.payment.api.PaymentTransaction kbSPaymentTransaction : kbSPayment.getTransactions()) {
            kbSPaymentTransactionsByExternalKey.put(kbSPaymentTransaction.getExternalKey(), kbSPaymentTransaction);
        }

        final LinkedListMultimap<String, PaymentTransaction> kbPPaymentTransactionsByExternalKey = LinkedListMultimap.create();
        for (final PaymentTransaction kbPPaymentTransaction : kbPPayment.getTransactions()) {
            kbPPaymentTransactionsByExternalKey.put(kbPPaymentTransaction.getTransactionExternalKey(), kbPPaymentTransaction);
        }

        final List<PaymentTransactionInfoPlugin> list = new ArrayList<>();
        for (final String kbSTransactionExternalKey : kbSPaymentTransactionsByExternalKey.keySet()) {
            final List<org.killbill.billing.payment.api.PaymentTransaction> kbSPaymentTransactions = kbSPaymentTransactionsByExternalKey.get(kbSTransactionExternalKey);
            final List<PaymentTransaction> kbPPaymentTransactions = kbPPaymentTransactionsByExternalKey.get(kbSTransactionExternalKey);

            for (int i = 0; i < kbSPaymentTransactions.size(); i++) {
                final org.killbill.billing.payment.api.PaymentTransaction kbSPaymentTransaction = kbSPaymentTransactions.get(i);

                final PaymentTransaction kbPPaymentTransaction;
                if (i >= kbPPaymentTransactions.size()) {
                    // Data integrity issue: more transaction on KB_S
                    kbPPaymentTransaction = kbPPaymentTransactions.get(kbPPaymentTransactions.size() - 1);
                } else {
                    kbPPaymentTransaction = kbPPaymentTransactions.get(i);
                }

                list.add(new PluginPaymentTransactionInfoPlugin(kbSPaymentTransaction.getPaymentId(),
                                                                kbSPaymentTransaction.getId(),
                                                                kbSPaymentTransaction.getTransactionType(),
                                                                kbSPaymentTransaction.getAmount(),
                                                                Currency.valueOf(kbPPaymentTransaction.getCurrency()),
                                                                ConverterHelper.toPluginStatus(kbPPaymentTransaction.getStatus()),
                                                                kbPPaymentTransaction.getGatewayErrorMsg(),
                                                                kbPPaymentTransaction.getGatewayErrorCode(),
                                                                kbPPaymentTransaction.getFirstPaymentReferenceId(),
                                                                kbPPaymentTransaction.getSecondPaymentReferenceId(),
                                                                kbPPaymentTransaction.getEffectiveDate(),
                                                                kbPPaymentTransaction.getEffectiveDate(),
                                                                ConverterHelper.convertToApiPluginProperties(kbPPaymentTransaction.getProperties())));
            }
        }

        return list;
    }
}
