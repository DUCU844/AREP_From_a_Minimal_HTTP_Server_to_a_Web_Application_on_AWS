package edu.eci.arem.lab2.framework;

import edu.eci.arem.lab2.http.HttpRequest;

public interface Service {
    String handle(HttpRequest req, Response resp);
        
}
