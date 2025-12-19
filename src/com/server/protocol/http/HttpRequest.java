package com.server.protocol.http;

import com.server.protocol.Protocol;
import com.server.protocol.Request;

import java.util.Map;

public class HttpRequest extends Request {
    private String method;
    private String uri;
    private String version;

    private Map<String, String> headers;

    Map<String, String> queryParams;
    HttpCookie[] httpCookies;
    Map<String, String> requestBody;

    public HttpRequest(Protocol protocol) {
        super(protocol);
    }

    public String getMethod() {
        return method;
    }

    void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public HttpCookie[] getCookies() {
        return httpCookies;
    }

    void setCookies(HttpCookie[] httpCookies) {
        this.httpCookies = httpCookies;
    }

    public Map<String, String> getRequestBody() {
        return requestBody;
    }

    void setRequestBody(Map<String, String> requestBody) {
        this.requestBody = requestBody;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
