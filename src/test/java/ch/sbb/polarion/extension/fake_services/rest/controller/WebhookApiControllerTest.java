package ch.sbb.polarion.extension.fake_services.rest.controller;

import ch.sbb.polarion.extension.fake_services.rest.model.webhook.WebhookCall;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Drives the fake webhook an exporter is pointed at. Two things are asserted: the answer the query asks for, which
 * is what an export reacts to, and the recording of the request, which is what a case asserts afterward.
 */
class WebhookApiControllerTest {

    private static final String HTML = "<html><body><p>Content</p></body></html>";
    private static final String EXPORT_PARAMS = "{\"projectId\":\"elibrary\"}";
    private static final String KEY = "case_1";

    private WebhookApiController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookApiController();
    }

    @Test
    void aCallWithoutParametersIsAnsweredWithTheHtmlItCarried() {
        Response response = respond(KEY, 200, null, 0, 0, null, null, multiPart(HTML));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(HTML, response.getEntity());
    }

    @Test
    void theMarkupToAppendIsInsertedBeforeTheEndOfTheBody() {
        Response response = respond(KEY, 200, null, 0, 0, "<p>Webhook was here</p>", null, multiPart(HTML));

        assertEquals("<html><body><p>Content</p><p>Webhook was here</p></body></html>", response.getEntity());
    }

    @Test
    void markupIsAppendedWhereThePageHasNoBodyEnd() {
        Response response = respond(KEY, 200, null, 0, 0, "<p>Webhook was here</p>", null, multiPart("<p>Content</p>"));

        assertEquals("<p>Content</p><p>Webhook was here</p>", response.getEntity());
    }

    @Test
    void aCallCarryingNoHtmlIsAnsweredWithAnEmptyBody() {
        Response response = respond(KEY, 200, null, 0, 0, null, null, new FormDataMultiPart());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("", response.getEntity());
        assertNull(calls(KEY).getFirst().html());
        assertEquals(0, calls(KEY).getFirst().htmlLength());
    }

    @Test
    void theStatusAndTheBodyOfARefusalAreTheOnesTheQueryAsksFor() {
        Response response = respond(KEY, 503, "The document is not approved", 0, 0, null, null, multiPart(HTML));

        assertEquals(503, response.getStatus());
        assertEquals("The document is not approved", response.getEntity());
    }

    @Test
    void aRefusalWithNeitherBodyNorSizeExplainsNothing() {
        Response response = respond(KEY, 500, null, 0, 0, null, null, multiPart(HTML));

        assertEquals(500, response.getStatus());
        assertEquals("", response.getEntity());
    }

    @Test
    void aGeneratedBodyHasTheAskedForLength() {
        Response response = respond(KEY, 500, null, 700, 0, null, null, multiPart(HTML));

        assertEquals(700, String.valueOf(response.getEntity()).length());
    }

    @Test
    void aNamedBodyWinsOverAGeneratedOne() {
        Response response = respond(KEY, 500, "Refused", 700, 0, null, null, multiPart(HTML));

        assertEquals("Refused", response.getEntity());
    }

    @Test
    void everyCallIsRecordedWithWhatTheRequestCarried() {
        respond(KEY, 200, null, 0, 0, null, "Bearer secret-token", multiPart(HTML));

        WebhookCall call = calls(KEY).getFirst();
        assertEquals(KEY, call.key());
        assertEquals("Bearer secret-token", call.authorization());
        assertEquals(EXPORT_PARAMS, call.exportParams());
        assertEquals(HTML, call.html());
        assertEquals(HTML.length(), call.htmlLength());
        assertEquals(Map.of(), call.params());
    }

    @Test
    void aCallFromAnUnauthenticatedExporterIsRecordedWithoutAuthorization() {
        respond(KEY, 200, null, 0, 0, null, null, multiPart(HTML));

        assertNull(calls(KEY).getFirst().authorization());
    }

    @Test
    void aRefusedCallIsRecordedToo() {
        respond(KEY, 500, "Refused", 0, 0, null, null, multiPart(HTML));

        assertEquals(1, calls(KEY).size());
    }

    @Test
    void fieldsBesidesTheTwoTheFakeReadsAreRecordedAsReceived() {
        FormDataMultiPart multiPart = multiPart(HTML)
                .field("revision", "4711")
                .field("tag", "first")
                .field("tag", "second");

        respond(KEY, 200, null, 0, 0, null, null, multiPart);

        // A field sent more than once is kept whole, joined with a comma
        assertEquals(Map.of("revision", "4711", "tag", "first,second"), calls(KEY).getFirst().params());
    }

    @Test
    void aCallWithoutAnyMultipartBodyIsRecordedEmpty() {
        assertEquals(Response.Status.OK.getStatusCode(), respond(KEY, 200, null, 0, 0, null, null, null).getStatus());

        WebhookCall call = calls(KEY).getFirst();
        assertNull(call.html());
        assertNull(call.exportParams());
        assertEquals(Map.of(), call.params());
    }

    @Test
    void callsAreReadBackPerBucket() {
        respond("case_1", 200, null, 0, 0, null, null, multiPart(HTML));
        respond("case_2", 200, null, 0, 0, null, null, multiPart(HTML));

        assertEquals(1, calls("case_1").size());
        assertEquals(1, calls("case_2").size());
        assertEquals(2, calls(null).size());
    }

    @Test
    void clearingABucketLeavesTheOtherBucketsAlone() {
        respond("case_1", 200, null, 0, 0, null, null, multiPart(HTML));
        respond("case_2", 200, null, 0, 0, null, null, multiPart(HTML));

        Response response = controller.clearCalls("case_1");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(1, entity(response).get("count"));
        assertTrue(calls("case_1").isEmpty());
        assertEquals(1, calls("case_2").size());
    }

    @Test
    void clearingWithoutABucketClearsEverything() {
        respond("case_1", 200, null, 0, 0, null, null, multiPart(HTML));
        respond("case_2", 200, null, 0, 0, null, null, multiPart(HTML));

        Response response = controller.clearCalls(null);

        assertEquals(2, entity(response).get("count"));
        assertTrue(calls(null).isEmpty());
    }

    @Test
    void anAnswerIsHeldBackForTheAskedForTime() {
        long started = System.currentTimeMillis();

        Response response = respond(KEY, 200, null, 0, 300, null, null, multiPart(HTML));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(System.currentTimeMillis() - started >= 300);
    }

    @SuppressWarnings("java:S107") // the endpoint itself takes these parameters, the helper only passes them on
    private Response respond(String key, int status, String body, int bodySize, long delayMillis, String append, String authorization, FormDataMultiPart multiPart) {
        return controller.respond(key, status, body, bodySize, delayMillis, append, authorization, multiPart);
    }

    /**
     * Both exporters send the two fields as streams, so a hand-built request has to carry the same kind of entity.
     */
    private static FormDataMultiPart multiPart(String html) {
        FormDataMultiPart multiPart = new FormDataMultiPart();
        multiPart.bodyPart(streamPart("exportParams", EXPORT_PARAMS));
        multiPart.bodyPart(streamPart("html", html));
        return multiPart;
    }

    private static FormDataBodyPart streamPart(String name, String content) {
        BodyPartEntity entity = mock(BodyPartEntity.class);
        when(entity.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        FormDataBodyPart part = new FormDataBodyPart(name, "replaced by the entity below");
        part.setEntity(entity);
        return part;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Response response) {
        return (Map<String, Object>) response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private List<WebhookCall> calls(String key) {
        return (List<WebhookCall>) controller.getCalls(key).getEntity();
    }
}
