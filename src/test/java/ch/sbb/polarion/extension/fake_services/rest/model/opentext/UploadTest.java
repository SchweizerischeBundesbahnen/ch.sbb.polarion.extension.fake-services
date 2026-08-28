package ch.sbb.polarion.extension.fake_services.rest.model.opentext;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UploadTest {

    @Test
    @SneakyThrows
    void testUploadWithNonEmptyStream() {
        String content = "Hello World!"; // 12 bytes
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Map<String, String> params = Map.of("type", "144", "parent_id", "456", "name", "file.txt");
        Upload upload = Upload.fromValues("file.txt", "123", "456", in, "ticket-1", false, params);

        assertEquals(params, upload.params());
        assertEquals("file.txt", upload.fileName());
        assertEquals("123", upload.nodeId());
        assertEquals("456", upload.parentId());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, upload.size());
        assertEquals("ticket-1", upload.ticket());
        assertFalse(upload.versionUpdate());
    }

    @Test
    @SneakyThrows
    void testUploadWithEmptyStream() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        Upload upload = Upload.fromValues("empty.txt", "n1", "p1", in, "t-2", false, Map.of());

        assertEquals(0, upload.size());
        assertEquals("empty.txt", upload.fileName());
        assertEquals("n1", upload.nodeId());
        assertEquals("p1", upload.parentId());
        assertEquals("t-2", upload.ticket());
        assertFalse(upload.versionUpdate());
    }

    @Test
    @SneakyThrows
    void testUploadWithNullStream() {
        Upload upload = Upload.fromValues("null.bin", "n2", "p2", null, "t-3", false, Map.of());
        assertEquals(0, upload.size());
        assertEquals("null.bin", upload.fileName());
        assertEquals("n2", upload.nodeId());
        assertEquals("p2", upload.parentId());
        assertEquals("t-3", upload.ticket());
        assertFalse(upload.versionUpdate());
    }

    @Test
    @SneakyThrows
    void testVersionUpdateUpload() {
        String content = "New Version"; // 11 bytes
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Upload upload = Upload.fromValues(null, "node-xyz", null, in, "ticket-4", true, Map.of("add_major_version", "true"));

        assertNull(upload.fileName());
        assertEquals("node-xyz", upload.nodeId());
        assertNull(upload.parentId());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, upload.size());
        assertEquals("ticket-4", upload.ticket());
        assertTrue(upload.versionUpdate());
    }

    @Test
    @SuppressWarnings("resource")
    void testSneakyThrowsIOException() {
        // Create a mock InputStream that throws IOException on read
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        // The @SneakyThrows annotation allows checked exceptions to be thrown
        // without being declared or caught. It "sneakily" throws the IOException
        // as if it were an unchecked exception at the bytecode level.
        IOException exception = assertThrows(IOException.class, () ->
                Upload.fromValues("failing.txt", "n3", "p3", failingStream, "t-5", false, Map.of())
        );

        // Verify it's the expected IOException
        assertEquals("Simulated IO error", exception.getMessage());
    }

}
