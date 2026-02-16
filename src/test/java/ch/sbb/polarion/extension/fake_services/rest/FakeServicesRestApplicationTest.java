package ch.sbb.polarion.extension.fake_services.rest;

import ch.sbb.polarion.extension.fake_services.rest.controller.OpenTextApiController;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, PlatformContextMockExtension.class})
class FakeServicesRestApplicationTest {

    @Test
    void testInitialization() {
        FakeServicesRestApplication application = new FakeServicesRestApplication();
        assertEquals(1, application.getExtensionControllerClasses().size());
        assertTrue(application.getExtensionControllerClasses().contains(OpenTextApiController.class));
    }

}
