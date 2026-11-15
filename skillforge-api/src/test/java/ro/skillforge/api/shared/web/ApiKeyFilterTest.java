package ro.skillforge.api.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test unitar, fără Spring: ce căi lasă filtrul libere și ce cere pentru restul.
 */
class ApiKeyFilterTest {
    private final ApiKeyFilter filter = new ApiKeyFilter("test-key");

    @Test
    void healthIsOpenForTheProbes() throws Exception {
        assertEquals(200, statusFor("/actuator/health/readiness", null));
    }

    @Test
    void metricsAndInfoNeedTheKey() throws Exception {
        assertEquals(401, statusFor("/actuator/metrics", null));
        assertEquals(401, statusFor("/actuator/info", null));
    }

    @Test
    void documentationIsOpen() throws Exception {
        assertEquals(200, statusFor("/v3/api-docs", null));
    }

    @Test
    void dataNeedsTheRightKey() throws Exception {
        assertEquals(401, statusFor("/conversations", null));
        assertEquals(401, statusFor("/conversations", "ghicit"));
        assertEquals(200, statusFor("/conversations", "test-key"));
    }

    @Test
    void withoutAConfiguredKeyEverythingIsClosed() throws Exception {
        ApiKeyFilter unconfigured = new ApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/conversations");
        request.addHeader(ApiKeyFilter.API_KEY_HEADER, "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        unconfigured.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
    }

    // 200 înseamnă că cererea a trecut de filtru mai departe, în lanț.
    private int statusFor(String path, String apiKey) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        if (apiKey != null) {
            request.addHeader(ApiKeyFilter.API_KEY_HEADER, apiKey);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        if (response.getStatus() == 200) {
            assertNotNull(chain.getRequest(), "cererea a trecut de filtru");
        }
        return response.getStatus();
    }
}
