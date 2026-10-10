package com.ascendion.roshan.simple_library.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationSizeLimitFilterTest {

    private final RegistrationSizeLimitFilter filter = new RegistrationSizeLimitFilter();

    private MockHttpServletResponse run(final String method, final String uri, final int bodyBytes) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContent(new byte[bodyBytes]);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void doFilter_smallRegistration_passesThrough() throws Exception {
        assertThat(run("POST", "/apis/v1/registrations", 400).getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_oversizedRegistration_returns413() throws Exception {
        assertThat(run("POST", "/apis/v1/registrations", 5000).getStatus()).isEqualTo(413);
    }

    @Test
    void doFilter_otherPaths_areNotLimited() throws Exception {
        assertThat(run("POST", "/apis/v1/books", 5000).getStatus()).isEqualTo(200);
    }
}
