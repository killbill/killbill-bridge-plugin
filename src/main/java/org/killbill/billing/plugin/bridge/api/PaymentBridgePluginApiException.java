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

package org.killbill.billing.plugin.bridge.api;

import java.util.UUID;

import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;

public class PaymentBridgePluginApiException extends PaymentPluginApiException {

    public PaymentBridgePluginApiException(final KillBillClientException e, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbPaymentMethodId, final String transactionType) {
        super(String.format("Failed to run payment operation='%s', account='%s', paymentMethodId='%s', payment='%s', errorType='%d'",
                            transactionType,
                            (kbAccountId != null) ? kbAccountId : "n/a",
                            (kbPaymentMethodId != null) ? kbPaymentMethodId : "n/a",
                            (kbPaymentId != null) ? kbPaymentId : "n/a",
                            (e.getBillingException() != null) ? e.getBillingException().getCode() : null), e);
    }


    public PaymentBridgePluginApiException(final String message, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbPaymentMethodId, final String transactionType) {
        super(String.format("Failed to run payment operation='%s', account='%s', paymentMethodId='%s', payment='%s', errorMsg='%s'",
                            transactionType,
                            (kbAccountId != null) ? kbAccountId : "n/a",
                            (kbPaymentMethodId != null) ? kbPaymentMethodId : "n/a",
                            (kbPaymentId != null) ? kbPaymentId : "n/a",
                            message), (String) null);
    }

}
