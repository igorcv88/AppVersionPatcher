package io.github.igorcv88.appversionpatcher;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class XposedServiceClient {
    public interface Listener {
        void onServiceAvailable(XposedService service);
        void onServiceUnavailable();
    }

    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static volatile XposedService currentService;
    private static boolean registered;

    private XposedServiceClient() {}

    public static synchronized void addListener(Listener listener) {
        LISTENERS.add(listener);
        ensureRegistered();
        XposedService service = currentService;
        if (service != null) {
            listener.onServiceAvailable(service);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static XposedService getService() {
        return currentService;
    }

    private static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                XposedService previous = currentService;
                if (previous == null || safeApi(service) >= safeApi(previous)) {
                    currentService = service;
                    for (Listener listener : LISTENERS) {
                        listener.onServiceAvailable(service);
                    }
                }
            }

            @Override
            public void onServiceDied(XposedService service) {
                if (currentService == service) {
                    currentService = null;
                    for (Listener listener : LISTENERS) {
                        listener.onServiceUnavailable();
                    }
                }
            }
        });
    }

    private static int safeApi(XposedService service) {
        try {
            return service.getApiVersion();
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
