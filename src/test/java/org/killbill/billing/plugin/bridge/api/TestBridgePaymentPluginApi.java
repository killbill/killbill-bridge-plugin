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
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
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

    private static int findFreePort() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final int freePort = serverSocket.getLocalPort();
        serverSocket.close();
        return freePort;
    }

    @Test(groups = "slow")
    public void testWithAbortedPayment() throws Exception {
        final Account account = TestUtils.buildAccount(Currency.USD, "US");
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
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
        final BridgePaymentPluginApi pluginApi = new BridgePaymentPluginApi(killbillAPI,
                                                                            logService,
                                                                            configurationHandler,
                                                                            paymentConfigurationHandler);

        final UUID paymentMethodId = UUID.randomUUID();
        final PaymentMethod paymentMethod = TestUtils.buildPaymentMethod(account.getId(), paymentMethodId, BridgeActivator.PLUGIN_NAME, killbillAPI);
        Mockito.when(killbillAPI.getPaymentApi().getPaymentMethodById(Mockito.eq(paymentMethodId), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<TenantContext>any())).thenReturn(paymentMethod);

        final PaymentTransactionInfoPlugin result = WireMockHelper.doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws PaymentPluginApiException, PaymentApiException, JsonProcessingException {
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
                                                                                                                                                                              account.isNotifiedForInvoices(),
                                                                                                                                                                              null,
                                                                                                                                                                              null)))
                                                                                      .withStatus(200)));

                // Aborted payment
                stubFor(post(urlPathEqualTo("/1.0/kb/accounts/" + account.getId() + "/payments")).willReturn(aResponse().withBody("{\"code\":7106}").withStatus(422)));

                final Payment payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency(), killbillAPI);
                final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, Currency.USD);
                final CallContext callContext = new PluginCallContext(BridgeActivator.PLUGIN_NAME, new DateTime(), account.getId(), UUID.randomUUID());

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
