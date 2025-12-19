package com.server.protocol.http;

public class HttpCookie {

    public enum SameSite {
        STRICT, LAX, NONE;

        @Override
        public String toString() {
            String output = super.toString();
            return output.charAt(0) + output.substring(1).toLowerCase();
        }
    }

    private String name;
    private String value;
    private String domain;
    private String path;
    private int maxAge;
    private boolean secure;
    private boolean httpOnly;
    private SameSite sameSite;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append('=').append(value);

        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }

        if (path != null && !path.isEmpty()) {
            sb.append("; Path=").append(path);
        }

        if (maxAge >= 0) {
            sb.append("; Max-Age=").append(maxAge);
        }

        if (secure) {
            sb.append("; Secure");
        }

        if (httpOnly) {
            sb.append("; HttpOnly");
        }

        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite);
        }

        return sb.toString();
    }


    private HttpCookie(Builder builder) {
        this.domain = builder.domain;
        this.maxAge = builder.maxAge;
        this.httpOnly = builder.httpOnly;
        this.path = builder.path;
        this.secure = builder.secure;
        this.sameSite = builder.sameSite;
        this.name = builder.name;
        this.value = builder.value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public SameSite getSameSite() {
        return sameSite;
    }

    public void setSameSite(SameSite sameSite) {
        this.sameSite = sameSite;
    }

    public static class Builder {
        private String name;
        private String value;

        private String domain = "localhost";
        private String path = "/";
        private int maxAge = 3600;
        private boolean secure = false;
        private boolean httpOnly = false;
        private SameSite sameSite = SameSite.NONE;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setMaxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder setSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        public Builder setSameSite(SameSite sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        public HttpCookie build() {
            return new HttpCookie(this);
        }
    }
}
