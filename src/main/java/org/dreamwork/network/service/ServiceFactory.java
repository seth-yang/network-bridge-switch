package org.dreamwork.network.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceFactory {
    private static final Map<String, IService> services = new ConcurrentHashMap<> ();

    public static void register (String name, IService service) {
        services.put (name, service);
    }

    public static void register (Class<? extends IService> serviceType) {
        try {
            IService service = serviceType.newInstance ();
            services.put (serviceType.getCanonicalName (), service);
        } catch (Exception ex) {
            throw new RuntimeException (ex);
        }
    }

    @SuppressWarnings ("unchecked")
    public static<T extends IService> T get (Class<T> type) {
        return (T) services.get (type.getCanonicalName ());
    }

    @SuppressWarnings ("unchecked")
    public static<T extends IService> T get (String name) {
        return (T) services.get (name);
    }
}
