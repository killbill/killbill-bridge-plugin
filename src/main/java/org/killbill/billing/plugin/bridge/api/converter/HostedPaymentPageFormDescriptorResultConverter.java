package org.killbill.billing.plugin.bridge.api.converter;

import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.plugin.api.payment.PluginHostedPaymentPageFormDescriptor;
import org.killbill.billing.plugin.bridge.api.BridgePaymentPluginApi;

public class HostedPaymentPageFormDescriptorResultConverter implements ResultConverter<org.killbill.billing.client.model.HostedPaymentPageFormDescriptor, HostedPaymentPageFormDescriptor> {
    @Override
    public HostedPaymentPageFormDescriptor convertModelToApi(final org.killbill.billing.client.model.HostedPaymentPageFormDescriptor desc) {
        return new PluginHostedPaymentPageFormDescriptor(desc.getKbAccountId(),
                desc.getFormUrl(),
                desc.getFormMethod(),
                ConverterHelper.convertToApiPluginProperties(desc.getFormFields()),
                ConverterHelper.convertToApiPluginProperties(desc.getProperties()));
    }
}
