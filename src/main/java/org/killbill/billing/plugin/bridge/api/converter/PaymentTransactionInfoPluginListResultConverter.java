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
