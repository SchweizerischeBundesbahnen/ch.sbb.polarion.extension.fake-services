package ch.sbb.polarion.extension.fake_services;

import ch.sbb.polarion.extension.generic.GenericUiServlet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakeServicesAppServletTest {

    @Test
    void instantiatesAsGenericUiServlet() {
        FakeServicesAppServlet servlet = new FakeServicesAppServlet();

        assertThat(servlet).isInstanceOf(GenericUiServlet.class);
    }
}
