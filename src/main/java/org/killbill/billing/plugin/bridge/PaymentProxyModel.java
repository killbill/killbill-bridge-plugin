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

package org.killbill.billing.plugin.bridge;

public enum PaymentProxyModel {
    PROXY_SIMPLE("proxy"),
    PROXY_ROUTING("proxy-routing");

    private String name;

    PaymentProxyModel(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PaymentProxyModel findPaymentProxyModel(final String name) {
        for (PaymentProxyModel cur :  PaymentProxyModel.values()) {
            if (name.equalsIgnoreCase(cur.getName())) {
                return cur;
            }
        }
        throw new IllegalStateException("Cannot find PaymentProxyModel " + name);
    }
}
