package org.killbill.billing.plugin.bridge.api.converter;

import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;

public class PaymentMethodPluginResultConverter implements ResultConverter<org.killbill.billing.client.model.PaymentMethod, PaymentMethodPlugin> {
    @Override
    public PaymentMethodPlugin convertModelToApi(final org.killbill.billing.client.model.PaymentMethod paymentMethod) {
        return new PluginPaymentMethodPlugin(paymentMethod.getPaymentMethodId(),
                paymentMethod.getPluginInfo().getExternalPaymentMethodId(),
                paymentMethod.getIsDefault(),
                ConverterHelper.convertToApiPluginProperties(paymentMethod.getPluginInfo().getProperties()));
    }
}
