package edu.eci.arem.lab2.framework;

public class Response {
    private int statusCode = 200;
    private String reasonPhrase = "OK";
    private String contentType = "text/plain; charset=UTF-8";
    public void setStatus(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }
    public int getStatusCode() { return statusCode; }
    public String getReasonPhrase() { return reasonPhrase; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentType() { return contentType; }
    
}
