package ch.sbb.polarion.extension.fake_services.rest.controller;

import ch.sbb.polarion.extension.fake_services.rest.model.webhook.WebhookCall;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A webhook an exporter can be pointed at.
 * <p>
 * The PDF and the DOCX exporter send the same request - a multipart form of {@code exportParams} and {@code html},
 * expecting the body of the answer to replace the HTML - so one fake serves both. What a case needs is a webhook
 * which answers in a given way, not one which knows who called it: the answer is chosen by the query of the URL
 * stored in the configuration, and every call is recorded so the request itself can be asserted afterward.
 */
@Singleton
@Tag(name = "Fake Webhook")
@Path("/api/webhook")
public class WebhookApiController {

    private static final String FIELD_EXPORT_PARAMS = "exportParams";
    private static final String FIELD_HTML = "html";
    private static final String MESSAGE = "message";
    private static final String COUNT = "count";
    private static final String DEFAULT_KEY = "default";
    private static final String BODY_FILLER = "X";
    private static final String MULTIPART_BODY_DESCRIPTION = "The multipart form an exporter sends: exportParams and html. Every field is recorded as received, the fake imposes no set of field names.";

    /**
     * Every call this fake answered, newest last.
     */
    private final List<WebhookCall> calls = new CopyOnWriteArrayList<>();

    @POST
    @Path("/respond")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Answer an export webhook call the way the query asks",
            description = """
                    With no parameters the call is answered with the HTML it carried, which is what an export that \
                    passes through a webhook unchanged looks like. The parameters make the webhook refuse, stall or \
                    change the HTML instead.""",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The HTML the export carries on with",
                            content = @Content(mediaType = MediaType.TEXT_PLAIN)
                    ),
                    @ApiResponse(
                            description = "The status the query asked for, with the body it asked for",
                            content = @Content(mediaType = MediaType.TEXT_PLAIN)
                    )
            }
    )
    @RequestBody(description = MULTIPART_BODY_DESCRIPTION, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(type = "object")))
    @SuppressWarnings("java:S2925") // a webhook which takes its time is what the stalling cases need
    public Response respond(
            @Parameter(description = "Bucket the call is recorded in") @QueryParam("key") @DefaultValue(DEFAULT_KEY) String key,
            @Parameter(description = "Status to answer with") @QueryParam("status") @DefaultValue("200") int status,
            @Parameter(description = "Body to answer with where the status is not 200") @QueryParam("body") String body,
            @Parameter(description = "Length of a generated body, used where no body is given") @QueryParam("bodySize") @DefaultValue("0") int bodySize,
            @Parameter(description = "Milliseconds to wait before answering") @QueryParam("delayMillis") @DefaultValue("0") long delayMillis,
            @Parameter(description = "Markup inserted before </body> of the HTML the webhook answers with") @QueryParam("append") String append,
            @Parameter(description = "Authorization header, recorded with the call", hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @Parameter(hidden = true) FormDataMultiPart multiPart) {

        String html = readField(multiPart, FIELD_HTML);
        calls.add(WebhookCall.fromValues(key, authorization, readField(multiPart, FIELD_EXPORT_PARAMS), html, collectOtherFields(multiPart)));

        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Interrupted while waiting").build();
            }
        }

        if (status == Response.Status.OK.getStatusCode()) {
            return Response.ok(insertBeforeBodyEnd(html == null ? "" : html, append)).build();
        }
        return Response.status(status).entity(errorBody(body, bodySize)).build();
    }

    @GET
    @Path("/manage/calls")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the recorded calls",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the recorded calls",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON)
                    )
            }
    )
    public Response getCalls(@Parameter(description = "Specific bucket") @QueryParam("key") String key) {
        return Response.ok(key == null ? List.copyOf(calls) : calls.stream().filter(call -> Objects.equals(call.key(), key)).toList()).build();
    }

    @DELETE
    @Path("/manage/calls")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Clear the recorded calls",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully cleared the recorded calls",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON)
                    )
            }
    )
    public Response clearCalls(@Parameter(description = "Specific bucket") @QueryParam("key") String key) {
        int count;
        if (key != null) {
            List<WebhookCall> toRemove = calls.stream().filter(call -> Objects.equals(call.key(), key)).toList();
            count = toRemove.size();
            calls.removeAll(toRemove);
        } else {
            count = calls.size();
            calls.clear();
        }
        return Response.ok(Map.of(MESSAGE, "Calls cleared successfully", COUNT, count)).build();
    }

    /**
     * The body of a refusal: the one the query named, or one of the asked-for length. An empty body is a refusal
     * which explains nothing, which is a case of its own on the calling side.
     */
    private String errorBody(String body, int bodySize) {
        if (body != null) {
            return body;
        }
        return bodySize > 0 ? BODY_FILLER.repeat(bodySize) : "";
    }

    /**
     * Puts the markup where a document carries it: before the end of the body, so what a webhook adds is rendered
     * rather than left after the end of the document. A page with no body end gets it appended.
     */
    private String insertBeforeBodyEnd(String html, String append) {
        if (append == null || append.isEmpty()) {
            return html;
        }
        int bodyEnd = html.lastIndexOf("</body>");
        return bodyEnd < 0 ? html + append : html.substring(0, bodyEnd) + append + html.substring(bodyEnd);
    }

    /**
     * The content of a field as text. The exporters send both fields as streams, so the entity is read rather than
     * converted.
     */
    @SneakyThrows
    private String readField(FormDataMultiPart multiPart, String name) {
        FormDataBodyPart part = multiPart == null ? null : multiPart.getField(name);
        if (part == null) {
            return null;
        }
        if (part.getEntity() instanceof BodyPartEntity entity) {
            try (InputStream inputStream = entity.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return part.getValue();
    }

    /**
     * Records every field of the request except the two this fake reads by name. Nothing is filtered, so a caller
     * can assert on fields this extension knows nothing about.
     */
    private Map<String, String> collectOtherFields(FormDataMultiPart multiPart) {
        Map<String, String> params = new LinkedHashMap<>();
        if (multiPart != null) {
            multiPart.getFields().forEach((key, parts) -> {
                if (!FIELD_HTML.equals(key) && !FIELD_EXPORT_PARAMS.equals(key)) {
                    params.put(key, parts.stream().map(FormDataBodyPart::getValue).collect(Collectors.joining(",")));
                }
            });
        }
        return params;
    }
}
