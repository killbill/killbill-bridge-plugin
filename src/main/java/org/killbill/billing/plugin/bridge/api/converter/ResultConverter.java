package org.killbill.billing.plugin.bridge.api.converter;

public interface ResultConverter<I /*extends org.killbill.billing.client.model.KillBillObject*/, R> {
    R convertModelToApi(I input);
}