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
import org.killbill.billing.plugin.bridge.api.resolver.remote.RemoteResolverRequest.UnresolvedException;
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
    public RemoteResolverResponse resolve(final RemoteResolverRequest request) throws KillBillClientException {
        final RemoteResolverResponse.RemoteResolverResponseBuilder result = new RemoteResolverResponse.RemoteResolverResponseBuilder();
        request.getRequests()
               .stream()
               .forEachOrdered(r -> {
                   boolean resolved = false;
                   try {
                       r.resolve(client, requestOptions, result);
                       resolved = true;
                   } catch (final KillBillClientException e) {
                       logger.warn("RemoteResolver {}: KillBillClientException...", request.getRequestId(), e);
                       throw new  WrappedKillBillClientException(e);
                   } catch (final UnresolvedException e) {
                       logger.warn("RemoteResolver {}: UnresolvedException...", request.getRequestId(), e);
                       throw new WrappedUnresolvedException(e.getMessage());
                   } finally {
                       if (resolved) {
                           logger.info("RemoteResolver {}: type='{}', id='{}' -> resolvedId='{}'",
                                                     request.getRequestId(), r.getType(), r.getSrcKey(), result.getMapping(r.getType()));
                       }
                   }
               });
        return result.build();
    }


    public static class WrappedKillBillClientException extends RuntimeException {
        public WrappedKillBillClientException(final Throwable cause) {
            super(cause);
        }
    }

    public static class WrappedUnresolvedException extends RuntimeException {
        public WrappedUnresolvedException(final String message) {
            super(message);
        }
    }


}
