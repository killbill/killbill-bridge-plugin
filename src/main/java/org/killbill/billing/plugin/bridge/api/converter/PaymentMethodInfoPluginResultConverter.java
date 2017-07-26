package org.killbill.billing.plugin.bridge.api.converter;

import org.killbill.billing.client.model.PaymentMethods;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodInfoPlugin;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodInfoPluginResultConverter implements ResultConverter<PaymentMethods, List<PaymentMethodInfoPlugin>> {

    @Override
    public List<PaymentMethodInfoPlugin> convertModelToApi(final PaymentMethods paymentMethods) {
        return paymentMethods.stream()
                .map(pm -> new PluginPaymentMethodInfoPlugin(pm.getAccountId(),
                        pm.getPaymentMethodId(),
                        pm.getIsDefault(),
                        pm.getPluginInfo().getExternalPaymentMethodId()))
                .collect(Collectors.toList());
    }
}
