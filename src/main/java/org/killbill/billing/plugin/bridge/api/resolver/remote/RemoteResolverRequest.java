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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.model.Account;
import org.killbill.billing.client.model.Payment;
import org.killbill.billing.client.model.PaymentMethod;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.plugin.bridge.api.resolver.ResolvingType;

public class RemoteResolverRequest {

    private final static AtomicLong RemoteResolverRequestSeq = new AtomicLong();

    private final Long requestId;
    private final Set<Request> requests;

    public RemoteResolverRequest() {
        this.requestId = RemoteResolverRequestSeq.incrementAndGet();
        this.requests = new HashSet<>();
    }


    public static class UnresolvedException extends Exception {
        public UnresolvedException(final String message) {
            super(message);

        }
    }

    public RemoteResolverRequest resolveAccount(final org.killbill.billing.account.api.Account srcAccount, final boolean createIfMissing) throws UnresolvedException {

        if (srcAccount.getExternalKey() != null) {
            requests.add(new Request(ResolvingType.ACCOUNT, srcAccount.getExternalKey(), (client, requestOptions, response) -> {
                Account account = client.getAccount(srcAccount.getExternalKey(), requestOptions);
                if (createIfMissing && account == null) {
                    final Account input = new Account();
                    input.setExternalKey(srcAccount.getExternalKey());
                    input.setCountry(srcAccount.getCountry());
                    input.setLocale(srcAccount.getLocale());
                    if (srcAccount.getCurrency() != null) {
                        input.setCurrency(srcAccount.getCurrency().toString());
                    }
                    account = client.createAccount(input, requestOptions);
                }
                if (account.getAccountId() != null) {
                    response.setAccountIdMapping(account.getAccountId());
                } else {
                    throw new UnresolvedException(String.format("Failed to resolve account externalKey='%s'", srcAccount.getExternalKey()));
                }
            }));
        }
        return this;
    }

    public RemoteResolverRequest resolvePM(final String pmExternalKey) throws UnresolvedException {

        if (pmExternalKey != null) {
            requests.add(new Request(ResolvingType.PAYMENT_METHOD, pmExternalKey, (client, requestOptions, response) -> {
                final PaymentMethod pm = client.getPaymentMethodByKey(pmExternalKey, requestOptions);
                if (pm != null) {
                    response.setPaymentMethodIdMapping(pm.getPaymentMethodId());
                } else {
                    throw new UnresolvedException(String.format("Failed to resolve payment method externalKey='%s'", pmExternalKey));
                }
            }));
        }
        return this;
    }

    public RemoteResolverRequest resolvePayment(final String paymentExternalKey) throws UnresolvedException {

        if (paymentExternalKey != null) {
            requests.add(new Request(ResolvingType.PAYMENT, paymentExternalKey, (client, requestOptions, response) -> {
                final Payment payment = client.getPaymentByExternalKey(paymentExternalKey, requestOptions);
                if (payment != null) {
                    response.setPaymentIdMapping(payment.getPaymentId());
                } else {
                    throw new UnresolvedException(String.format("Failed to resolve payment externalKey='%s'", paymentExternalKey));
                }
            }));
        }
        return this;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Set<Request> getRequests() {
        return requests;
    }

    public interface Resolver {

        void resolveIds(final KillBillClient client, final RequestOptions requestOptions, final RemoteResolverResponse.RemoteResolverResponseBuilder response) throws KillBillClientException, UnresolvedException;
    }

    public static class Request {

        private final ResolvingType type;
        private final String srcKey;
        private final Resolver resolver;

        public Request(final ResolvingType type, final String srcKey, final Resolver resolver) {
            this.type = type;
            this.srcKey = srcKey;
            this.resolver = resolver;
        }

        public void resolve(final KillBillClient client, final RequestOptions requestOptions, final RemoteResolverResponse.RemoteResolverResponseBuilder response) throws UnresolvedException, KillBillClientException {
            resolver.resolveIds(client, requestOptions, response);
        }

        public ResolvingType getType() {
            return type;
        }

        public String getSrcKey() {
            return srcKey;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Request)) {
                return false;
            }

            final Request request = (Request) o;

            if (type != request.type) {
                return false;
            }
            return srcKey != null ? srcKey.equals(request.srcKey) : request.srcKey == null;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (srcKey != null ? srcKey.hashCode() : 0);
            return result;
        }
    }
}
