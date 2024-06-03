/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.httpclient.ejb;

import org.jboss.ejb.client.EJBModuleIdentifier;
import org.wildfly.transaction.client.SimpleXid;

import javax.transaction.xa.Xid;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class Serializer {

    private Serializer() {
        // forbidden instantiation
    }

    static Set<EJBModuleIdentifier> deserializeSet(final ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        Set<EJBModuleIdentifier> ret = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            ret.add((EJBModuleIdentifier) in.readObject());
        }
        return ret;
    }

    static void serializeSet(final ObjectOutput out, final Set<EJBModuleIdentifier> modules) throws IOException {
        out.writeInt(modules.size());
        for (EJBModuleIdentifier ejbModuleIdentifier : modules) {
            out.writeObject(ejbModuleIdentifier);
        }
    }

    static Object deserializeObject(final ObjectInput in) throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    static void serializeObject(final ObjectOutput out, final Object o) throws IOException {
        out.writeObject(o);
    }

    static Map<String, Object> deserializeMap(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int contextDataSize = PackedInteger.readPackedInteger(in);
        if (contextDataSize == 0) {
            return null;
        }
        final Map<String, Object> ret = new HashMap<>(contextDataSize);
        for (int i = 0; i < contextDataSize; i++) {
            // read the key
            final String key = (String) in.readObject();
            // read the attachment value
            final Object val = in.readObject();
            ret.put(key, val);
        }
        return ret;
    }

    static void serializeMap(final ObjectOutput out, final Map<String, Object> contextData) throws IOException {
        int size = contextData != null ? contextData.size() : 0;
        PackedInteger.writePackedInteger(out, size);
        if (size > 0) for (Map.Entry<String, Object> entry : contextData.entrySet()) {
            out.writeObject(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    static Xid deserializeXid(final ObjectInput in) throws IOException {
        int formatId = in.readInt();
        int length = in.readInt();
        byte[] globalId = new byte[length];
        in.readFully(globalId);
        length = in.readInt();
        byte[] branchId = new byte[length];
        in.readFully(branchId);
        return new SimpleXid(formatId, globalId, branchId);
    }

    static void serializeXid(final ObjectOutput out, final Xid xid) throws IOException {
        out.writeInt(xid.getFormatId());
        out.writeInt(xid.getGlobalTransactionId().length);
        out.write(xid.getGlobalTransactionId());
        out.writeInt(xid.getBranchQualifier().length);
        out.write(xid.getBranchQualifier());
    }

}
