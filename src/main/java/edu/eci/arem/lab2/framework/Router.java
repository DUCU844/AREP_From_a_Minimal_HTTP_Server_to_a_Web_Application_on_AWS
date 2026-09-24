package edu.eci.arem.lab2.framework;

import java.util.LinkedHashMap;
import java.util.Map;

public class Router {

    private final Map<String, Service> getRoutes = new LinkedHashMap<>();

    public void addGet(String path, Service service) {
        getRoutes.put(path, service);
    }

    /** null si no hay ruta dinámica registrada para este path -> el server cae a static/404. */
    public Service resolve(String path) {
        return getRoutes.get(path);
    }
    
}
