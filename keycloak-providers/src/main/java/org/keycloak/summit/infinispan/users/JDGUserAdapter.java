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

import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGUserAdapter extends AbstractUserAdapterFederatedStorage {

    private static final Logger logger = Logger.getLogger(JDGUserAdapter.class);

    protected final JDGUserEntity entity;
    protected final String keycloakId;
    protected final JDGUserStorageProvider provider;
    protected final JDGUserStorageTransaction transaction;

    public JDGUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, JDGUserStorageProvider provider, JDGUserEntity entity) {
        super(session, realm, model);
        keycloakId = StorageId.keycloakId(model, entity.getId());
        this.provider = provider;
        this.transaction = provider.transaction;
        this.entity = entity;
    }

    public String getPassword() {
        return entity.getPassword();
    }

    public void setPassword(String password) {
        entity.setPassword(password);
    }

    public String getTotp() {
        return entity.getTotp();
    }

    public void setTotp(String totp) {
        entity.setTotp(totp);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);
        update();
    }

    @Override
    public void setEmail(String email) {
        if (Objects.equals(email, entity.getEmail())) {
            return;
        }

        // Changing email
        if (entity.getEmail() != null) {
            String existingEmailCacheKey = provider.getByEmailCacheKey(entity.getEmail());
            transaction.remove(existingEmailCacheKey);
        }

        entity.setEmail(email);
        update();

        // Cache key for lookup by email
        String emailCacheKey = provider.getByEmailCacheKey(email);
        transaction.create(emailCacheKey, entity.getId());
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
        update();
    }

    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        entity.setFirstName(firstName);
        update();
    }

    @Override
    public String getLastName() {
        return entity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        entity.setLastName(lastName);
        update();
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    protected void update() {
        this.transaction.replace(provider.getByIdCacheKey(entity.getId()), entity);
    }

//    @Override
//    public void setSingleAttribute(String name, String value) {
//        if (name.equals("phone")) {
//            entity.setPhone(value);
//        } else {
//            super.setSingleAttribute(name, value);
//        }
//    }
//
//    @Override
//    public void removeAttribute(String name) {
//        if (name.equals("phone")) {
//            entity.setPhone(null);
//        } else {
//            super.removeAttribute(name);
//        }
//    }
//
//    @Override
//    public void setAttribute(String name, List<String> values) {
//        if (name.equals("phone")) {
//            entity.setPhone(values.get(0));
//        } else {
//            super.setAttribute(name, values);
//        }
//    }
//
//    @Override
//    public String getFirstAttribute(String name) {
//        if (name.equals("phone")) {
//            return entity.getPhone();
//        } else {
//            return super.getFirstAttribute(name);
//        }
//    }
//
//    @Override
//    public Map<String, List<String>> getAttributes() {
//        Map<String, List<String>> attrs = super.getAttributes();
//        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
//        all.putAll(attrs);
//        all.add("phone", entity.getPhone());
//        return all;
//    }
//
//    @Override
//    public List<String> getAttribute(String name) {
//        if (name.equals("phone")) {
//            List<String> phone = new LinkedList<>();
//            phone.add(entity.getPhone());
//            return phone;
//        } else {
//            return super.getAttribute(name);
//        }
//    }
}
