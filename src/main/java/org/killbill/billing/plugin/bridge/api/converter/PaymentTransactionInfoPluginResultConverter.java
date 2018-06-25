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

import java.util.Optional;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

public class PaymentTransactionInfoPluginResultConverter implements ResultConverter<PaymentTransaction, PaymentTransactionInfoPlugin> {

    private final Payment kbSPayment;

    public PaymentTransactionInfoPluginResultConverter(final Payment kbSPayment) {
        this.kbSPayment = kbSPayment;
    }

    @Override
    public PaymentTransactionInfoPlugin convertModelToApi(final PaymentTransaction kbPTransaction) {
        if (kbPTransaction == null) {
            return null;
        }

        final Optional<org.killbill.billing.payment.api.PaymentTransaction> kbSTransactionOptional = kbSPayment.getTransactions()
                                                                                                               .stream()
                                                                                                               .filter(t -> t.getExternalKey().equals(kbPTransaction.getTransactionExternalKey()))
                                                                                                               .findFirst();

        if (!kbSTransactionOptional.isPresent()) {
            return null;
        }

        final org.killbill.billing.payment.api.PaymentTransaction kbSTransaction = kbSTransactionOptional.get();
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
