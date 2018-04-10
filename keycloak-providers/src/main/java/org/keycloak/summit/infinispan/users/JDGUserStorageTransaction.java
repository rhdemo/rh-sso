/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.summit.infinispan.users;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGUserStorageTransaction extends AbstractKeycloakTransaction {

    private static final Logger logger = Logger.getLogger(JDGUserStorageTransaction.class);

    protected final RemoteCache remoteCache;
    private final Map<String, JDGOperationContext> localCache = new HashMap<>();

    public JDGUserStorageTransaction(RemoteCache remoteCache) {
        this.remoteCache = remoteCache;
    }

    public Object get(String cacheKey) {
        if (localCache.containsKey(cacheKey)) {
            JDGOperationContext ctx = localCache.get(cacheKey);

            // Removed in this transaction
            if (ctx.op == JDGOperation.REMOVE) {
                return null;
            } else {
                return ctx.object;
            }
        } else {
            Object obj = remoteCache.get(cacheKey);
            if (obj != null) {
                localCache.put(cacheKey, new JDGOperationContext(JDGOperation.GET, obj));
            }
            return obj;
        }
    }

    public void create(String cacheKey, Object value) {
        localCache.put(cacheKey, new JDGOperationContext(JDGOperation.CREATE, value));
    }

    public void replace(String cacheKey, Object value) {
        JDGOperationContext existing = localCache.get(cacheKey);
        if (existing != null) {
            existing.object = value;
            if (existing.op == JDGOperation.GET) {
                existing.op = JDGOperation.REPLACE;
            }
        } else {
            localCache.put(cacheKey, new JDGOperationContext(JDGOperation.REPLACE, value));
        }
    }

    public void remove(String cacheKey) {
        localCache.put(cacheKey, new JDGOperationContext(JDGOperation.REMOVE, new Object()));
    }

    @Override
    protected void commitImpl() {
        for (Map.Entry<String, JDGOperationContext> entry : localCache.entrySet()) {
            String cacheKey = entry.getKey();
            JDGOperationContext ctx = entry.getValue();
            if (ctx.op == JDGOperation.CREATE) {
                logger.debugf("Calling remoteCache.put(%s, %s)", cacheKey, ctx.object);
                remoteCache.put(cacheKey, ctx.object);
            } else if (ctx.op == JDGOperation.REPLACE){
                logger.debugf("Calling remoteCache.replace(%s, %s)", cacheKey, ctx.object);
                remoteCache.replace(cacheKey, ctx.object);
            } else if (ctx.op == JDGOperation.REMOVE) {
                logger.debugf("Calling remoteCache.remove(%s)", cacheKey);
                remoteCache.remove(cacheKey);
            }
        }
    }

    @Override
    protected void rollbackImpl() {

    }

    private static class JDGOperationContext {

        private JDGOperation op;
        private Object object;

        public JDGOperationContext(JDGOperation op, Object object) {
            this.op = op;
            this.object = object;
        }

    }

    private enum JDGOperation {
        CREATE, REPLACE, REMOVE, GET
    }
}
