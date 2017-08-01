package org.killbill.billing.plugin.bridge;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

public class PaymentConfig {


    private final PaymentProxyModel proxyModel;
    private final String internalPaymentMethodIdName;
    private final List<String> controlPlugins;

    public PaymentConfig(final String proxyModel, final String internalPaymentMethodIdName, @Nullable final String controlPluginList) {
        this.internalPaymentMethodIdName = internalPaymentMethodIdName;
        this.proxyModel = PaymentProxyModel.findPaymentProxyModel(proxyModel);
        this.controlPlugins = controlPluginList != null ? ImmutableList.copyOf(controlPluginList.split(",\\s*")) : ImmutableList.of();
    }

    public PaymentProxyModel getProxyModel() {
        return proxyModel;
    }

    public String getInternalPaymentMethodIdName() {
        return internalPaymentMethodIdName;
    }

    public List<String> getControlPlugins() {
        return controlPlugins;
    }
}
