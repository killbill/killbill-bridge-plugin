/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.bridge.BridgeActivator;
import org.killbill.billing.plugin.bridge.KillbillClientConfigurationHandler;
import org.killbill.billing.plugin.bridge.PaymentConfigurationHandler;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.collect.ImmutableList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class TestBridgePaymentPluginApi {

    private static final String DEFAULT_WIREMOCK_CONFIG = "local: !!org.killbill.billing.plugin.bridge.BridgeConfig\n" +
                                                          "  killbillClientConfig:\n" +
                                                          "    username: admin\n" +
                                                          "    password: password\n" +
                                                          "    apiKey: bob\n" +
                                                          "    apiSecret: lazar\n" +
                                                          "    serverUrl: " + WireMockHelper.wireMockUri() + "\n" +
                                                          "  paymentConfig:\n" +
                                                          "    proxyModel: PROXY_SIMPLE\n" +
                                                          "    internalPaymentMethodIdName: paymentInstrumentId\n";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Account account;
    private OSGIKillbillAPI killbillAPI;
    private BridgePaymentPluginApi pluginApi;
    private PaymentMethod paymentMethod;
    private Payment payment;
    private CallContext callContext;

    private static int findFreePort() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final int freePort = serverSocket.getLocalPort();
        serverSocket.close();
        return freePort;
    }

    @BeforeMethod(groups = "slow")
    public void setUp() throws AccountApiException, PaymentApiException {
        account = TestUtils.buildAccount(Currency.USD, "US");
        killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final OSGIKillbillLogService logService = TestUtils.buildLogService();
        final String region = "local";
        final KillbillClientConfigurationHandler configurationHandler = new KillbillClientConfigurationHandlerForTestBridgePaymentPluginApi(BridgeActivator.PLUGIN_NAME,
                                                                                                                                            killbillAPI,
                                                                                                                                            logService,
                                                                                                                                            region);
        final PaymentConfigurationHandler paymentConfigurationHandler = new PaymentConfigurationHandlerForTestBridgePaymentPluginApi(BridgeActivator.PLUGIN_NAME,
                                                                                                                                     killbillAPI,
                                                                                                                                     logService,
                                                                                                                                     region);
        pluginApi = new BridgePaymentPluginApi(killbillAPI,
                                               logService,
                                               configurationHandler,
                                               paymentConfigurationHandler);

        final UUID paymentMethodId = account.getPaymentMethodId();
        paymentMethod = TestUtils.buildPaymentMethod(account.getId(), paymentMethodId, BridgeActivator.PLUGIN_NAME, killbillAPI);
        Mockito.when(killbillAPI.getPaymentApi().getPaymentMethodById(Mockito.eq(paymentMethodId), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<TenantContext>any())).thenReturn(paymentMethod);

        payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency(), killbillAPI);
        callContext = new PluginCallContext(BridgeActivator.PLUGIN_NAME, new DateTime(), account.getId(), UUID.randomUUID());
    }

    @Test(groups = "slow")
    public void testWithAbortedPayment() throws Exception {
        final PaymentTransactionInfoPlugin result = WireMockHelper.doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws PaymentPluginApiException, JsonProcessingException {
                stubFor(get(urlPathEqualTo("/1.0/kb/accounts")).willReturn(aResponse().withBody(OBJECT_MAPPER.writeValueAsBytes(new org.killbill.billing.client.model.Account(account.getId(),
                                                                                                                                                                              account.getName(),
                                                                                                                                                                              account.getFirstNameLength(),
                                                                                                                                                                              account.getExternalKey(),
                                                                                                                                                                              account.getEmail(),
                                                                                                                                                                              account.getBillCycleDayLocal(),
                                                                                                                                                                              account.getCurrency().toString(),
                                                                                                                                                                              account.getParentAccountId(),
                                                                                                                                                                              account.isPaymentDelegatedToParent(),
                                                                                                                                                                              account.getPaymentMethodId(),
                                                                                                                                                                              account.getTimeZone().toString(),
                                                                                                                                                                              account.getAddress1(),
                                                                                                                                                                              account.getAddress2(),
                                                                                                                                                                              account.getPostalCode(),
                                                                                                                                                                              account.getCompanyName(),
                                                                                                                                                                              account.getCity(),
                                                                                                                                                                              account.getStateOrProvince(),
                                                                                                                                                                              account.getCountry(),
                                                                                                                                                                              account.getLocale(),
                                                                                                                                                                              account.getPhone(),
                                                                                                                                                                              account.getNotes(),
                                                                                                                                                                              account.isMigrated(),
                                                                                                                                                                              false,
                                                                                                                                                                              null,
                                                                                                                                                                              null)))
                                                                                      .withStatus(200)));

                // Aborted payment
                stubFor(post(urlPathEqualTo("/1.0/kb/accounts/" + account.getId() + "/payments")).willReturn(aResponse().withBody("{\"code\":7106}").withStatus(422)));

                final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, Currency.USD);

                return pluginApi.authorizePayment(account.getId(),
                                                  payment.getId(),
                                                  authorizationTransaction.getId(),
                                                  paymentMethod.getId(),
                                                  authorizationTransaction.getAmount(),
                                                  authorizationTransaction.getCurrency(),
                                                  ImmutableList.<PluginProperty>of(),
                                                  callContext);
            }
        });
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
    }

    @Test(groups = "slow")
    public void testGetSuccessfulPayment() throws Exception {
        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, Currency.USD);

        final List<PaymentTransactionInfoPlugin> result = WireMockHelper.doWithWireMock(new WithWireMock<List<PaymentTransactionInfoPlugin>>() {
            @Override
            public List<PaymentTransactionInfoPlugin> execute(final WireMockServer server) throws PaymentPluginApiException {
                // External keys match, but not the ID
                final String paymentId = UUID.randomUUID().toString();
                stubFor(get(urlPathEqualTo("/1.0/kb/payments")).willReturn(aResponse().withBody("{\"accountId\":\"" + UUID.randomUUID().toString() + "\"," +
                                                                                                "\"paymentId\":\"" + paymentId + "\"," +
                                                                                                "\"paymentExternalKey\":\"" + payment.getExternalKey() + "\"," +
                                                                                                "\"currency\":\"USD\"," +
                                                                                                "\"paymentMethodId\":\"" + UUID.randomUUID().toString() + "\"," +
                                                                                                "\"transactions\":[" +
                                                                                                "{\"transactionId\":\"" + UUID.randomUUID().toString() + "\"," +
                                                                                                "\"transactionExternalKey\":\"" + purchaseTransaction.getExternalKey() + "\"," +
                                                                                                "\"paymentId\":\"" + paymentId + "\"," +
                                                                                                "\"paymentExternalKey\":\"" + payment.getExternalKey() + "\"," +
                                                                                                "\"transactionType\":\"PURCHASE\"," +
                                                                                                "\"amount\":10.00," +
                                                                                                "\"currency\":\"USD\"," +
                                                                                                "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                                                                "\"processedAmount\":10.00," +
                                                                                                "\"processedCurrency\":\"USD\"," +
                                                                                                "\"status\":\"SUCCESS\"}]}")
                                                                                      .withStatus(200)));

                return pluginApi.getPaymentInfo(account.getId(),
                                                payment.getId(),
                                                ImmutableList.<PluginProperty>of(),
                                                callContext);
            }
        });
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(0).getKbTransactionPaymentId(), purchaseTransaction.getId());
        Assert.assertEquals(result.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.get(0).getAmount().compareTo(BigDecimal.TEN), 0);
    }

    @Test(groups = "slow")
    public void testGetForJanitor() throws Exception {
        final UUID kbSPaymentId = payment.getId();
        final String kbSPaymentExternalKey = payment.getExternalKey();

        final String kbSPurchaseTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentTransaction purchase1Transaction = TestUtils.buildPaymentTransaction(payment, kbSPurchaseTransactionExternalKey, TransactionType.PURCHASE, TransactionStatus.PAYMENT_FAILURE, BigDecimal.TEN, Currency.USD);
        final PaymentTransaction purchase2Transaction = TestUtils.buildPaymentTransaction(payment, kbSPurchaseTransactionExternalKey, TransactionType.PURCHASE, TransactionStatus.UNKNOWN, BigDecimal.TEN, Currency.USD);

        final String kbSRefundTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentTransaction refund1Transaction = TestUtils.buildPaymentTransaction(payment, kbSRefundTransactionExternalKey, TransactionType.REFUND, TransactionStatus.PLUGIN_FAILURE, BigDecimal.TEN, Currency.USD);
        final PaymentTransaction refund2Transaction = TestUtils.buildPaymentTransaction(payment, kbSRefundTransactionExternalKey, TransactionType.REFUND, TransactionStatus.PLUGIN_FAILURE, BigDecimal.TEN, Currency.USD);
        final PaymentTransaction refund3Transaction = TestUtils.buildPaymentTransaction(payment, kbSRefundTransactionExternalKey, TransactionType.REFUND, TransactionStatus.UNKNOWN, BigDecimal.TEN, Currency.USD);

        final String kbSChargebackTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentTransaction chargeback1Transaction = TestUtils.buildPaymentTransaction(payment, kbSChargebackTransactionExternalKey, TransactionType.CHARGEBACK, TransactionStatus.UNKNOWN, BigDecimal.TEN, Currency.USD);

        // External keys match, but not the ID
        final UUID kbPAccountId = UUID.randomUUID();
        final UUID kbPPaymentId = UUID.randomUUID();
        final UUID kbPPurchase1TransactionId = UUID.randomUUID();
        final UUID kbPPurchase2TransactionId = UUID.randomUUID();
        final UUID kbPRefund1TransactionId = UUID.randomUUID();
        final UUID kbPRefund2TransactionId = UUID.randomUUID();
        final UUID kbPRefund3TransactionId = UUID.randomUUID();
        final UUID kbPChargeback1TransactionId = UUID.randomUUID();
        final UUID kbPPaymentMethodId = UUID.randomUUID();

        final String purchase1TransactionResponse = "{\"transactionId\":\"" + kbPPurchase1TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-10T15:47:00.000Z\"," +
                                                    "\"processedAmount\":0.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"PAYMENT_FAILURE\"}";
        final String purchase2TransactionResponse = "{\"transactionId\":\"" + kbPPurchase2TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                    "\"processedAmount\":10.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"SUCCESS\"}";
        final String refund1TransactionResponse = "{\"transactionId\":\"" + kbPRefund1TransactionId + "\"," +
                                                  "\"transactionExternalKey\":\"" + kbSRefundTransactionExternalKey + "\"," +
                                                  "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                  "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                  "\"transactionType\":\"REFUND\"," +
                                                  "\"amount\":10.00," +
                                                  "\"currency\":\"USD\"," +
                                                  "\"effectiveDate\":\"2018-06-10T15:47:00.000Z\"," +
                                                  "\"processedAmount\":0.00," +
                                                  "\"processedCurrency\":\"USD\"," +
                                                  "\"status\":\"PLUGIN_FAILURE\"}";
        final String refund2TransactionResponse = "{\"transactionId\":\"" + kbPRefund2TransactionId + "\"," +
                                                  "\"transactionExternalKey\":\"" + kbSRefundTransactionExternalKey + "\"," +
                                                  "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                  "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                  "\"transactionType\":\"REFUND\"," +
                                                  "\"amount\":10.00," +
                                                  "\"currency\":\"USD\"," +
                                                  "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                  "\"processedAmount\":10.00," +
                                                  "\"processedCurrency\":\"USD\"," +
                                                  "\"status\":\"PLUGIN_FAILURE\"}";
        final String refund3TransactionResponse = "{\"transactionId\":\"" + kbPRefund3TransactionId + "\"," +
                                                  "\"transactionExternalKey\":\"" + kbSRefundTransactionExternalKey + "\"," +
                                                  "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                  "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                  "\"transactionType\":\"REFUND\"," +
                                                  "\"amount\":10.00," +
                                                  "\"currency\":\"USD\"," +
                                                  "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                  "\"processedAmount\":10.00," +
                                                  "\"processedCurrency\":\"USD\"," +
                                                  "\"status\":\"PAYMENT_FAILURE\"}";
        final String chargeback1TransactionResponse = "{\"transactionId\":\"" + kbPChargeback1TransactionId + "\"," +
                                                      "\"transactionExternalKey\":\"" + kbSChargebackTransactionExternalKey + "\"," +
                                                      "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                      "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                      "\"transactionType\":\"CHARGEBACK\"," +
                                                      "\"amount\":10.00," +
                                                      "\"currency\":\"USD\"," +
                                                      "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                      "\"processedAmount\":10.00," +
                                                      "\"processedCurrency\":\"USD\"," +
                                                      "\"status\":\"SUCCESS\"}";
        final String paymentResponse = "{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                       "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                       "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                       "\"currency\":\"USD\"," +
                                       "\"paymentMethodId\":\"" + kbPPaymentMethodId + "\"," +
                                       "\"transactions\":";
        final String paymentWithTransactionsResponse = paymentResponse + "[" +
                                                       purchase1TransactionResponse + "," +
                                                       purchase2TransactionResponse + "," +
                                                       refund1TransactionResponse + "," +
                                                       refund2TransactionResponse + "," +
                                                       refund3TransactionResponse + "," +
                                                       chargeback1TransactionResponse + "]}";

        final List<PaymentTransactionInfoPlugin> result = WireMockHelper.doWithWireMock(new WithWireMock<List<PaymentTransactionInfoPlugin>>() {
            @Override
            public List<PaymentTransactionInfoPlugin> execute(final WireMockServer server) throws PaymentPluginApiException {
                stubFor(get(urlPathEqualTo("/1.0/kb/accounts"))
                                .willReturn(aResponse().withBody("{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                                                 "\"externalKey\":\"" + account.getExternalKey() + "\"}")
                                                       .withStatus(200)));

                // GET requests
                stubFor(get(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId))
                                .willReturn(aResponse().withBody(paymentWithTransactionsResponse).withStatus(200)));
                stubFor(get(urlPathEqualTo("/1.0/kb/payments"))
                                .willReturn(aResponse().withBody(paymentWithTransactionsResponse).withStatus(200)));

                return pluginApi.getPaymentInfo(account.getId(),
                                                kbSPaymentId,
                                                ImmutableList.<PluginProperty>of(),
                                                callContext);
            }
        });
        Assert.assertEquals(result.size(), 6);
        Assert.assertEquals(result.get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(result.get(0).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(0).getKbTransactionPaymentId(), purchase1Transaction.getId());
        Assert.assertEquals(result.get(0).getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.get(1).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(result.get(1).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(1).getKbTransactionPaymentId(), purchase2Transaction.getId());
        Assert.assertEquals(result.get(1).getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(result.get(2).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(2).getKbTransactionPaymentId(), refund1Transaction.getId());
        Assert.assertEquals(result.get(2).getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.get(3).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(result.get(3).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(3).getKbTransactionPaymentId(), refund2Transaction.getId());
        Assert.assertEquals(result.get(3).getStatus(), PaymentPluginStatus.CANCELED);
        Assert.assertEquals(result.get(4).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(result.get(4).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(4).getKbTransactionPaymentId(), refund3Transaction.getId());
        Assert.assertEquals(result.get(4).getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.get(5).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(result.get(5).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(5).getKbTransactionPaymentId(), chargeback1Transaction.getId());
        Assert.assertEquals(result.get(5).getStatus(), PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "slow")
    public void testGetForJanitorWithDataIntegrityIssue() throws Exception {
        final UUID kbSPaymentId = payment.getId();
        final String kbSPaymentExternalKey = payment.getExternalKey();

        final String kbSPurchaseTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentTransaction purchase1Transaction = TestUtils.buildPaymentTransaction(payment, kbSPurchaseTransactionExternalKey, TransactionType.PURCHASE, TransactionStatus.PAYMENT_FAILURE, BigDecimal.TEN, Currency.USD);
        final PaymentTransaction purchase2Transaction = TestUtils.buildPaymentTransaction(payment, kbSPurchaseTransactionExternalKey, TransactionType.PURCHASE, TransactionStatus.PAYMENT_FAILURE, BigDecimal.TEN, Currency.USD);
        final PaymentTransaction purchase3Transaction = TestUtils.buildPaymentTransaction(payment, kbSPurchaseTransactionExternalKey, TransactionType.PURCHASE, TransactionStatus.UNKNOWN, BigDecimal.TEN, Currency.USD);

        // External keys match, but not the ID
        final UUID kbPAccountId = UUID.randomUUID();
        final UUID kbPPaymentId = UUID.randomUUID();
        final UUID kbPPurchase1TransactionId = UUID.randomUUID();
        final UUID kbPPurchase2TransactionId = UUID.randomUUID();
        final UUID kbPPaymentMethodId = UUID.randomUUID();

        final String purchase1TransactionResponse = "{\"transactionId\":\"" + kbPPurchase1TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-10T15:47:00.000Z\"," +
                                                    "\"processedAmount\":0.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"PAYMENT_FAILURE\"}";
        final String purchase2TransactionResponse = "{\"transactionId\":\"" + kbPPurchase2TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                    "\"processedAmount\":10.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"SUCCESS\"}";
        final String paymentResponse = "{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                       "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                       "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                       "\"currency\":\"USD\"," +
                                       "\"paymentMethodId\":\"" + kbPPaymentMethodId + "\"," +
                                       "\"transactions\":";
        final String paymentWithTransactionsResponse = paymentResponse + "[" +
                                                       purchase1TransactionResponse + "," +
                                                       purchase2TransactionResponse + "]}";

        final List<PaymentTransactionInfoPlugin> result = WireMockHelper.doWithWireMock(new WithWireMock<List<PaymentTransactionInfoPlugin>>() {
            @Override
            public List<PaymentTransactionInfoPlugin> execute(final WireMockServer server) throws PaymentPluginApiException {
                stubFor(get(urlPathEqualTo("/1.0/kb/accounts"))
                                .willReturn(aResponse().withBody("{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                                                 "\"externalKey\":\"" + account.getExternalKey() + "\"}")
                                                       .withStatus(200)));

                // GET requests
                stubFor(get(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId))
                                .willReturn(aResponse().withBody(paymentWithTransactionsResponse).withStatus(200)));
                stubFor(get(urlPathEqualTo("/1.0/kb/payments"))
                                .willReturn(aResponse().withBody(paymentWithTransactionsResponse).withStatus(200)));

                return pluginApi.getPaymentInfo(account.getId(),
                                                kbSPaymentId,
                                                ImmutableList.<PluginProperty>of(),
                                                callContext);
            }
        });
        Assert.assertEquals(result.size(), 3);
        Assert.assertEquals(result.get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(result.get(0).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(0).getKbTransactionPaymentId(), purchase1Transaction.getId());
        Assert.assertEquals(result.get(0).getStatus(), PaymentPluginStatus.ERROR);
        Assert.assertEquals(result.get(1).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(result.get(1).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(1).getKbTransactionPaymentId(), purchase2Transaction.getId());
        Assert.assertEquals(result.get(1).getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.get(2).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(result.get(2).getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.get(2).getKbTransactionPaymentId(), purchase3Transaction.getId());
        Assert.assertEquals(result.get(2).getStatus(), PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "slow")
    public void testGetSuccessfulPaymentAfterRetry() throws Exception {
        final UUID kbSPaymentId = payment.getId();
        final String kbSPaymentExternalKey = payment.getExternalKey();
        final UUID kbSPaymentMethodId = paymentMethod.getId();

        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, Currency.USD);
        final String kbSPurchaseTransactionExternalKey = purchaseTransaction.getExternalKey();
        final UUID kbSPurchaseTransactionId = purchaseTransaction.getId();

        // External keys match, but not the ID
        final UUID kbPAccountId = UUID.randomUUID();
        final UUID kbPPaymentId = UUID.randomUUID();
        final UUID kbPPurchase1TransactionId = UUID.randomUUID();
        final UUID kbPPurchase2TransactionId = UUID.randomUUID();
        final UUID kbPPaymentMethodId = UUID.randomUUID();

        final String purchase1TransactionResponse = "{\"transactionId\":\"" + kbPPurchase1TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-10T15:47:00.000Z\"," +
                                                    "\"processedAmount\":0.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"PAYMENT_FAILURE\"}";
        final String purchase2TransactionResponse = "{\"transactionId\":\"" + kbPPurchase2TransactionId + "\"," +
                                                    "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                    "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                    "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                    "\"transactionType\":\"PURCHASE\"," +
                                                    "\"amount\":10.00," +
                                                    "\"currency\":\"USD\"," +
                                                    "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                    "\"processedAmount\":10.00," +
                                                    "\"processedCurrency\":\"USD\"," +
                                                    "\"status\":\"SUCCESS\"}";
        final String paymentResponse = "{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                       "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                       "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                       "\"currency\":\"USD\"," +
                                       "\"paymentMethodId\":\"" + kbPPaymentMethodId + "\"," +
                                       "\"transactions\":";
        final String paymentWithPurchase2Response = paymentResponse + "[" +
                                                    purchase1TransactionResponse + "," +
                                                    purchase2TransactionResponse + "]}";

        final PaymentTransactionInfoPlugin result = WireMockHelper.doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws PaymentPluginApiException {
                stubFor(get(urlPathEqualTo("/1.0/kb/accounts"))
                                .inScenario("PURCHASE")
                                .willReturn(aResponse().withBody("{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                                                 "\"externalKey\":\"" + account.getExternalKey() + "\"}")
                                                       .withStatus(200)));

                // GET requests post purchase
                stubFor(get(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId))
                                .inScenario("PURCHASE")
                                .whenScenarioStateIs("Purchased")
                                .willReturn(aResponse().withBody(paymentWithPurchase2Response).withStatus(200)));
                stubFor(get(urlPathEqualTo("/1.0/kb/payments"))
                                .inScenario("PURCHASE")
                                .whenScenarioStateIs("Purchased")
                                .willReturn(aResponse().withBody(paymentWithPurchase2Response).withStatus(200)));

                // Purchase request
                stubFor(post(urlPathEqualTo("/1.0/kb/accounts/" + kbPAccountId + "/payments"))
                                .inScenario("PURCHASE")
                                .whenScenarioStateIs(STARTED)
                                .willReturn(aResponse()
                                                    .withHeader("Location", "/1.0/kb/payments/" + kbPPaymentId)
                                                    .withStatus(201))
                                .willSetStateTo("Purchased"));

                return pluginApi.purchasePayment(account.getId(),
                                                 kbSPaymentId,
                                                 kbSPurchaseTransactionId,
                                                 kbSPaymentMethodId,
                                                 BigDecimal.TEN,
                                                 Currency.USD,
                                                 ImmutableList.<PluginProperty>of(),
                                                 callContext);
            }
        });
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getKbPaymentId(), payment.getId());
        Assert.assertEquals(result.getKbTransactionPaymentId(), purchaseTransaction.getId());
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getAmount().compareTo(BigDecimal.TEN), 0);
    }

    @Test(groups = "slow")
    public void testRefund() throws Exception {
        final UUID kbSPaymentId = payment.getId();
        final String kbSPaymentExternalKey = payment.getExternalKey();
        final UUID kbSPaymentMethodId = paymentMethod.getId();

        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, Currency.USD);
        final String kbSPurchaseTransactionExternalKey = purchaseTransaction.getExternalKey();

        final PaymentTransaction refundTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.REFUND, BigDecimal.TEN, Currency.USD);
        final UUID kbSRefundTransactionId = refundTransaction.getId();
        final String kbSRefundTransactionExternalKey = refundTransaction.getExternalKey();

        // External keys match, but not the ID
        final UUID kbPAccountId = UUID.randomUUID();
        final UUID kbPPaymentId = UUID.randomUUID();
        final UUID kbPPurchaseTransactionId = UUID.randomUUID();
        final UUID kbPRefundTransactionId = UUID.randomUUID();
        final UUID kbPPaymentMethodId = UUID.randomUUID();

        final String purchaseTransactionResponse = "{\"transactionId\":\"" + kbPPurchaseTransactionId + "\"," +
                                                   "\"transactionExternalKey\":\"" + kbSPurchaseTransactionExternalKey + "\"," +
                                                   "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                   "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                   "\"transactionType\":\"PURCHASE\"," +
                                                   "\"amount\":10.00," +
                                                   "\"currency\":\"USD\"," +
                                                   "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                   "\"processedAmount\":10.00," +
                                                   "\"processedCurrency\":\"USD\"," +
                                                   "\"status\":\"SUCCESS\"}";
        final String refundTransactionResponse = "{\"transactionId\":\"" + kbPRefundTransactionId + "\"," +
                                                 "\"transactionExternalKey\":\"" + kbSRefundTransactionExternalKey + "\"," +
                                                 "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                                 "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                                 "\"transactionType\":\"REFUND\"," +
                                                 "\"amount\":10.00," +
                                                 "\"currency\":\"USD\"," +
                                                 "\"effectiveDate\":\"2018-06-11T15:47:00.000Z\"," +
                                                 "\"processedAmount\":10.00," +
                                                 "\"processedCurrency\":\"USD\"," +
                                                 "\"status\":\"SUCCESS\"}";
        final String paymentResponse = "{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                       "\"paymentId\":\"" + kbPPaymentId + "\"," +
                                       "\"paymentExternalKey\":\"" + kbSPaymentExternalKey + "\"," +
                                       "\"currency\":\"USD\"," +
                                       "\"paymentMethodId\":\"" + kbPPaymentMethodId + "\"," +
                                       "\"transactions\":";
        final String paymentWithPurchaseResponse = paymentResponse + "[" +
                                                   purchaseTransactionResponse + "]}";
        final String paymentWithPurchaseAndRefundResponse = paymentResponse + "[" +
                                                            purchaseTransactionResponse + "," +
                                                            refundTransactionResponse + "]}";

        final PaymentTransactionInfoPlugin result = WireMockHelper.doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws PaymentPluginApiException {
                stubFor(get(urlPathEqualTo("/1.0/kb/accounts"))
                                .inScenario("REFUND")
                                .willReturn(aResponse().withBody("{\"accountId\":\"" + kbPAccountId.toString() + "\"," +
                                                                 "\"externalKey\":\"" + account.getExternalKey() + "\"}")
                                                       .withStatus(200)));

                // Requests pre-refund
                stubFor(get(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId))
                                .inScenario("REFUND")
                                .whenScenarioStateIs(STARTED)
                                .willReturn(aResponse().withBody(paymentWithPurchaseResponse).withStatus(200)));
                stubFor(get(urlPathEqualTo("/1.0/kb/payments"))
                                .inScenario("REFUND")
                                .whenScenarioStateIs(STARTED)
                                .willReturn(aResponse().withBody(paymentWithPurchaseResponse).withStatus(200)));

                // Requests post-refund
                stubFor(get(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId))
                                .inScenario("REFUND")
                                .whenScenarioStateIs("Refunded")
                                .willReturn(aResponse().withBody(paymentWithPurchaseAndRefundResponse).withStatus(200)));
                stubFor(get(urlPathEqualTo("/1.0/kb/payments"))
                                .inScenario("REFUND")
                                .whenScenarioStateIs("Refunded")
                                .willReturn(aResponse().withBody(paymentWithPurchaseAndRefundResponse).withStatus(200)));

                // Refund request
                stubFor(post(urlPathEqualTo("/1.0/kb/payments/" + kbPPaymentId + "/refunds"))
                                .inScenario("REFUND")
                                .whenScenarioStateIs(STARTED)
                                .willReturn(aResponse()
                                                    .withHeader("Location", "/1.0/kb/payments/" + kbPPaymentId)
                                                    .withStatus(201))
                                .willSetStateTo("Refunded"));

                return pluginApi.refundPayment(account.getId(),
                                               kbSPaymentId,
                                               kbSRefundTransactionId,
                                               kbSPaymentMethodId,
                                               BigDecimal.TEN,
                                               Currency.USD,
                                               ImmutableList.<PluginProperty>of(),
                                               callContext);
            }
        });
        Assert.assertEquals(result.getStatus(), PaymentPluginStatus.PROCESSED);
        Assert.assertEquals(result.getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(result.getKbTransactionPaymentId(), kbSRefundTransactionId);
        Assert.assertEquals(result.getKbPaymentId(), kbSPaymentId);
    }

    private interface WithWireMock<T> {

        T execute(WireMockServer server) throws Exception;
    }

    private static final class PaymentConfigurationHandlerForTestBridgePaymentPluginApi extends PaymentConfigurationHandler {

        public PaymentConfigurationHandlerForTestBridgePaymentPluginApi(final String pluginName,
                                                                        final OSGIKillbillAPI osgiKillbillAPI,
                                                                        final OSGIKillbillLogService osgiKillbillLogService,
                                                                        final String region) {
            super(pluginName, osgiKillbillAPI, osgiKillbillLogService, region);
        }

        @Override
        public String getTenantConfigurationAsString(@Nullable final UUID kbTenantId) {
            return DEFAULT_WIREMOCK_CONFIG;
        }
    }

    private static final class KillbillClientConfigurationHandlerForTestBridgePaymentPluginApi extends KillbillClientConfigurationHandler {

        public KillbillClientConfigurationHandlerForTestBridgePaymentPluginApi(final String pluginName,
                                                                               final OSGIKillbillAPI osgiKillbillAPI,
                                                                               final OSGIKillbillLogService osgiKillbillLogService,
                                                                               final String region) {
            super(pluginName, osgiKillbillAPI, osgiKillbillLogService, region);
        }

        @Override
        public String getTenantConfigurationAsString(@Nullable final UUID kbTenantId) {
            return DEFAULT_WIREMOCK_CONFIG;
        }
    }

    static class WireMockHelper {

        private static final WireMockHelper INSTANCE = new WireMockHelper();
        private int freePort = -1;

        public static WireMockHelper instance() {
            return INSTANCE;
        }

        public static String wireMockUri() {
            try {
                return "http://localhost:" + WireMockHelper.instance().getFreePort();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static <T> T doWithWireMock(final WithWireMock<T> command) throws Exception {
            final int wireMockPort = WireMockHelper.instance().getFreePort();
            final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(wireMockPort));
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockPort);
            try {
                return command.execute(wireMockServer);
            } finally {
                wireMockServer.shutdown();
                while (wireMockServer.isRunning()) {
                    Thread.sleep(1);
                }
            }
        }

        private synchronized int getFreePort() throws IOException {
            if (freePort == -1) {
                freePort = findFreePort();
            }
            return freePort;
        }
    }
}
