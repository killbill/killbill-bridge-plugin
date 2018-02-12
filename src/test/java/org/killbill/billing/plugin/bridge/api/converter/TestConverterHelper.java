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
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TestConverterHelper {


    @Test(groups = "fast")
    public void testFindTransactionWithTarget() {

        //
        final UUID targetTransactionId = UUID.randomUUID();
        PaymentTransaction t1 = createPaymentTransaction(targetTransactionId);
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = ConverterHelper.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), targetTransactionId.toString());
        Assert.assertTrue(result.isPresent());
        // We should default have found the target transaction
        Assert.assertEquals(result.get().getTransactionExternalKey(), targetTransactionId.toString());
    }

    @Test(groups = "fast")
    public void testFindTransactionWithMissingTarget() {

        final UUID missingTargetTransactionId = UUID.randomUUID();
        PaymentTransaction t1 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = ConverterHelper.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), missingTargetTransactionId.toString());
        Assert.assertTrue(result.isPresent());
        // We should default to last transaction
        Assert.assertEquals(result.get().getTransactionId(), t3.getTransactionId());
    }


    @Test(groups = "fast")
    public void testFindTransactionWithNullTarget() {
        PaymentTransaction t1 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = ConverterHelper.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), null);
        Assert.assertTrue(result.isPresent());
        // We should default to last transaction
        Assert.assertEquals(result.get().getTransactionId(), t3.getTransactionId());
    }

    @Test(groups = "fast")
    public void testConvertToApiPluginPropertiesWithNullProperties() {
        Assert.assertEquals(ConverterHelper.convertToApiPluginProperties((List) null).size(), 0);
    }


    @Test(groups = "fast")
    public void testConvertToApiPluginPropertiesWithEmptyProperties() {
        Assert.assertEquals(ConverterHelper.convertToApiPluginProperties(ImmutableList.of()).size(), 0);
    }


    @Test(groups = "fast")
    public void testConvertToApiPluginPropertiesWithExistingProperties() {
        final org.killbill.billing.client.model.PluginProperty p1 = new org.killbill.billing.client.model.PluginProperty("keyA", "valueA", true);
        final org.killbill.billing.client.model.PluginProperty p2 = new org.killbill.billing.client.model.PluginProperty("keyB", "valueB", true);

        final List<PluginProperty> result = ConverterHelper.convertToApiPluginProperties(ImmutableList.of(p1, p2));
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getKey(), p1.getKey());
        Assert.assertEquals(result.get(0).getValue(), p1.getValue());
        Assert.assertEquals(result.get(0).getIsUpdatable(), p1.getIsUpdatable());

        Assert.assertEquals(result.get(1).getKey(), p2.getKey());
        Assert.assertEquals(result.get(1).getValue(), p2.getValue());
        Assert.assertEquals(result.get(1).getIsUpdatable(), p2.getIsUpdatable());
    }

    @Test(groups = "fast")
    public void testConvertToClientPluginPropertiesWithNullProperties() {
        Assert.assertEquals(ConverterHelper.convertToClientListPluginProperties(null).size(), 0);
    }

    @Test(groups = "fast")
    public void testConvertToClientPluginPropertiesWithEmptyProperties() {
        Assert.assertEquals(ConverterHelper.convertToClientListPluginProperties(null).size(), 0);
    }


    @Test(groups = "fast")
    public void testConvertToClientPluginPropertiesWithExistingProperties() {

        final PluginProperty p1 = new PluginProperty("keyA", "valueA", true);
        final PluginProperty p2 = new PluginProperty("keyB", "valueB", true);

        final Map<String, String> result = ConverterHelper.convertToClientMapPluginProperties(ImmutableList.of(p1, p2));
        Assert.assertEquals(result.size(), 2);

        Assert.assertEquals(result.get(p1.getKey()), p1.getValue());
        Assert.assertEquals(result.get(p2.getKey()), p2.getValue());
    }


    private PaymentTransaction createPaymentTransaction(final UUID transactionId) {
        PaymentTransaction t = new PaymentTransaction();
        t.setTransactionId(transactionId);
        t.setTransactionExternalKey(transactionId.toString());
        return t;
    }
}
