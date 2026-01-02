package ch.sbb.polarion.extension.fake_services.rest.model.opentext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {

    @Test
    void testConstructor() {
        String id = "123";
        String name = "Test Container";
        String nodeId = "node-456";
        String folder = "/test/folder";
        String ticket = "ticket-789";

        Container container = new Container(id, name, nodeId, folder, ticket);

        assertEquals(id, container.id());
        assertEquals(name, container.name());
        assertEquals(nodeId, container.nodeId());
        assertEquals(folder, container.folder());
        assertEquals(ticket, container.ticket());
    }

    @Test
    void testEquals_equalObjects() {
        Container container1 = new Container("1", "Name", "node1", "/folder", "ticket1");
        Container container2 = new Container("1", "Name", "node1", "/folder", "ticket1");

        assertEquals(container1, container2);
        assertEquals(container2, container1);
    }

    @Test
    void testEquals_differentObjects() {
        Container container1 = new Container("1", "Name", "node1", "/folder", "ticket1");
        Container container2 = new Container("2", "Name", "node1", "/folder", "ticket1");

        assertNotEquals(container1, container2);
    }

    @Test
    void testEquals_nullObject() {
        Container container = new Container("1", "Name", "node1", "/folder", "ticket1");

        assertNotEquals(null, container);
    }

    @Test
    void testHashCode_equalObjects() {
        Container container1 = new Container("1", "Name", "node1", "/folder", "ticket1");
        Container container2 = new Container("1", "Name", "node1", "/folder", "ticket1");

        assertEquals(container1.hashCode(), container2.hashCode());
    }

    @Test
    void testToString() {
        Container container = new Container("123", "TestName", "node456", "/test", "ticket789");

        String result = container.toString();

        assertNotNull(result);
        assertTrue(result.contains("123"));
        assertTrue(result.contains("TestName"));
        assertTrue(result.contains("node456"));
        assertTrue(result.contains("/test"));
        assertTrue(result.contains("ticket789"));
    }

    @Test
    void testWithNullValues() {
        Container container = new Container(null, null, null, null, null);

        assertNull(container.id());
        assertNull(container.name());
        assertNull(container.nodeId());
        assertNull(container.folder());
        assertNull(container.ticket());
    }

    @Test
    void testWithEmptyStrings() {
        Container container = new Container("", "", "", "", "");

        assertEquals("", container.id());
        assertEquals("", container.name());
        assertEquals("", container.nodeId());
        assertEquals("", container.folder());
        assertEquals("", container.ticket());
    }

    @Test
    void testPartialNullValues() {
        Container container = new Container("1", null, "node1", null, "ticket1");

        assertEquals("1", container.id());
        assertNull(container.name());
        assertEquals("node1", container.nodeId());
        assertNull(container.folder());
        assertEquals("ticket1", container.ticket());
    }

}
