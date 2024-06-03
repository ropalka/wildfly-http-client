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
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

import java.io.IOException;
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

    static Set<EJBModuleIdentifier> deserializeSet(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int size = unmarshaller.readInt();
        Set<EJBModuleIdentifier> ret = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            ret.add((EJBModuleIdentifier) unmarshaller.readObject());
        }
        return ret;
    }

    static void serializeSet(final Marshaller marshaller, final Set<EJBModuleIdentifier> modules) throws IOException {
        marshaller.writeInt(modules.size());
        for (EJBModuleIdentifier ejbModuleIdentifier : modules) {
            marshaller.writeObject(ejbModuleIdentifier);
        }
    }

    static Object deserializeObject(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        return unmarshaller.readObject();
    }

    static void serializeObject(final Marshaller marshaller, final Object o) throws IOException {
        marshaller.writeObject(o);
    }

    static Map<String, Object> deserializeMap(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        final int contextDataSize = PackedInteger.readPackedInteger(unmarshaller);
        if (contextDataSize == 0) {
            return null;
        }
        final Map<String, Object> ret = new HashMap<>(contextDataSize);
        for (int i = 0; i < contextDataSize; i++) {
            // read the key
            final String key = (String) unmarshaller.readObject();
            // read the attachment value
            final Object val = unmarshaller.readObject();
            ret.put(key, val);
        }
        return ret;
    }

    static void serializeMap(final Marshaller marshaller, final Map<String, Object> contextData) throws IOException {
        PackedInteger.writePackedInteger(marshaller, contextData.size());
        for (Map.Entry<String, Object> entry : contextData.entrySet()) {
            marshaller.writeObject(entry.getKey());
            marshaller.writeObject(entry.getValue());
        }
    }

}
