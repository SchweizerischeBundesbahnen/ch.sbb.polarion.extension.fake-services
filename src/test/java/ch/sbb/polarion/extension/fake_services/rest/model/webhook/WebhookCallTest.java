package ch.sbb.polarion.extension.fake_services.rest.model.webhook;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebhookCallTest {

    @Test
    void theLengthOfTheHtmlIsStoredBesideIt() {
        String html = "<html><body><p>Content</p></body></html>";
        Map<String, String> params = Map.of("revision", "4711");

        WebhookCall call = WebhookCall.fromValues("case_1", "Basic dXNlcjpwYXNz", "{\"projectId\":\"elibrary\"}", html, params);

        assertEquals("case_1", call.key());
        assertEquals("Basic dXNlcjpwYXNz", call.authorization());
        assertEquals("{\"projectId\":\"elibrary\"}", call.exportParams());
        assertEquals(html, call.html());
        assertEquals(html.length(), call.htmlLength());
        assertEquals(params, call.params());
    }

    @Test
    void aCallCarryingNoHtmlHasLengthZero() {
        WebhookCall call = WebhookCall.fromValues("case_1", null, null, null, Map.of());

        assertNull(call.html());
        assertNull(call.authorization());
        assertNull(call.exportParams());
        assertEquals(0, call.htmlLength());
    }
}
