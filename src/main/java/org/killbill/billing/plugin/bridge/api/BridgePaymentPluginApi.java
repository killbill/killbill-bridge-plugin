/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.bridge.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.killbill.billing.ErrorCode;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.bridge.KillbillClientConfigurationHandler;
import org.killbill.billing.plugin.bridge.PaymentConfig;
import org.killbill.billing.plugin.bridge.PaymentConfigurationHandler;
import org.killbill.billing.plugin.bridge.PaymentProxyModel;
import org.killbill.billing.plugin.bridge.api.converter.ConverterHelper;
import org.killbill.billing.plugin.bridge.api.converter.HostedPaymentPageFormDescriptorResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentMethodInfoPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentMethodPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentTransactionInfoPluginListResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.PaymentTransactionInfoPluginResultConverter;
import org.killbill.billing.plugin.bridge.api.converter.ResultConverter;
import org.killbill.billing.plugin.bridge.api.resolver.local.LocalResolver;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolver;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolver.WrappedKillBillClientException;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolver.WrappedUnresolvedException;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolverRequest;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolverRequest.UnresolvedException;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolverResponse;
import org.killbill.billing.util.api.AuditLevel;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.osgi.service.log.LogService;

import com.ning.http.client.Response;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

// NOTE: We explicitly import 'org.killbill.billing.client.model.*' objects to avoid confusing api and client model -- which often have the same name

public class BridgePaymentPluginApi implements PaymentPluginApi {

    protected static final String createdBy = "BridgePaymentPluginApi";
    protected static final String reason = null;
    protected static final String comment = null;

    private static final RequestOptions DEFAULT_OPTIONS = RequestOptions.builder()
                                                                        .withCreatedBy(createdBy)
                                                                        .withReason(reason)
                                                                        .withComment(comment)
                                                                        .build();

    private final OSGIKillbillAPI killbillAPI;
    private final OSGIKillbillLogService logService;
    private final KillbillClientConfigurationHandler configurationHandler;
    private final PaymentConfigurationHandler paymentConfigurationHandler;

