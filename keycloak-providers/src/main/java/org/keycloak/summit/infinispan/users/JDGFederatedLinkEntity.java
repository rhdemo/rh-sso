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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(JDGFederatedLinkEntity.ExternalizerImpl.class)
public class JDGFederatedLinkEntity {

    private String userId;
    private String identityProvider;
    private String userName;

    public JDGFederatedLinkEntity() {
    }

    public JDGFederatedLinkEntity(String userId, String identityProvider, String userName) {
        this.userId = userId;
        this.identityProvider = identityProvider;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + identityProvider.hashCode();
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JDGFederatedLinkEntity that = (JDGFederatedLinkEntity) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (!identityProvider.equals(that.identityProvider)) return false;
        return userName != null ? userName.equals(that.userName) : that.userName == null;
    }

    @Override
    public String toString() {
        return String.format("JDGFederatedLinkEntity [ userId=%s, userName=%s, identityProvider=%s ]", userId, userName, identityProvider);
    }


    public static class ExternalizerImpl implements Externalizer<JDGFederatedLinkEntity> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, JDGFederatedLinkEntity value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.userId, output);
            MarshallUtil.marshallString(value.identityProvider, output);
            MarshallUtil.marshallString(value.userName, output);
        }

        @Override
        public JDGFederatedLinkEntity readObject(ObjectInput input) throws IOException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public JDGFederatedLinkEntity readObjectVersion1(ObjectInput input) throws IOException {
            return new JDGFederatedLinkEntity(
                    MarshallUtil.unmarshallString(input), // userId
                    MarshallUtil.unmarshallString(input), // identityProvider
                    MarshallUtil.unmarshallString(input)  // userName
            );
        }
    }
}
