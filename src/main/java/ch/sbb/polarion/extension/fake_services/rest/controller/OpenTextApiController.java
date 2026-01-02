package ch.sbb.polarion.extension.fake_services.rest.controller;

import ch.sbb.polarion.extension.fake_services.rest.model.opentext.Container;
import ch.sbb.polarion.extension.fake_services.rest.model.opentext.Upload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "Fake OpenText Content Server")
@Path("/api/opentext")
public class OpenTextApiController {

    private static final String HEADER_TICKET_PARAM = "OTCSTICKET";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String TICKET_ERROR = "Invalid or missing ticket";
    private static final String COUNT = "count";

    /**
     * Map username to password for authentication
     */
    private final Map<String, String> users = new HashMap<>();

    /**
     * Map username to ticket for session management
     */
    private final Map<String, String> tickets = new HashMap<>();

    /**
     * Contains created containers
     */
    private final List<Container> containers = new ArrayList<>();

    /**
     * Contains executed uploads
     */
    private final List<Upload> uploads = new ArrayList<>();

    public OpenTextApiController() {
        users.put("test_user", "test_password");
    }

    @POST
    @Path("/api/v1/auth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Authenticate and get ticket",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully authenticated",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Authentication failed"
                    )
            }
    )
    public Response auth(
            @Parameter(description = "Username", required = true) @FormParam("username") String username,
            @Parameter(description = "Password", required = true) @FormParam("password") String password) {
        if (username == null || password == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, "Username and password are required"))
                    .build();
        }

        String storedPassword = users.get(username);
        if (!Objects.equals(storedPassword, password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, "Invalid username or password"))
                    .build();
        }

        String ticket = "TICKET_" + username + "_" + System.currentTimeMillis();
        tickets.put(username, ticket);

        return Response.ok(Map.of("ticket", ticket)).build();
    }

    @POST
    @Path("/api/v1/nodes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload file to container",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully uploaded file",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = TICKET_ERROR
                    )
            }
    )
    @SneakyThrows
    public Response writeFileToContainer(
            @Parameter(description = "Authentication ticket", required = true) @HeaderParam(HEADER_TICKET_PARAM) String ticket,
            @Parameter(description = "Type", required = true) @FormDataParam("type") String type,
            @Parameter(description = "Parent ID", required = true) @FormDataParam("parent_id") String parentId,
            @Parameter(description = "Document name", required = true) @FormDataParam("name") String name,
            @Parameter(description = "File to upload", required = true) @FormDataParam("file") InputStream inputStream) {

        if (isInvalidTicket(ticket)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, TICKET_ERROR))
                    .build();
        }

        String nodeId = "NODE_" + System.currentTimeMillis();
        uploads.add(Upload.fromValues(name, nodeId, parentId, inputStream, ticket, false));

        return Response.ok(Map.of("id", nodeId)).build();
    }

    @POST
    @Path("/api/v2/nodes/{nodeId}/versions/{version}/promote")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Promote version",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully promoted version",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = TICKET_ERROR
                    )
            }
    )
    @SneakyThrows
    public Response promoteVersion(
            @Parameter(description = "Authentication ticket", required = true) @HeaderParam(HEADER_TICKET_PARAM) String ticket,
            @Parameter(description = "Node ID", required = true) @PathParam("nodeId") String nodeId,
            @Parameter(description = "Version", required = true) @PathParam("version") String version) {

        if (isInvalidTicket(ticket)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, TICKET_ERROR))
                    .build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/api/v1/nodes/{nodeId}/versions")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload new file version",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully uploaded new file version",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = TICKET_ERROR
                    )
            }
    )
    @SneakyThrows
    public Response writeNewFileVersion(
            @Parameter(description = "Node ID", required = true) @PathParam("nodeId") String nodeId,
            @Parameter(description = "Authentication ticket", required = true) @HeaderParam(HEADER_TICKET_PARAM) String ticket,
            @Parameter(description = "Add major version", required = true) @FormDataParam("add_major_version") String addMajorVersion,
            @Parameter(description = "File to upload", required = true) @FormDataParam("file") InputStream inputStream) {

        if (isInvalidTicket(ticket)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, TICKET_ERROR))
                    .build();
        }

        uploads.add(Upload.fromValues(null, nodeId, null, inputStream, ticket, true));

        return Response.ok(Map.of("id", nodeId, "version_number", "2.0")).build();
    }

    @GET
    @Path("/api/v1/nodes/{nodeId}/output")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create container or check file existence",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully created container or file existence checked",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = TICKET_ERROR
                    )
            }
    )
    public Response createContainer(
            @Parameter(description = "Authentication ticket", required = true) @HeaderParam(HEADER_TICKET_PARAM) String ticket,
            @Parameter(description = "Destination (folder)") @QueryParam("destination") String destination,
            @Parameter(description = "Name", required = true) @QueryParam("name") String name,
            @Parameter(description = "Container ID") @QueryParam("containerid") String containerId,
            @Parameter(description = "File Name", required = true) @QueryParam("filename") String filename,
            @PathParam("nodeId") @Parameter(description = "Node ID", required = true) String nodeId) {

        if (isInvalidTicket(ticket)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(ERROR, TICKET_ERROR))
                    .build();
        }

        if (destination != null) {
            // New container creation scenario
            String newContainerId = "CONTAINER_" + System.currentTimeMillis();
            containers.add(new Container(newContainerId, name, nodeId, destination, ticket));
            return Response.ok(Map.of("objId", newContainerId, "status", "OK")).build();
        } else if (containerId != null) {
            // Check file existence scenario
            boolean fileExists = uploads.stream().anyMatch(upload -> Objects.equals(upload.fileName(), filename) && Objects.equals(upload.ticket(), ticket));
            return Response.ok(Map.of("documentexists", fileExists)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(ERROR, "Either destination or containerid must be provided"))
                    .build();
        }
    }

    @GET
    @Path("/manage/users")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all users",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved all users",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response getAllUsers() {
        return Response.ok(users).build();
    }

    @POST
    @Path("/manage/users")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add a new user",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully added user",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "User already exists"
                    )
            }
    )
    public Response addUser(
            @Parameter(description = "Username", required = true) @QueryParam("username") String username,
            @Parameter(description = "Password", required = true) @QueryParam("password") String password) {
        if (users.containsKey(username)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(ERROR, "User already exists"))
                    .build();
        }
        users.put(username, password);
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(MESSAGE, "User created successfully", "username", username))
                .build();
    }

    @DELETE
    @Path("/manage/users/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully deleted user",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found"
                    )
            }
    )
    public Response deleteUser(
            @Parameter(description = "Username to delete", required = true) @PathParam("username") String username) {
        if (!users.containsKey(username)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(ERROR, "User not found"))
                    .build();
        }
        users.remove(username);
        return Response.ok(Map.of(MESSAGE, "User deleted successfully", "username", username)).build();
    }

    @GET
    @Path("/manage/uploads")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all uploads",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved all uploads",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response getAllUploads(@Parameter(description = "Specific ticket id") @QueryParam("ticket") String ticket) {
        return Response.ok(ticket == null ? uploads : uploads.stream().filter(upload -> Objects.equals(upload.ticket(), ticket)).toList()).build();
    }

    @DELETE
    @Path("/manage/uploads")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Clear all uploads",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully cleared uploads list",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response clearUploads(@Parameter(description = "Specific ticket id") @QueryParam("ticket") String ticket) {
        int count;
        if (ticket != null) {
            List<Upload> toRemove = uploads.stream()
                    .filter(upload -> Objects.equals(upload.ticket(), ticket)).toList();
            count = toRemove.size();
            toRemove.forEach(uploads::remove);
        } else {
            count = uploads.size();
            uploads.clear();
        }
        return Response.ok(Map.of(MESSAGE, "Uploads cleared successfully", COUNT, count)).build();
    }

    @GET
    @Path("/manage/containers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all containers",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved all containers",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response getAllContainers(@Parameter(description = "Specific ticket id") @QueryParam("ticket") String ticket) {
        return Response.ok(ticket == null ? containers : containers.stream().filter(container -> Objects.equals(container.ticket(), ticket)).toList()).build();
    }

    @DELETE
    @Path("/manage/containers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Clear containers",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully cleared containers list",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response clearContainers(@Parameter(description = "Specific ticket id") @QueryParam("ticket") String ticket) {
        int count;
        if (ticket != null) {
            List<Container> toRemove = containers.stream()
                    .filter(container -> Objects.equals(container.ticket(), ticket)).toList();
            count = toRemove.size();
            toRemove.forEach(containers::remove);
        } else {
            count = containers.size();
            containers.clear();
        }
        return Response.ok(Map.of(MESSAGE, "Containers cleared successfully", COUNT, count)).build();
    }

    @GET
    @Path("/manage/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all tickets",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved all tickets",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response getAllTickets() {
        return Response.ok(tickets).build();
    }

    @DELETE
    @Path("/manage/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Clear tickets",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully cleared tickets list",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON
                            )
                    )
            }
    )
    public Response clearTickets(@Parameter(description = "Specific ticket id") @QueryParam("ticket") String ticket) {
        int count;
        if (ticket != null) {
            List<String> keysToRemove = tickets.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue(), ticket)).map(Map.Entry::getKey).toList();
            count = keysToRemove.size();
            keysToRemove.forEach(tickets::remove);
        } else {
            count = tickets.size();
            tickets.clear();
        }
        return Response.ok(Map.of(MESSAGE, "Ticket(s) cleared successfully", COUNT, count)).build();
    }

    /**
     * Validate if the ticket is invalid (missing or not in tickets map)
     *
     * @param ticket the ticket to validate
     * @return true if ticket is invalid, false otherwise
     */
    private boolean isInvalidTicket(String ticket) {
        return ticket == null || !tickets.containsValue(ticket);
    }
}
