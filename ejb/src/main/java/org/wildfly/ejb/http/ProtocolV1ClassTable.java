package org.wildfly.ejb.http;

import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.BasicSessionID;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBHandle;
import org.jboss.ejb.client.EJBHomeHandle;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.ejb.client.SerializedEJBInvocationHandler;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.UnknownSessionID;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.marshalling.ByteWriter;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Unmarshaller;

import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBObject;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.FinderException;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Future;
/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Jaikiran Pai
 */
final class ProtocolV1ClassTable implements ClassTable {
    static final ProtocolV1ClassTable INSTANCE = new ProtocolV1ClassTable();

    private static final Map<Class<?>, ByteWriter> writers;
    /**
     * Do NOT change the order of this list.
     * Do NOT even remove entries from this list.
     */
    private static final Class<?>[] classes = {
            EJBLocator.class,
            EJBHomeLocator.class,
            StatelessEJBLocator.class,
            StatefulEJBLocator.class,
            EntityEJBLocator.class,
            EJBHandle.class,
            EJBHomeHandle.class,
            SerializedEJBInvocationHandler.class,
            SessionID.class,
            UnknownSessionID.class,
            BasicSessionID.class,
            UserTransactionID.class,
            XidTransactionID.class,
            EJBHome.class,
            EJBObject.class,
            Handle.class,
            HomeHandle.class,
            EJBMetaData.class,
            RemoteException.class,
            NoSuchEJBException.class,
            NoSuchEntityException.class,
            CreateException.class,
            DuplicateKeyException.class,
            EJBAccessException.class,
            EJBException.class,
            EJBTransactionRequiredException.class,
            EJBTransactionRolledbackException.class,
            FinderException.class,
            RemoveException.class,
            ObjectNotFoundException.class,
            Future.class,
            SystemException.class,
            RollbackException.class,
            TransactionRequiredException.class,
            TransactionRolledbackException.class,
            NotSupportedException.class,
            InvalidTransactionException.class,
            Throwable.class, // <--
            Exception.class, // <--
            RuntimeException.class, // <--
            StackTraceElement.class,
            SessionID.Serialized.class,
            TransactionID.class,
            TransactionID.Serialized.class,
            Affinity.class,
            Affinity.NONE.getClass(),
            NodeAffinity.class,
            ClusterAffinity.class,
    };

    static {
        final Map<Class<?>, ByteWriter> map = new IdentityHashMap<Class<?>, ByteWriter>();

        Class<?> clazz;

        for (int i = 0, length = classes.length; i < length; i++) {
            clazz = classes[i];
            if (Throwable.class.isAssignableFrom(clazz)) {
                // don't write out exception types as class table items since they're out of our control
                continue;
            }
            map.put(clazz, new ByteWriter((byte) i));
        }
        writers = map;
    }

    @Override
    public Writer getClassWriter(final Class<?> clazz) throws IOException {
        return writers.get(clazz);
    }

    @Override
    public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int idx = unmarshaller.readUnsignedByte();
        if (idx >= classes.length) {
            throw new ClassNotFoundException("ClassTable " + this.getClass().getName() + " cannot find a class for class index " + idx);
        }
        return classes[idx];
    }

    private ProtocolV1ClassTable() {
    }
}