package edu.eci.arem.lab2.framework;

import java.util.Map;
import java.util.LinkedHashMap;

public class Router {

    private volatile Map<String, Service> getRoutes = Map.of();

    public synchronized void addGet(String path, Service service) {
        Map<String, Service> updatedRoutes = new LinkedHashMap<>(getRoutes);
        updatedRoutes.put(path, service);
        getRoutes = Map.copyOf(updatedRoutes);
    }

    /** null si no hay ruta dinámica registrada para este path -> el server cae a static/404. */
    public Service resolve(String path) {
        return getRoutes.get(path);
    }
    
}
