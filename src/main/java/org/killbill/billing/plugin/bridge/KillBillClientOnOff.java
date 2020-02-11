/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillHttpClient;

public class KillBillClientOnOff extends KillBillClient {

    private final Boolean isActive;

    public KillBillClientOnOff() {
        super(new KillBillHttpClient());
        this.isActive = true;
    }

    public KillBillClientOnOff(final KillBillHttpClient httpClient, final Boolean isActive) {
        super(httpClient);
        this.isActive = isActive;
    }

    public Boolean isActive() {
        return isActive;
    }
}

