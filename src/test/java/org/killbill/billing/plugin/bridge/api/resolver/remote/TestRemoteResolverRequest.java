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

package org.killbill.billing.plugin.bridge.api.resolver.remote;

import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.model.Account;
import org.killbill.billing.client.model.Payment;
import org.killbill.billing.client.model.PaymentMethod;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolver.WrappedKillBillClientException;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolver.WrappedUnresolvedException;
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolverRequest.UnresolvedException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRemoteResolverRequest {

    @Test(groups = "fast")
    public void testResolveExistingAccount() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String accountExternalKey = "coin coin";
        final org.killbill.billing.account.api.Account srcAccount = createAccount(accountExternalKey, "Vatican", "it_IT", Currency.EUR);
        remoteResolverRequest.resolveAccount(srcAccount, true);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        final Account account = new Account();
        final UUID accountId = UUID.randomUUID();
        account.setAccountId(accountId);
        Mockito.when(client.getAccount(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(account);

        final RemoteResolver resolver = new RemoteResolver(client, null);
        final RemoteResolverResponse resolverResp = resolver.resolve(remoteResolverRequest);
        Assert.assertNotNull(resolverResp.getAccountIdMapping());
        Assert.assertEquals(resolverResp.getAccountIdMapping(), accountId);
    }

    @Test(groups = "fast")
    public void testCreateAndResoleAccount() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String accountExternalKey = "coin coin";
        final org.killbill.billing.account.api.Account srcAccount = createAccount(accountExternalKey, "Vatican", "it_IT", Currency.EUR);
        remoteResolverRequest.resolveAccount(srcAccount, true);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        Mockito.when(client.getAccount(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(null);

        final Account account = new Account();
        final UUID accountId = UUID.randomUUID();
        account.setAccountId(accountId);
        Mockito.when(client.createAccount(Mockito.any(), Mockito.<RequestOptions>any())).thenReturn(account);

        final RemoteResolver resolver = new RemoteResolver(client, null);
        final RemoteResolverResponse resolverResp = resolver.resolve(remoteResolverRequest);
        Assert.assertNotNull(resolverResp.getAccountIdMapping());
        Assert.assertEquals(resolverResp.getAccountIdMapping(), accountId);
    }

    @Test(groups = "fast")
    public void testResolveAccountWithKillBillClientException() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String accountExternalKey = "coin coin";
        final org.killbill.billing.account.api.Account srcAccount = createAccount(accountExternalKey, "Vatican", "it_IT", Currency.EUR);
        remoteResolverRequest.resolveAccount(srcAccount, true);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        Mockito.when(client.getAccount(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(null);

        final Account account = new Account();
        final UUID accountId = UUID.randomUUID();
        account.setAccountId(accountId);
        Mockito.when(client.createAccount(Mockito.any(), Mockito.<RequestOptions>any())).thenThrow(new KillBillClientException(new RuntimeException("NOT FOUND")));

        boolean gotKillBillClientException = false;
        final RemoteResolver resolver = new RemoteResolver(client, null);
        try {
            resolver.resolve(remoteResolverRequest);
            Assert.fail("Call should not succeed");
        } catch (final WrappedKillBillClientException e) {
            gotKillBillClientException = true;
            Assert.assertTrue(e.getCause() instanceof KillBillClientException);
        }
        Assert.assertTrue(gotKillBillClientException);
    }

    @Test(groups = "fast")
    public void testResolveExistingPaymentMethod() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String pmExternalKey = "couac couac";
        remoteResolverRequest.resolvePM(pmExternalKey);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        final PaymentMethod paymentMethod = new PaymentMethod();
        final UUID pmId = UUID.randomUUID();
        paymentMethod.setPaymentMethodId(pmId);
        Mockito.when(client.getPaymentMethodByKey(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(paymentMethod);

        final RemoteResolver resolver = new RemoteResolver(client, null);
        final RemoteResolverResponse resolverResp = resolver.resolve(remoteResolverRequest);
        Assert.assertNotNull(resolverResp.getPaymentMethodIdMapping());
        Assert.assertEquals(resolverResp.getPaymentMethodIdMapping(), pmId);
    }

    @Test(groups = "fast")
    public void testResolveMissingPaymentMethod() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String pmExternalKey = "couac couac";
        remoteResolverRequest.resolvePM(pmExternalKey);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        Mockito.when(client.getPaymentMethodByKey(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(null);

        boolean gotUnresolved = false;
        final RemoteResolver resolver = new RemoteResolver(client, null);
        try {
            resolver.resolve(remoteResolverRequest);
            Assert.fail("Call should not succeed");
        } catch (final WrappedUnresolvedException e) {
            gotUnresolved = true;
        }
        Assert.assertTrue(gotUnresolved);
    }

    @Test(groups = "fast")
    public void testResolveExistingPayment() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String paymentExternalKey = "foin foin";
        remoteResolverRequest.resolvePayment(paymentExternalKey);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        final Payment payment = new Payment();
        final UUID paymentId = UUID.randomUUID();
        payment.setPaymentId(paymentId);
        Mockito.when(client.getPaymentByExternalKey(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(payment);

        final RemoteResolver resolver = new RemoteResolver(client, null);
        final RemoteResolverResponse resolverResp = resolver.resolve(remoteResolverRequest);
        Assert.assertNotNull(resolverResp.getPaymentIdMapping());
        Assert.assertEquals(resolverResp.getPaymentIdMapping(), paymentId);
    }

    @Test(groups = "fast")
    public void testResolveUnresolvedPayment() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String paymentExternalKey = "foin foin";
        remoteResolverRequest.resolvePayment(paymentExternalKey);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        final Payment payment = new Payment();
        final UUID paymentId = UUID.randomUUID();
        payment.setPaymentId(paymentId);
        Mockito.when(client.getPaymentByExternalKey(Mockito.anyString(), Mockito.<RequestOptions>any())).thenReturn(null);

        boolean gotUnresolved = false;
        final RemoteResolver resolver = new RemoteResolver(client, null);
        try {
            resolver.resolve(remoteResolverRequest);
            Assert.fail("Call should not succeed");
        } catch (final WrappedUnresolvedException e) {
            gotUnresolved = true;
        }
        Assert.assertTrue(gotUnresolved);
    }

    @Test(groups = "fast")
    public void testResolveWithRuntimeException() throws KillBillClientException, UnresolvedException {

        final RemoteResolverRequest remoteResolverRequest = new RemoteResolverRequest();

        final String paymentExternalKey = "foin foin";
        remoteResolverRequest.resolvePayment(paymentExternalKey);

        final KillBillClient client = Mockito.mock(KillBillClient.class);
        final Payment payment = new Payment();
        final UUID paymentId = UUID.randomUUID();
        payment.setPaymentId(paymentId);
        Mockito.when(client.getPaymentByExternalKey(Mockito.anyString(), Mockito.<RequestOptions>any())).thenThrow(new RuntimeException("Ah ah !!!"));

        final RemoteResolver resolver = new RemoteResolver(client, null);
        try {
            resolver.resolve(remoteResolverRequest);
            Assert.fail("Call should not succeed");
        } catch (final RuntimeException e) {
        }
    }




    private org.killbill.billing.account.api.Account createAccount(final String externalKey, final String country, final String locale, final Currency currency) {
        final org.killbill.billing.account.api.Account result = Mockito.mock(org.killbill.billing.account.api.Account.class);
        Mockito.when(result.getExternalKey()).thenReturn(externalKey);
        Mockito.when(result.getCountry()).thenReturn(country);
        Mockito.when(result.getLocale()).thenReturn(locale);
        Mockito.when(result.getCurrency()).thenReturn(currency);
        return result;
    }

}