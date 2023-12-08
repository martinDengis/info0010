/**
 * Represents the HTTP methods supported by the application.
 */
public enum HttpMethod {
    GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS;

    /**
     * Checks if the given method is allowed.
     *
     * @param method the HTTP method to check
     * @return true if the method is allowed, false otherwise
     */
    public static boolean isMethodAllowed(String method) {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.name().equals(method)) {
                return true;
            }
        }
        return false;
    }
}