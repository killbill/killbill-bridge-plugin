package org.killbill.billing.plugin.bridge.api.converter;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;

public class PaymentTransactionInfoPluginResultConverter implements ResultConverter<PaymentTransaction, PaymentTransactionInfoPlugin> {

    @Override
    public PaymentTransactionInfoPlugin convertModelToApi(final PaymentTransaction paymentTransaction) {
        return new PluginPaymentTransactionInfoPlugin(paymentTransaction.getPaymentId(),
                paymentTransaction.getTransactionId(),
                TransactionType.valueOf(paymentTransaction.getTransactionType()),
                paymentTransaction.getAmount(),
                Currency.valueOf(paymentTransaction.getCurrency()),
                ConverterHelper.toPluginStatus(paymentTransaction.getStatus()),
                paymentTransaction.getGatewayErrorMsg(),
                paymentTransaction.getGatewayErrorCode(),
                paymentTransaction.getFirstPaymentReferenceId(),
                paymentTransaction.getSecondPaymentReferenceId(),
                paymentTransaction.getEffectiveDate(),
                paymentTransaction.getEffectiveDate(),
                ConverterHelper.convertToApiPluginProperties(paymentTransaction.getProperties()));

    }
}
