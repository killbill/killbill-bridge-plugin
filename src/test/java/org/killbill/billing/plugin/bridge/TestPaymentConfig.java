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

package org.killbill.billing.plugin.bridge;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class TestPaymentConfig {

    @Test(groups = "fast")
    public void testParsePluginProperties() {
        PaymentConfig paymentConfig = new PaymentConfig();
        Assert.assertTrue(paymentConfig.getPluginProperties().isEmpty());

        paymentConfig = new PaymentConfig();
        paymentConfig.pluginProperties = ImmutableMap.<String, String>of("a", "bcd");
        Assert.assertEquals(paymentConfig.getPluginProperties().size(), 1);
        Assert.assertEquals(paymentConfig.getPluginProperties().get(0).getKey(), "a");
        Assert.assertEquals(paymentConfig.getPluginProperties().get(0).getValue(), "bcd");

        paymentConfig = new PaymentConfig();
        paymentConfig.pluginProperties = ImmutableMap.<String, String>of("a", "bcd", "e", "{\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"}");
        Assert.assertEquals(paymentConfig.getPluginProperties().size(), 2);
        Assert.assertEquals(paymentConfig.getPluginProperties().get(0).getKey(), "a");
        Assert.assertEquals(paymentConfig.getPluginProperties().get(0).getValue(), "bcd");
        Assert.assertEquals(paymentConfig.getPluginProperties().get(1).getKey(), "e");
        Assert.assertEquals(paymentConfig.getPluginProperties().get(1).getValue(), "{\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"}");
    }
}
