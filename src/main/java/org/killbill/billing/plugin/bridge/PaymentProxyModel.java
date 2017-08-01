package org.killbill.billing.plugin.bridge;

public enum PaymentProxyModel {
    PROXY_SIMPLE("proxy"),
    PROXY_ROUTING("proxy-routing");

    private String name;

    PaymentProxyModel(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PaymentProxyModel findPaymentProxyModel(final String name) {
        for (PaymentProxyModel cur :  PaymentProxyModel.values()) {
            if (name.equalsIgnoreCase(cur.getName())) {
                return cur;
            }
        }
        throw new IllegalStateException("Cannot find PaymentProxyModel " + name);
    }
}
