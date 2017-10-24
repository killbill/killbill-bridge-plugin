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

package org.killbill.billing.plugin.bridge.api.resolver.remote;

import org.killbill.billing.client.KillBillClient;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteResolver {

    private final Logger logger = LoggerFactory.getLogger(RemoteResolver.class);

    private final KillBillClient client;
    private final RequestOptions requestOptions;


    public RemoteResolver(final KillBillClient client, final RequestOptions requestOptions) {
        this.client = client;
        this.requestOptions = requestOptions;
    }

    // TODO We could execute some requests in parallel -- exercise for the reader...
    public RemoteResolverResponse resolve(final RemoteResolverRequest request) {
        final RemoteResolverResponse.RemoteResolverResponseBuilder result = new RemoteResolverResponse.RemoteResolverResponseBuilder();
        request.getRequests()
                .stream()
                .forEachOrdered(r -> {
                    try {
                        r.resolve(client, requestOptions, result);
                        logger.info(String.format("RemoteResolver [%d]: type='%s', id='%s' -> resolvedId='%s'",
                                request.getRequestId(), r.getType(), r.getSrcKey(), result.getMapping(r.getType())));
                    } catch (final KillBillClientException e) {
                        logger.warn(String.format("RemoteResolver failed to execute request='%s', id='%s'", r.getType(), r.getSrcKey()));
                    }
                });

        return result.build();
    }
}
