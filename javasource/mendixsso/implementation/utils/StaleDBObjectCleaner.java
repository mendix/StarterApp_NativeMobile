package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IEntityProxy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class StaleDBObjectCleaner {

    private static final ILogNode LOG = Core.getLogger("StaleDBObjectCleaner");

    private StaleDBObjectCleaner() {
    }

    public static <T> long cleanupStaleObjects(
        final Class<T> clazz,
        final String entityName,
        final String expiryFieldName,
        final long olderThan,
        final int batchSize)
        throws InterruptedException {
        return ThreadingBatchingListProcessor.process(
            clazz.getSimpleName() + "Cleanup",
            batchSize,
            retrieveStaleObjects(clazz, entityName, expiryFieldName, olderThan),
            deleteStaleObject());
    }

    private static <T> BiFunction<Integer, Long, List<T>> retrieveStaleObjects(
        final Class<T> clazz,
        final String entityName,
        final String expiryFieldName,
        final long deadline) {
        return (batchSize, total) -> {
            final IContext context = Core.createSystemContext();
            return MendixUtils.retrieveFromDatabase(
                context,
                clazz,
                batchSize,
                0,
                Collections.emptyMap(),
                0,
                "//%s[%s <= $deadline]",
                Map.of("deadline", deadline),
                entityName,
                expiryFieldName);
        };
    }

    private static <T> Consumer<T> deleteStaleObject() {
        return staleObject -> {
            try {
                IEntityProxy entityProxy = (IEntityProxy) staleObject;
                // create new system contexts to avoid concurrency clashes
                entityProxy.delete(Core.createSystemContext());
            } catch (Exception exception) {
                String exceptionMessage =
                    String.format(
                        "Could not cleaned object of type: %s, reason: %s ",
                        staleObject.getClass().getSimpleName(), exception.getMessage());
                LOG.error(exceptionMessage);
                throw new MendixRuntimeException(exceptionMessage);
            }
        };
    }

}