    public BridgePaymentPluginApi(final OSGIKillbillAPI killbillAPI, final OSGIKillbillLogService logService, final KillbillClientConfigurationHandler configurationHandler, final PaymentConfigurationHandler paymentConfigurationHandler) {
        this.configurationHandler = configurationHandler;
        this.paymentConfigurationHandler = paymentConfigurationHandler;
        this.killbillAPI = killbillAPI;
        this.logService = logService;
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.AUTHORIZE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.CAPTURE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.VOID, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, null, null, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.CREDIT, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return internalPaymentTransactionOperation(TransactionType.REFUND, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {

        final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
        final Payment payment = localResolver.getPayment(kbPaymentId);

        final PaymentTransactionInfoPluginListResultConverter converter = new PaymentTransactionInfoPluginListResultConverter(killbillAPI, kbPaymentId, context);
        return internalGenericPaymentTransactionOperation(new ClientOperation<org.killbill.billing.client.model.Payment>(null, null, null, "GET") {
                                                              @Override
                                                              public org.killbill.billing.client.model.Payment doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                                  return client.getPaymentByExternalKey(payment.getExternalKey(), true, ConverterHelper.convertToClientMapPluginProperties(properties), AuditLevel.NONE, requestOptions);
                                                              }
                                                          },
                                                          converter,
                                                          context.getTenantId(),
                                                          converter.convertModelToApi(new org.killbill.billing.client.model.Payment()));
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        throw new IllegalStateException("BridgePaymentPluginApi#searchPayments has not been implemented");
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        if (paymentConfig.getProxyModel() == PaymentProxyModel.PROXY_SIMPLE) {
            throw new IllegalStateException("BridgePaymentPluginApi#addPaymentMethod has not been implemented for 'proxy' model");
            /*
            internalGenericPaymentTransactionOperation(new ClientOperation<Void>(null, null, null, "ADD_PAYMENT_METHOD") {
                                             @Override
                                             public Void doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                 final org.killbill.billing.client.model.PaymentMethod paymentMethod = new org.killbill.billing.client.model.PaymentMethod();
                                                 paymentMethod.setAccountId(kbAccountId);
                                                 // TODO Required by client: Defaut tenant config or fancy way based on control plugin setting a plugin property
                                                 paymentMethod.setPluginName(null);
                                                 paymentMethod.setIsDefault(setDefault);
                                                 client.createPaymentMethod(paymentMethod, requestOptions);
                                                 return null;
                                             }
                                         },
                    null,
                    context.getTenantId());
             */
        }
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        if (paymentConfig.getProxyModel() == PaymentProxyModel.PROXY_SIMPLE) {

            final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
            final String pmExternalKey = localResolver.getPaymentMethodExternalKey(kbPaymentMethodId);

            internalGenericPaymentTransactionOperation(new ClientOperation<Void>(null, null, null, "DELETE_PAYMENT_METHOD") {
                                                           @Override
                                                           public Void doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, UnresolvedException {

                                                               final RemoteResolver resolver = new RemoteResolver(client, requestOptions);
                                                               final RemoteResolverResponse resolverResp = resolver.resolve(new RemoteResolverRequest()
                                                                                                                                    .resolvePM(pmExternalKey));
                                                               client.deletePaymentMethod(resolverResp.getPaymentMethodIdMapping(), true, true, requestOptions);
                                                               return null;
                                                           }
                                                       },
                                                       null,
                                                       context.getTenantId(),
                                                       null);
        }
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {

        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        if (paymentConfig.getProxyModel() == PaymentProxyModel.PROXY_SIMPLE) {

            final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
            final String paymentExternalKey = localResolver.getPaymentMethodExternalKey(kbPaymentMethodId);
            final PaymentMethodPluginResultConverter converter = new PaymentMethodPluginResultConverter();
            return internalGenericPaymentTransactionOperation(new ClientOperation<org.killbill.billing.client.model.PaymentMethod>(kbAccountId, null, kbPaymentMethodId, "GET_PAYMENT_METHOD") {
                                                                  @Override
                                                                  public org.killbill.billing.client.model.PaymentMethod doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException {
                                                                      return client.getPaymentMethodByKey(paymentExternalKey, true, AuditLevel.NONE, requestOptions);
                                                                  }
                                                              },
                                                              converter,
                                                              context.getTenantId(),
                                                              converter.convertModelToApi(new org.killbill.billing.client.model.PaymentMethod()));
        } else {
            return null;
        }
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        if (paymentConfig.getProxyModel() == PaymentProxyModel.PROXY_SIMPLE) {

            final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
            final Account account = localResolver.getAccount(kbAccountId);
            final String pmExternalKey = localResolver.getPaymentMethodExternalKey(kbPaymentMethodId);

            internalGenericPaymentTransactionOperation(new ClientOperation<Void>(null, null, null, "SET_DEFAULT_PAYMENT_METHOD") {
                                                           @Override
                                                           public Void doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, UnresolvedException {

                                                               final RemoteResolver resolver = new RemoteResolver(client, requestOptions);
                                                               final RemoteResolverResponse resolverResp = resolver.resolve(new RemoteResolverRequest()
                                                                                                                                    .resolveAccount(account, true)
                                                                                                                                    .resolvePM(pmExternalKey));
                                                               client.updateDefaultPaymentMethod(resolverResp.getAccountIdMapping(), resolverResp.getPaymentMethodIdMapping(), requestOptions);
                                                               return null;
                                                           }
                                                       },
                                                       null,
                                                       context.getTenantId(),
                                                       null);
        }
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        if (paymentConfig.getProxyModel() == PaymentProxyModel.PROXY_SIMPLE) {

            final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
            final Account account = localResolver.getAccount(kbAccountId);

            final PaymentMethodInfoPluginResultConverter converter = new PaymentMethodInfoPluginResultConverter();
            return internalGenericPaymentTransactionOperation(new ClientOperation<org.killbill.billing.client.model.PaymentMethods>(kbAccountId, null, null, "GET_ACCOUNT_PAYMENT_METHODS") {
                                                                  @Override
                                                                  public org.killbill.billing.client.model.PaymentMethods doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, UnresolvedException {

                                                                      final RemoteResolver resolver = new RemoteResolver(client, requestOptions);
                                                                      final RemoteResolverResponse resolverResp = resolver.resolve(new RemoteResolverRequest()
                                                                                                                                           .resolveAccount(account, true));
                                                                      return client.getPaymentMethodsForAccount(resolverResp.getAccountIdMapping(), ConverterHelper.convertToClientMapPluginProperties(properties), true, AuditLevel.NONE, requestOptions);
                                                                  }
                                                              },
                                                              converter,
                                                              context.getTenantId(),
                                                              converter.convertModelToApi(new org.killbill.billing.client.model.PaymentMethods()));
        } else {
            return null;
        }
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        throw new IllegalStateException("BridgePaymentPluginApi#searchPaymentMethods has not been implemented");
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new IllegalStateException("BridgePaymentPluginApi#resetPaymentMethods has not been implemented");
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

        final LocalResolver localResolver = new LocalResolver(killbillAPI, context);
        final Account account = localResolver.getAccount(kbAccountId);

        final HostedPaymentPageFormDescriptorResultConverter converter = new HostedPaymentPageFormDescriptorResultConverter();
        return internalGenericPaymentTransactionOperation(new ClientOperation<org.killbill.billing.client.model.HostedPaymentPageFormDescriptor>(kbAccountId, null, null, "BUILD_FORM_DESC") {
                                                              @Override
                                                              public org.killbill.billing.client.model.HostedPaymentPageFormDescriptor doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, UnresolvedException {
                                                                  final RemoteResolver resolver = new RemoteResolver(client, requestOptions);
                                                                  final RemoteResolverResponse resolverResp = resolver.resolve(new RemoteResolverRequest()
                                                                                                                                       .resolveAccount(account, true));

                                                                  final org.killbill.billing.client.model.HostedPaymentPageFields fields = new org.killbill.billing.client.model.HostedPaymentPageFields(ConverterHelper.convertToClientListPluginProperties(customFields));
                                                                  return client.buildFormDescriptor(fields, resolverResp.getAccountIdMapping(), null /* TODO ??? */, ConverterHelper.convertToClientMapPluginProperties(properties), requestOptions);
                                                              }
                                                          },
                                                          converter,
                                                          context.getTenantId(),
                                                          converter.convertModelToApi(new org.killbill.billing.client.model.HostedPaymentPageFormDescriptor()));
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {

        KillBillClient client = null;
        try {
            client = configurationHandler.getConfigurable(context.getTenantId());
            final Response response = client.processNotification(notification, null /* TODO ????*/, ConverterHelper.convertToClientMapPluginProperties(properties), DEFAULT_OPTIONS);
            // TODO
            return null;
        } catch (final KillBillClientException e) {
            throw new PaymentPluginApiException(String.format("Failed to processNotification for %s", notification), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private Iterable<PluginProperty> buildProperties(final Iterable<PluginProperty> originalProperties, final TenantContext context) {
        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());
        return PluginProperties.merge(paymentConfig.getPluginProperties(), originalProperties);
    }

    private PaymentTransactionInfoPlugin internalPaymentTransactionOperation(final TransactionType transactionType, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, @Nullable final BigDecimal amount, @Nullable final Currency currency, final Iterable<PluginProperty> originalProperties, final CallContext context) throws PaymentPluginApiException {

        logService.log(LogService.LOG_INFO, String.format("Bridge Payment ENTERING: transactionType='%s', kbAccountId='%s', kbPaymentId='%s', kbPaymentMethodId='%s', amount='%s', currency='%s'",
                                                          transactionType, kbAccountId, kbPaymentId, kbPaymentMethodId, amount, currency));

        final PaymentConfig paymentConfig = paymentConfigurationHandler.getConfigurable(context.getTenantId());

        final LocalResolver localResolver = new LocalResolver(killbillAPI, context);

        final Payment payment = localResolver.getPayment(kbPaymentId);
        final PaymentTransaction paymentTransaction = payment.getTransactions()
                                                             .stream()
                                                             .filter(t -> t.getId().equals(kbTransactionId))
                                                             .findFirst()
                                                             // It has to exist we are currently handling the call...
                                                             .get();

        final PluginProperty internalPaymentMethodIdProperty = new PluginProperty(paymentConfig.getInternalPaymentMethodIdName(), localResolver.getPaymentMethodExternalKey(kbPaymentMethodId), true);

        final ClientOperation<org.killbill.billing.client.model.PaymentTransaction> op = new ClientOperation<org.killbill.billing.client.model.PaymentTransaction>(kbAccountId, kbPaymentId, kbPaymentMethodId, transactionType.name()) {
            @Override
            public org.killbill.billing.client.model.PaymentTransaction doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, PaymentPluginApiException, UnresolvedException {

                final Account account = localResolver.getAccount(kbAccountId);

                final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest()
                        .resolveAccount(account, true);
                if (transactionType == TransactionType.REFUND ||
                    transactionType == TransactionType.VOID ||
                    transactionType == TransactionType.CAPTURE) {
                    remoteResolverRequest.resolvePayment(payment.getExternalKey());
                }
                final RemoteResolver resolver = new RemoteResolver(client, requestOptions);
                final RemoteResolverResponse resolverResp = resolver.resolve(remoteResolverRequest);

                final org.killbill.billing.client.model.PaymentTransaction transaction = new org.killbill.billing.client.model.PaymentTransaction();
                transaction.setTransactionType(transactionType.name());
                transaction.setPaymentExternalKey(payment.getExternalKey());
                transaction.setTransactionExternalKey(paymentTransaction.getExternalKey());
                transaction.setPaymentId(resolverResp.getPaymentIdMapping());
                if (amount != null) {
                    transaction.setAmount(amount);
                }
                if (currency != null) {
                    transaction.setCurrency(currency.toString());
                }

                final Iterable<PluginProperty> properties = buildProperties(originalProperties, context);
                final org.killbill.billing.client.model.Payment result;

                try {
                    switch (transactionType) {
                        case AUTHORIZE:
                        case PURCHASE:
                        case CREDIT:
                            result = client.createPayment(resolverResp.getAccountIdMapping(), resolverResp.getPaymentMethodIdMapping(), transaction, paymentConfig.getControlPlugins(), ConverterHelper.convertToClientMapPluginProperties(properties, internalPaymentMethodIdProperty), requestOptions);
                            break;

                        case REFUND:
                            result = client.refundPayment(transaction, paymentConfig.getControlPlugins(), ConverterHelper.convertToClientMapPluginProperties(properties, internalPaymentMethodIdProperty), requestOptions);
                            break;

                        case VOID:
                            result = client.voidPayment(resolverResp.getPaymentMethodIdMapping(), payment.getExternalKey(), paymentTransaction.getExternalKey(), paymentConfig.getControlPlugins(), ConverterHelper.convertToClientMapPluginProperties(properties, internalPaymentMethodIdProperty), requestOptions);
                            break;

                        case CAPTURE:
                            result = client.captureAuthorization(transaction, paymentConfig.getControlPlugins(), ConverterHelper.convertToClientMapPluginProperties(properties, internalPaymentMethodIdProperty), requestOptions);
                            break;

                        default:
                            throw new IllegalStateException("Unexpected transaction type " + transactionType);
                    }

                    // Filter the transaction associated with this operation
                    final Optional<org.killbill.billing.client.model.PaymentTransaction> optionalTargetTransaction = ConverterHelper.getTransactionMatchOrLast(result.getTransactions(), paymentTransaction.getExternalKey());
                    Preconditions.checkState(optionalTargetTransaction.isPresent(), String.format("Cannot find the matching transaction for payment='%s', kbTransactionId='%s'", kbPaymentId, kbTransactionId));
                    return optionalTargetTransaction.get();
                } catch (final KillBillClientException e) {
                    if (e.getBillingException() != null && e.getBillingException().getCode() == ErrorCode.PAYMENT_PLUGIN_API_ABORTED.getCode()) {
                        return new org.killbill.billing.client.model.PaymentTransaction(transaction.getTransactionId(),
                                                                                        transaction.getTransactionExternalKey(),
                                                                                        transaction.getPaymentId(),
                                                                                        transaction.getPaymentExternalKey(),
                                                                                        transaction.getTransactionType(),
                                                                                        transaction.getAmount(),
                                                                                        transaction.getCurrency(),
                                                                                        null,
                                                                                        BigDecimal.ZERO,
                                                                                        transaction.getCurrency(),
                                                                                        TransactionStatus.PLUGIN_FAILURE.name(),
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        ImmutableList.<org.killbill.billing.client.model.PluginProperty>of(),

                                                                                        ImmutableList.<org.killbill.billing.client.model.AuditLog>of());
                    } else {
                        throw e;
                    }
                }
            }
        };

        try {
            final PaymentTransactionInfoPluginResultConverter converter = new PaymentTransactionInfoPluginResultConverter(payment);
            final PaymentTransactionInfoPlugin result = internalGenericPaymentTransactionOperation(op, converter, context.getTenantId(), converter.convertModelToApi(new org.killbill.billing.client.model.PaymentTransaction()));

            logService.log(LogService.LOG_INFO, String.format("Bridge Payment EXITING: Success running transactionType='%s', kbAccountId='%s', kbPaymentId='%s', kbPaymentMethodId='%s', amount='%s', currency='%s'",
                                                              transactionType, kbAccountId, kbPaymentId, kbPaymentMethodId, amount, currency));

            return result;
        } catch (final PaymentPluginApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Bridge Payment EXITING: Failed to run transactionType='%s', kbAccountId='%s', kbPaymentId='%s', kbPaymentMethodId='%s', amount='%s', currency='%s'",
                                                                 transactionType, kbAccountId, kbPaymentId, kbPaymentMethodId, amount, currency), e);
            throw e;
        }
    }

    private <R, CR> CR internalGenericPaymentTransactionOperation(final ClientOperation<R> op, final ResultConverter<R, CR> converter, final UUID tenantId, final CR defaultValue) throws PaymentPluginApiException {

        KillBillClient client = null;
        try {
            // Handle (generic) case where client is not configured
            client = configurationHandler.getConfigurable(tenantId);
            if (client == null) {
                return defaultValue;
            }
            final R result = op.doOperation(client, DEFAULT_OPTIONS);
            return converter != null ? converter.convertModelToApi(result) : null;

        } catch (final KillBillClientException e) { // When calling killbill client directly
            throw new PaymentBridgePluginApiException(e, op.getKbAccountId(), op.getKbPaymentId(), op.getKbPaymentMethodId(), op.getTransactionType());
        } catch (final WrappedKillBillClientException e) { // When going through resolver where java 8 stream api mask checked exceptions
            if (e.getCause() instanceof KillBillClientException) {
                throw new PaymentBridgePluginApiException((KillBillClientException) e.getCause(), op.getKbAccountId(), op.getKbPaymentId(), op.getKbPaymentMethodId(), op.getTransactionType());
            } else {
                // Should never happen
                throw e;
            }
        } catch (final WrappedUnresolvedException e) { // When going through resolver where java 8 stream api mask checked exceptions
            throw new PaymentBridgePluginApiException(e.getMessage(), op.getKbAccountId(), op.getKbPaymentId(), op.getKbPaymentMethodId(), op.getTransactionType());
        } catch (UnresolvedException e) { // When calling killbill client directly
            throw new PaymentBridgePluginApiException(e.getMessage(), op.getKbAccountId(), op.getKbPaymentId(), op.getKbPaymentMethodId(), op.getTransactionType());
        }  finally {
            if (client != null) {
                client.close();
            }
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

        public abstract R doOperation(final KillBillClient client, final RequestOptions requestOptions) throws KillBillClientException, PaymentPluginApiException, UnresolvedException;

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
