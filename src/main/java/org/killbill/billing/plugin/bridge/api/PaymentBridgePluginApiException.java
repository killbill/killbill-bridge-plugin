package org.killbill.billing.plugin.bridge.api;

import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;

import java.util.UUID;

public class PaymentBridgePluginApiException extends PaymentPluginApiException {
    public PaymentBridgePluginApiException(final KillBillClientException e, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbPaymentMethodId, final String transactionType) {
        super((e.getBillingException() != null) ? String.format("Error type='%d'", e.getBillingException().getCode()) : null,
                String.format("Failed to run payment operation='%s', account='%s', paymentMethodId='%s', payment='%s'",
                        transactionType,
                        (kbAccountId != null) ? kbAccountId : "n/a",
                        (kbPaymentMethodId != null) ? kbPaymentMethodId : "n/a",
                        (kbPaymentId != null) ? kbPaymentId : "n/a"));
    }
}
