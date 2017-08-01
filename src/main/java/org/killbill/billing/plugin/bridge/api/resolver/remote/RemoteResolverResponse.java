package org.killbill.billing.plugin.bridge.api.resolver.remote;

import org.killbill.billing.plugin.bridge.api.resolver.ResolvingType;

import java.util.UUID;

public class RemoteResolverResponse {

    private final UUID accountIdMapping;
    private final UUID paymentMethodIdMapping;
    private final UUID paymentIdMapping;
    private final UUID transactionIdMapping;

    public RemoteResolverResponse(final UUID accountIdMapping, final UUID paymentMethodIdMapping, final UUID paymentIdMapping, final UUID transactionIdMapping) {
        this.accountIdMapping = accountIdMapping;
        this.paymentMethodIdMapping = paymentMethodIdMapping;
        this.paymentIdMapping = paymentIdMapping;
        this.transactionIdMapping = transactionIdMapping;
    }

    public UUID getAccountIdMapping() {
        return accountIdMapping;
    }

    public UUID getPaymentMethodIdMapping() {
        return paymentMethodIdMapping;
    }

    public UUID getPaymentIdMapping() {
        return paymentIdMapping;
    }

    public UUID getTransactionIdMapping() {
        return transactionIdMapping;
    }

    public static class RemoteResolverResponseBuilder {


        private UUID accountIdMapping;
        private UUID paymentMethodIdMapping;
        private UUID paymentIdMapping;
        private UUID transactionIdMapping;


        public void setAccountIdMapping(final UUID accountIdMapping) {
            this.accountIdMapping = accountIdMapping;
        }

        public void setPaymentMethodIdMapping(final UUID paymentMethodIdMapping) {
            this.paymentMethodIdMapping = paymentMethodIdMapping;
        }

        public void setPaymentIdMapping(final UUID paymentIdMapping) {
            this.paymentIdMapping = paymentIdMapping;
        }

        public void setTransactionIdMapping(final UUID transactionIdMapping) {
            this.transactionIdMapping = transactionIdMapping;
        }

        // Debug
        public UUID getMapping(final ResolvingType type) {
            switch (type) {
                case ACCOUNT:
                    return accountIdMapping;
                case PAYMENT_METHOD:
                    return paymentMethodIdMapping;
                case PAYMENT:
                case PAYMENT_AND_TRANSACTION:
                    return paymentIdMapping;
                default:
                    throw new IllegalStateException("unknown type " + type);
            }
        }

        public RemoteResolverResponse build() {
            return new RemoteResolverResponse(accountIdMapping, paymentMethodIdMapping, paymentIdMapping, transactionIdMapping);
        }
    }
}
