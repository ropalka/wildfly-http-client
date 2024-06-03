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
package org.wildfly.httpclient.transaction;

import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Marshaller;
import org.wildfly.httpclient.common.NoFlushByteOutput;
import org.wildfly.transaction.client.SimpleXid;

import javax.transaction.xa.Xid;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.OutputStream;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class Serializer {

    private Serializer() {
        // forbidden instantiation
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

    static void serializeXid(final Marshaller marshaller, final OutputStream os, final Xid xid) throws IOException {
        try (os) {
            marshaller.start(new NoFlushByteOutput(Marshalling.createByteOutput(os)));
            marshaller.writeInt(xid.getFormatId());
            marshaller.writeInt(xid.getGlobalTransactionId().length);
            marshaller.write(xid.getGlobalTransactionId());
            marshaller.writeInt(xid.getBranchQualifier().length);
            marshaller.write(xid.getBranchQualifier());
            marshaller.flush();
        } finally {
            marshaller.finish();
        }
    }

    static Xid[] deserializeXidArray(final ObjectInput in) throws IOException {
        int length = in.readInt();
        Xid[] ret = new Xid[length];
        for (int i = 0; i < length; ++i) {
            ret[i] = deserializeXid(in);
        }
        return ret;
    }

    static void serializeXidArray(final Marshaller marshaller, final OutputStream os, final Xid[] xids) throws IOException {
        try (os) {
            marshaller.start(new NoFlushByteOutput(Marshalling.createByteOutput(os)));
            marshaller.writeInt(xids.length);
            for (Xid xid : xids) {
                marshaller.writeInt(xid.getFormatId());
                marshaller.writeInt(xid.getGlobalTransactionId().length);
                marshaller.write(xid.getGlobalTransactionId());
                marshaller.writeInt(xid.getBranchQualifier().length);
                marshaller.write(xid.getBranchQualifier());
            }
            marshaller.flush();
        } finally {
            marshaller.finish();
        }
    }

    static void serializeThrowable(final Marshaller marshaller, final OutputStream os, final Throwable t) throws IOException {
        try (os) {
            final ByteOutput byteOutput = new NoFlushByteOutput(Marshalling.createByteOutput(os));
            marshaller.start(byteOutput);
            marshaller.writeObject(t);
            marshaller.write(0);
            marshaller.flush();
        } finally {
            marshaller.finish();
        }
    }

}
