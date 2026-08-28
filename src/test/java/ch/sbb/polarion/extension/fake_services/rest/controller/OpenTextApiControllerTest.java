package ch.sbb.polarion.extension.fake_services.rest.controller;

import ch.sbb.polarion.extension.fake_services.rest.model.opentext.Container;
import ch.sbb.polarion.extension.fake_services.rest.model.opentext.Upload;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Drives the OpenText endpoints which record what a client sent. The point of these tests is the recording itself:
 * the fake names no parameter of its own, so a caller can assert on parameters this extension knows nothing about.
 */
class OpenTextApiControllerTest {

    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "test_password";

    private OpenTextApiController controller;
    private String ticket;

    @BeforeEach
    void setUp() {
        controller = new OpenTextApiController();
        ticket = String.valueOf(entity(controller.auth(TEST_USER, TEST_PASSWORD)).get("ticket"));
    }

    @Test
    void createContainerRecordsEveryQueryParameter() {
        MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle("destination", "prod_folder");
        query.putSingle("name", "project_DOC-1");
        // Parameters this fake knows nothing about have to be recorded all the same
        query.putSingle("objectType", "CompoundDocument");
        query.putSingle("TMSID", "ref-4711");
        query.put("Release", List.of("2026.1", "2026.2"));

        Response response = controller.createContainer(ticket, "prod_folder", "project_DOC-1", null, null, "NODE_NUM_1", uriInfo(query));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Container container = containers().get(0);
        assertEquals("NODE_NUM_1", container.nodeId());
        assertEquals("prod_folder", container.folder());
        assertEquals(Map.of(
                "destination", "prod_folder",
                "name", "project_DOC-1",
                "objectType", "CompoundDocument",
                "TMSID", "ref-4711",
                // A parameter sent more than once is kept whole, joined with a comma
                "Release", "2026.1,2026.2"
        ), container.params());
    }

    @Test
    void createContainerRecordsNothingWhenTheRequestCarriesNoQueryParameters() {
        Response response = controller.createContainer(ticket, "prod_folder", "name", null, null, "NODE_NUM_1", uriInfo(new MultivaluedHashMap<>()));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(Map.of(), containers().get(0).params());
    }

    @Test
    void writeFileToContainerRecordsEveryFormFieldExceptTheFile() {
        byte[] content = "a pretend PDF".getBytes(StandardCharsets.UTF_8);
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("type", "144")
                .field("parent_id", "container_42")
                .field("name", "TestUpload");
        multiPart.bodyPart(filePart(content));

        Response response = controller.writeFileToContainer(ticket, multiPart);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Upload upload = uploads().get(0);
        assertEquals("TestUpload", upload.fileName());
        assertEquals("container_42", upload.parentId());
        assertFalse(upload.versionUpdate());
        // The file is left out of the parameters, its size is stored on the upload instead
        assertEquals(content.length, upload.size());
        assertFalse(upload.params().containsKey("file"));
        assertEquals(Map.of("type", "144", "parent_id", "container_42", "name", "TestUpload"), upload.params());
    }

    @Test
    void repeatedFormFieldsAreKeptWhole() {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("name", "TestUpload")
                .field("tag", "first")
                .field("tag", "second");

        controller.writeFileToContainer(ticket, multiPart);

        assertEquals("first,second", uploads().get(0).params().get("tag"));
    }

    @Test
    void anUploadWithoutAFilePartIsRecordedWithSizeZero() {
        FormDataMultiPart multiPart = new FormDataMultiPart().field("name", "TestUpload");

        controller.writeFileToContainer(ticket, multiPart);

        Upload upload = uploads().get(0);
        assertEquals(0, upload.size());
        assertEquals(Map.of("name", "TestUpload"), upload.params());
    }

    @Test
    void anUploadWithoutAnyMultipartBodyIsRecordedEmpty() {
        Response response = controller.writeFileToContainer(ticket, null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Upload upload = uploads().get(0);
        assertEquals(Map.of(), upload.params());
        assertEquals(0, upload.size());
        assertNull(upload.fileName());
        assertNull(upload.parentId());
    }

    @Test
    void writeNewFileVersionRecordsItsFormFields() {
        byte[] content = "a newer pretend PDF".getBytes(StandardCharsets.UTF_8);
        FormDataMultiPart multiPart = new FormDataMultiPart().field("add_major_version", "true");
        multiPart.bodyPart(filePart(content));

        Response response = controller.writeNewFileVersion("id_17", ticket, multiPart);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Upload upload = uploads().get(0);
        assertTrue(upload.versionUpdate());
        assertEquals("id_17", upload.nodeId());
        assertNull(upload.fileName());
        assertNull(upload.parentId());
        assertEquals(content.length, upload.size());
        assertEquals(Map.of("add_major_version", "true"), upload.params());
    }

    @Test
    void anInvalidTicketRecordsNothing() {
        Response upload = controller.writeFileToContainer("no-such-ticket", new FormDataMultiPart().field("name", "TestUpload"));
        Response container = controller.createContainer("no-such-ticket", "prod_folder", "name", null, null, "NODE_NUM_1", uriInfo(new MultivaluedHashMap<>()));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), upload.getStatus());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), container.getStatus());
        // Unfiltered on purpose: the ticket-filtered view would hide a record stored under the rejected ticket, so
        // this would still pass if the recording ever moved above the ticket check
        assertTrue(((List<?>) controller.getAllUploads(null).getEntity()).isEmpty());
        assertTrue(((List<?>) controller.getAllContainers(null).getEntity()).isEmpty());
    }

    /**
     * Jersey materialises the file of a parsed request as a {@link BodyPartEntity}, which is what the endpoint reads
     * the content from. A hand-built part has to carry the same kind of entity.
     */
    private static FormDataBodyPart filePart(byte[] content) {
        BodyPartEntity fileEntity = mock(BodyPartEntity.class);
        when(fileEntity.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        FormDataBodyPart filePart = new FormDataBodyPart("file", "replaced by the entity below");
        filePart.setEntity(fileEntity);
        return filePart;
    }

    private static UriInfo uriInfo(MultivaluedMap<String, String> queryParameters) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        return uriInfo;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Response response) {
        return (Map<String, Object>) response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private List<Container> containers() {
        return (List<Container>) controller.getAllContainers(ticket).getEntity();
    }

    @SuppressWarnings("unchecked")
    private List<Upload> uploads() {
        return (List<Upload>) controller.getAllUploads(ticket).getEntity();
    }
}
