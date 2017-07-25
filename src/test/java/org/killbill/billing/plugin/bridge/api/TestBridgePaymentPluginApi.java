package org.killbill.billing.plugin.bridge.api;

import com.google.common.collect.ImmutableList;
import org.killbill.billing.client.model.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TestBridgePaymentPluginApi {


    @Test
    public void testFindTransactionWithTarget() {

        //
        final UUID targetTransactionId = UUID.randomUUID();
        PaymentTransaction t1 = createPaymentTransaction(targetTransactionId);
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = BridgePaymentPluginApi.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), targetTransactionId);
        Assert.assertTrue(result.isPresent());
        // We should default have found the target transaction
        Assert.assertEquals(result.get().getTransactionId(), targetTransactionId);
    }

    @Test
    public void testFindTransactionWithMissingTarget() {

        final UUID missingTargetTransactionId = UUID.randomUUID();
        PaymentTransaction t1 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = BridgePaymentPluginApi.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), missingTargetTransactionId);
        Assert.assertTrue(result.isPresent());
        // We should default to last transaction
        Assert.assertEquals(result.get().getTransactionId(), t3.getTransactionId());
    }


    @Test
    public void testFindTransactionWithNullTarget() {
        PaymentTransaction t1 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t2 = createPaymentTransaction(UUID.randomUUID());
        PaymentTransaction t3 = createPaymentTransaction(UUID.randomUUID());

        final Optional<PaymentTransaction> result = BridgePaymentPluginApi.getTransactionMatchOrLast(ImmutableList.of(t1, t2, t3), null);
        Assert.assertTrue(result.isPresent());
        // We should default to last transaction
        Assert.assertEquals(result.get().getTransactionId(), t3.getTransactionId());
    }

    @Test
    public void testConvertToApiPluginPropertiesWithNullProperties() {
        Assert.assertEquals(BridgePaymentPluginApi.convertToApiPluginProperties(null).size(), 0);
    }


    @Test
    public void testConvertToApiPluginPropertiesWithEmptyProperties() {
        Assert.assertEquals(BridgePaymentPluginApi.convertToApiPluginProperties(ImmutableList.of()).size(), 0);
    }


    @Test
    public void testConvertToApiPluginPropertiesWithExistingProperties() {
        final org.killbill.billing.client.model.PluginProperty p1 = new org.killbill.billing.client.model.PluginProperty("keyA", "valueA", true);
        final org.killbill.billing.client.model.PluginProperty p2 = new org.killbill.billing.client.model.PluginProperty("keyB", "valueB", true);

        final List<PluginProperty> result = BridgePaymentPluginApi.convertToApiPluginProperties(ImmutableList.of(p1, p2));
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getKey(), p1.getKey());
        Assert.assertEquals(result.get(0).getValue(), p1.getValue());
        Assert.assertEquals(result.get(0).getIsUpdatable(), p1.getIsUpdatable());

        Assert.assertEquals(result.get(1).getKey(), p2.getKey());
        Assert.assertEquals(result.get(1).getValue(), p2.getValue());
        Assert.assertEquals(result.get(1).getIsUpdatable(), p2.getIsUpdatable());
    }

    @Test
    public void testConvertToClientPluginPropertiesWithNullProperties() {
        Assert.assertEquals(BridgePaymentPluginApi.convertToClientPluginProperties(null).size(), 0);
    }

    @Test
    public void testConvertToClientPluginPropertiesWithEmptyProperties() {
        Assert.assertEquals(BridgePaymentPluginApi.convertToClientPluginProperties(null).size(), 0);
    }


    @Test
    public void testConvertToClientPluginPropertiesWithExistingProperties() {

        final PluginProperty p1 = new PluginProperty("keyA", "valueA", true);
        final PluginProperty p2 = new PluginProperty("keyB", "valueB", true);

        final Map<String, String> result = BridgePaymentPluginApi.convertToClientPluginProperties(ImmutableList.of(p1, p2));
        Assert.assertEquals(result.size(), 2);

        Assert.assertEquals(result.get(p1.getKey()), p1.getValue());
        Assert.assertEquals(result.get(p2.getKey()), p2.getValue());
    }


    private PaymentTransaction createPaymentTransaction(final UUID transactionId) {
        PaymentTransaction t = new PaymentTransaction();
        t.setTransactionId(transactionId);
        return t;
    }
}
