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

package org.killbill.billing.plugin.bridge.api.resolver.local;

import com.google.common.collect.ImmutableList;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.util.callcontext.TenantContext;

import java.util.UUID;

public class LocalResolver {

    private final TenantContext tenantContext;
    private final OSGIKillbillAPI killbillAPI;

    public LocalResolver(final OSGIKillbillAPI killbillAPI, final TenantContext tenantContext) {
        this.tenantContext = tenantContext;
        this.killbillAPI = killbillAPI;
    }

    public String getPaymentMethodExternalKey(final UUID kbPaymentMethodId) throws PaymentPluginApiException {
        try {
            final PaymentMethod paymentMethod = killbillAPI.getPaymentApi().getPaymentMethodById(kbPaymentMethodId, false, false, ImmutableList.of(), tenantContext);
            return paymentMethod.getExternalKey();
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve payment method %s", kbPaymentMethodId), e);
        }
    }

    public Account getAccount(final UUID kbAccountId) throws PaymentPluginApiException {
        try {
            final Account account = killbillAPI.getAccountUserApi().getAccountById(kbAccountId, tenantContext);
            return account;
        } catch (final AccountApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve account %s", kbAccountId), e);
        }
    }


    public String getAccountExternalKey(final UUID kbAccountId) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId);
        return account.getExternalKey();
    }

    public Payment getPayment(final UUID kbPaymentId) throws PaymentPluginApiException {
        try {
            return killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, false, ImmutableList.of(), tenantContext);
        } catch (PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve payment %s", kbPaymentId), e);
        }
    }


    public String getPaymentExternalKey(final UUID kbPaymentId) throws PaymentPluginApiException {
        final Payment payment = getPayment(kbPaymentId);
        return payment.getExternalKey();
    }
}



