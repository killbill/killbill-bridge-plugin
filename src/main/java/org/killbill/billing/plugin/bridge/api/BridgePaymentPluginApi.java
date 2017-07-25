package org.killbill.billing.plugin.bridge.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.model.Payment;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.bridge.BridgeConfigurationHandler;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public PaymentTransactionInfoPlugin authorizePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.AUTHORIZE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.CAPTURE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.VOID, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, null, null, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.CREDIT, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return proxyTransactionOperation(TransactionType.REFUND, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(UUID kbAccountId, UUID kbPaymentId, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(String searchKey, Long offset, Long limit, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, PaymentMethodPlugin paymentMethodProps, boolean setDefault, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public void deletePaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(UUID kbAccountId, boolean refreshFromGateway, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(String searchKey, Long offset, Long limit, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(UUID kbAccountId, List<PaymentMethodInfoPlugin> paymentMethods, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(UUID kbAccountId, Iterable<PluginProperty> customFields, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public GatewayNotification processNotification(String notification, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    private  PaymentTransactionInfoPlugin proxyTransactionOperation(final TransactionType transactionType, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, @Nullable final BigDecimal amount, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final KillBillClient client = configurationHandler.getConfigurable(context.getTenantId());

        final PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionType(transactionType.name());
        transaction.setPaymentId(kbPaymentId);
        transaction.setTransactionId(kbTransactionId);
        if (amount != null) {
            transaction.setAmount(amount);
        }
        if (currency != null) {
            transaction.setCurrency(currency.toString());
        }

        final RequestOptions requestOptionsWithoutFollowLocation = RequestOptions.builder()
                .withCreatedBy(createdBy)
                .withReason(reason)
                .withComment(comment)
                .withFollowLocation(false)
                .build();

        try {
            final Payment result = client.createPayment(kbAccountId, kbPaymentMethodId, transaction, convertToClientPluginProperties(properties), requestOptionsWithoutFollowLocation);

            return toPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, result);
        } catch (final KillBillClientException e) {
            final String errorMsg = String.format("Failed to run payment operation='%s', account='%s', paymentMethodId='%s', payment='%s'", transactionType, kbAccountId, kbPaymentMethodId, kbPaymentId);
            final String errorType =  (e.getBillingException() != null) ? String.format("Error type='%d'", e.getBillingException().getCode()) : null ;
            throw new PaymentPluginApiException(errorType, errorMsg);
        }

    }

    private static PaymentTransactionInfoPlugin toPaymentTransactionInfoPlugin(final UUID kbPaymentId, final UUID kbTransactionId, final Payment result) {
        final Optional<PaymentTransaction> optionalTargetTransaction = getTransactionMatchOrLast(result.getTransactions(), kbTransactionId);
        Preconditions.checkState(optionalTargetTransaction.isPresent(), String.format("Cannot find the matching transaction for payment='%s', kbTransactionId='%s'", kbPaymentId, kbTransactionId));

        final PaymentTransaction targetTransaction = optionalTargetTransaction.get();
        final PaymentPluginStatus pluginStatus = null;

        return new PluginPaymentTransactionInfoPlugin(targetTransaction.getPaymentId(),
                targetTransaction.getTransactionId(),
                TransactionType.valueOf(targetTransaction.getTransactionType()),
                targetTransaction.getAmount(),
                Currency.valueOf(targetTransaction.getCurrency()),
                pluginStatus,
                targetTransaction.getGatewayErrorMsg(),
                targetTransaction.getGatewayErrorCode(),
                targetTransaction.getFirstPaymentReferenceId(),
                targetTransaction.getSecondPaymentReferenceId(),
                targetTransaction.getEffectiveDate(),
                targetTransaction.getEffectiveDate(),
                convertToApiPluginProperties(targetTransaction.getProperties()));
    }

    static List<PluginProperty> convertToApiPluginProperties(@Nullable final List<org.killbill.billing.client.model.PluginProperty> input) {
        return input != null ?
                input.stream()
                        .map(p -> new PluginProperty(p.getKey(), p.getValue(), p.getIsUpdatable()))
                        .collect(Collectors.toList()) :
                ImmutableList.of();
    }


    static Map<String, String> convertToClientPluginProperties(@Nullable final Iterable<PluginProperty> input) {

        final List<PluginProperty> sanitizedInput = input != null ? ImmutableList.copyOf(input) : ImmutableList.of();
        return sanitizedInput.stream()
                .collect(Collectors.toMap(s -> s.getKey(), s -> (String) s.getValue()));
    }

    static Optional<PaymentTransaction> getTransactionMatchOrLast(final List<PaymentTransaction> transactions, @Nullable final UUID kbTransactionId) {
        PaymentTransaction result = null;
        if (transactions != null && !transactions.isEmpty()) {
            result = transactions.stream().filter(paymentTransaction -> (kbTransactionId != null && paymentTransaction.getTransactionId().equals(kbTransactionId)))
                    //.peek(paymentTransaction -> System.err.println(String.format(".... transactionId ='%s'", paymentTransaction.getTransactionId())))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), paymentTransactions -> paymentTransactions.isEmpty() ? transactions.get(transactions.size() - 1) : paymentTransactions.get(0)));
        }
        return Optional.ofNullable(result);
    }

}
