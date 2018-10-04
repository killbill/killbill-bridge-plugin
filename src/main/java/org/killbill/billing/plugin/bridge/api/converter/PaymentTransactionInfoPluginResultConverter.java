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

import java.util.List;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

import com.google.common.collect.LinkedListMultimap;

public class PaymentTransactionInfoPluginResultConverter implements ResultConverter<PaymentTransaction, PaymentTransactionInfoPlugin> {

    private final Payment kbSPayment;
    // -1 means last one
    private final int kbPTxNb;

    public PaymentTransactionInfoPluginResultConverter(final Payment kbSPayment, final int kbPTxNb) {
        this.kbSPayment = kbSPayment;
        this.kbPTxNb = kbPTxNb;
    }

    @Override
    public PaymentTransactionInfoPlugin convertModelToApi(final PaymentTransaction kbPTransaction) {
        if (kbPTransaction == null) {
            return null;
        }

        final LinkedListMultimap<String, org.killbill.billing.payment.api.PaymentTransaction> kbSPaymentTransactionsByExternalKey = LinkedListMultimap.create();
        for (final org.killbill.billing.payment.api.PaymentTransaction kbSPaymentTransaction : kbSPayment.getTransactions()) {
            kbSPaymentTransactionsByExternalKey.put(kbSPaymentTransaction.getExternalKey(), kbSPaymentTransaction);
        }

        // We rely on the ordering, both on KB_S and KB_P, to match transactions with the same external key (we don't have any other way unfortunately)
        org.killbill.billing.payment.api.PaymentTransaction kbSTransaction = null;
        final List<org.killbill.billing.payment.api.PaymentTransaction> kbSPaymentTransactionsForExternalKey = kbSPaymentTransactionsByExternalKey.get(kbPTransaction.getTransactionExternalKey());
        if (kbSPaymentTransactionsForExternalKey.isEmpty()) {
            return null;
        } else if (kbPTxNb == -1) {
            kbSTransaction = kbSPaymentTransactionsForExternalKey.get(kbSPaymentTransactionsForExternalKey.size() - 1);
        } else {
            kbSTransaction = kbSPaymentTransactionsForExternalKey.get(kbPTxNb);
        }

        return new PluginPaymentTransactionInfoPlugin(kbSTransaction.getPaymentId(),
                                                      kbSTransaction.getId(),
                                                      kbSTransaction.getTransactionType(),
                                                      kbPTransaction.getAmount(),
                                                      Currency.valueOf(kbPTransaction.getCurrency()),
                                                      ConverterHelper.toPluginStatus(kbPTransaction.getStatus()),
                                                      kbPTransaction.getGatewayErrorMsg(),
                                                      kbPTransaction.getGatewayErrorCode(),
                                                      kbPTransaction.getFirstPaymentReferenceId(),
                                                      kbPTransaction.getSecondPaymentReferenceId(),
                                                      kbPTransaction.getEffectiveDate(),
                                                      kbPTransaction.getEffectiveDate(),
                                                      ConverterHelper.convertToApiPluginProperties(kbPTransaction.getProperties()));

    }
}
