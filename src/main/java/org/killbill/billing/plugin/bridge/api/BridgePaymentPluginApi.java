package org.killbill.billing.plugin.bridge.api;

import com.google.common.base.Preconditions;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.bridge.BridgeConfigurationHandler;
import org.killbill.billing.plugin.bridge.api.converter.ConverterHelper;
import org.killbill.billing.plugin.bridge.api.converter.HostedPaymentPageFormDescriptorResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentMethodInfoPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentMethodPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentTransactionInfoPluginListResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentTransactionInfoPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.ResultConverter;
import org.killbill.billing.util.api.AuditLevel;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// NOTE: We explicitly import 'org.killbill.billing.client.model.*' objects to avoid confusing api and client model -- which often have the same name


// TODO Mapping of ID paymentMethodId, paymentId ,...

public class BridgePaymentPluginApi implements PaymentPluginApi, Closeable {

    protected static final String createdBy = "BridgePaymentPluginApi";
    protected static final String reason = null;
    protected static final String comment = null;


    private final BridgeConfigurationHandler configurationHandler;

    public BridgePaymentPluginApi(final BridgeConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public void close() throws IOException {
        // TODO call close on KillBill client
    }


    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.AUTHORIZE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.CAPTURE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.VOID, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, null, null, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.CREDIT, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(TransactionType.REFUND, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(new ClientOperation<org.killbill.billing.client.model.Payment>(kbAccountId, kbPaymentId, null, "GET") {
                                                @Override
                                                public org.killbill.billing.client.model.Payment doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                    return client.getPayment(kbPaymentId, true, ConverterHelper.convertToClientMapPluginProperties(properties), AuditLevel.NONE, requestOptions);
                                                }
                                            },
                new PaymentTransactionInfoPluginListResultConverter(),
                context.getTenantId());
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        // TODO
        return null;
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        internalClientProxyOperation(new ClientOperation<Void>(kbAccountId, null, kbPaymentMethodId, "ADD_PAYMENT_METHOD") {
                                         @Override
                                         public Void doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                             final org.killbill.billing.client.model.PaymentMethod paymentMethod = new org.killbill.billing.client.model.PaymentMethod();
                                             paymentMethod.setAccountId(kbAccountId);
                                             // TODO Required by client
                                             paymentMethod.setPluginName(null);
                                             paymentMethod.setIsDefault(setDefault);
                                             client.createPaymentMethod(paymentMethod, requestOptions);
                                             return null;
                                         }
                                     },
                null,
                context.getTenantId());
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        internalClientProxyOperation(new ClientOperation<Void>(kbAccountId, null, kbPaymentMethodId, "DELETE_PAYMENT_METHOD") {
                                         @Override
                                         public Void doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                             client.deletePaymentMethod(kbPaymentMethodId, true, true, requestOptions);
                                             return null;
                                         }
                                     },
                null,
                context.getTenantId());
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(new ClientOperation<org.killbill.billing.client.model.PaymentMethod>(kbAccountId, null, kbPaymentMethodId, "GET_PAYMENT_METHOD") {
                                                @Override
                                                public org.killbill.billing.client.model.PaymentMethod doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                    return client.getPaymentMethod(kbPaymentMethodId, true, AuditLevel.NONE, requestOptions);
                                                }
                                            },
                new PaymentMethodPluginResultConverter(),
                context.getTenantId());
    }


    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // TODO Missing java client api
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(new ClientOperation<org.killbill.billing.client.model.PaymentMethods>(kbAccountId, null, null, "GET_ACCOUNT_PAYMENT_METHODS") {
                                                @Override
                                                public org.killbill.billing.client.model.PaymentMethods doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                    return client.getPaymentMethodsForAccount(kbAccountId, ConverterHelper.convertToClientMapPluginProperties(properties), true, AuditLevel.NONE, requestOptions);
                                                }
                                            },
                new PaymentMethodInfoPluginResultConverter(),
                context.getTenantId());
    }


    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        // TODO
        return null;
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new IllegalStateException("BridgePaymentPluginApi#resetPaymentMethods has not been implemented");
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalClientProxyOperation(new ClientOperation<org.killbill.billing.client.model.HostedPaymentPageFormDescriptor>(kbAccountId, null, null, "BUILD_FORM_DESC") {
                                                @Override
                                                public org.killbill.billing.client.model.HostedPaymentPageFormDescriptor doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                    final org.killbill.billing.client.model.HostedPaymentPageFields fields = new org.killbill.billing.client.model.HostedPaymentPageFields(ConverterHelper.convertToClientListPluginProperties(customFields));
                                                    return client.buildFormDescriptor(fields, kbAccountId, null, ConverterHelper.convertToClientMapPluginProperties(properties), requestOptions);
                                                }
                                            },
                new HostedPaymentPageFormDescriptorResultConverter(),
                context.getTenantId());
    }


    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // TODO
        return null;
    }

    private PaymentTransactionInfoPlugin internalClientProxyOperation(final TransactionType transactionType, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, @Nullable final BigDecimal amount, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

        final ClientOperation<org.killbill.billing.client.model.PaymentTransaction> op = new ClientOperation<org.killbill.billing.client.model.PaymentTransaction>(kbAccountId, kbPaymentId, kbPaymentMethodId, transactionType.name()) {
            @Override
            public org.killbill.billing.client.model.PaymentTransaction doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {

                final org.killbill.billing.client.model.PaymentTransaction transaction = new org.killbill.billing.client.model.PaymentTransaction();
                transaction.setTransactionType(transactionType.name());
                transaction.setPaymentId(kbPaymentId);
                transaction.setTransactionId(kbTransactionId);
                if (amount != null) {
                    transaction.setAmount(amount);
                }
                if (currency != null) {
                    transaction.setCurrency(currency.toString());
                }

                final org.killbill.billing.client.model.Payment result = client.createPayment(kbAccountId, kbPaymentMethodId, transaction, ConverterHelper.convertToClientMapPluginProperties(properties), requestOptions);
                // Filter the transaction associated with this operation
                final Optional<org.killbill.billing.client.model.PaymentTransaction> optionalTargetTransaction = ConverterHelper.getTransactionMatchOrLast(result.getTransactions(), kbTransactionId);
                Preconditions.checkState(optionalTargetTransaction.isPresent(), String.format("Cannot find the matching transaction for payment='%s', kbTransactionId='%s'", kbPaymentId, kbTransactionId));
                return optionalTargetTransaction.get();
            }
        };

        return internalClientProxyOperation(op, new PaymentTransactionInfoPluginResultConverter(), context.getTenantId());
    }

    private <R, CR> CR internalClientProxyOperation(final ClientOperation<R> op, final ResultConverter<R, CR> converter, final UUID tenantId) throws PaymentBridgePluginApiException {


        final RequestOptions requestOptions = RequestOptions.builder()
                .withCreatedBy(createdBy)
                .withReason(reason)
                .withComment(comment)
                .withFollowLocation(false)
                .build();

        final KillBillClient client = configurationHandler.getConfigurable(tenantId);
        try {
            final R result = op.doOperation(client, requestOptions);
            return converter.convertModelToApi(result);
        } catch (final KillBillClientException e) {
            throw new PaymentBridgePluginApiException(e, op.getKbAccountId(), op.getKbPaymentId(), op.getKbPaymentMethodId(), op.getTransactionType());
        }
    }


    private static abstract class ClientOperation<R> {

        private final UUID kbAccountId;
        private final UUID kbPaymentId;
        private final UUID kbPaymentMethodId;
        private final String transactionType;

        public ClientOperation(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbPaymentMethodId, final String transactionType) {
            this.kbAccountId = kbAccountId;
            this.kbPaymentId = kbPaymentId;
            this.kbPaymentMethodId = kbPaymentMethodId;
            this.transactionType = transactionType;
        }

        public abstract R doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException;

        public UUID getKbAccountId() {
            return kbAccountId;
        }

        public UUID getKbPaymentId() {
            return kbPaymentId;
        }

        public UUID getKbPaymentMethodId() {
            return kbPaymentMethodId;
        }

        public String getTransactionType() {
            return transactionType;
        }
    }
}
