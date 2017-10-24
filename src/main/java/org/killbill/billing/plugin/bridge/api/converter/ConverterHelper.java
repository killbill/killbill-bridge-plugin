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

package org.killbill.billing.plugin.bridge.api.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConverterHelper {

    public static PaymentPluginStatus toPluginStatus(final String transactionStatusInput) {
        final TransactionStatus transactionStatus = TransactionStatus.valueOf(transactionStatusInput);
        switch(transactionStatus) {
            case SUCCESS:
                return PaymentPluginStatus.PROCESSED;
            case PENDING:
                return PaymentPluginStatus.PENDING;
            case PAYMENT_FAILURE:
                return PaymentPluginStatus.ERROR;
            case PLUGIN_FAILURE:
                return PaymentPluginStatus.CANCELED;
            case UNKNOWN:
            default:
                return PaymentPluginStatus.UNDEFINED;

        }
    }


    public static List<PluginProperty> convertToApiPluginProperties(@Nullable final Map<String, Object> input) {
        return input != null && !input.isEmpty()?
                input.keySet().stream()
                        .map(k -> new PluginProperty(k, input.get(k), true))
                        .collect(Collectors.toList()) :
                ImmutableList.of();
    }

    public static List<PluginProperty> convertToApiPluginProperties(@Nullable final List<org.killbill.billing.client.model.PluginProperty> input) {
        return input != null ?
                input.stream()
                        .map(p -> new PluginProperty(p.getKey(), p.getValue(), p.getIsUpdatable()))
                        .collect(Collectors.toList()) :
                ImmutableList.of();
    }


    public static List<org.killbill.billing.client.model.PluginProperty> convertToClientListPluginProperties(@Nullable final Iterable<PluginProperty> input) {

        return input != null && input.iterator().hasNext() ?
                ImmutableList.copyOf(input).stream()
                        .map(p -> new org.killbill.billing.client.model.PluginProperty(p.getKey(), safeValue(p.getValue()), p.getIsUpdatable()))
                        .collect(Collectors.toList()) :
                ImmutableList.of();
    }

    public static Map<String, String> convertToClientMapPluginProperties(@Nullable final Iterable<PluginProperty> input, @Nullable final PluginProperty...extras) {

        final ImmutableList.Builder<PluginProperty> sanitizedInput = ImmutableList.builder();
        if (input != null) {
            sanitizedInput.addAll(input);
        }
        if (extras != null && extras.length > 0) {
            sanitizedInput.add(extras);
        }

        return sanitizedInput.build().stream()
                .collect(Collectors.toMap(s -> s.getKey(), s -> safeValue(s.getValue())));
    }

    public static Optional<org.killbill.billing.client.model.PaymentTransaction> getTransactionMatchOrLast(final List<org.killbill.billing.client.model.PaymentTransaction> transactions, @Nullable final String kbTransactionExternalKey) {
        org.killbill.billing.client.model.PaymentTransaction result = null;
        if (transactions != null && !transactions.isEmpty()) {
            result = transactions.stream().filter(paymentTransaction -> (kbTransactionExternalKey != null && paymentTransaction.getTransactionExternalKey().equals(kbTransactionExternalKey)))
                    //.peek(paymentTransaction -> System.err.println(String.format(".... transactionId ='%s'", paymentTransaction.getTransactionId())))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), paymentTransactions -> paymentTransactions.isEmpty() ? transactions.get(transactions.size() - 1) : paymentTransactions.get(0)));
        }
        return Optional.ofNullable(result);
    }

    private static String safeValue(final Object rawValue) {
        if (rawValue instanceof String) {
            return (String) rawValue;
        } else {
            return String.valueOf(rawValue);
        }
    }
}
