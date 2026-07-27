package ch.sbb.polarion.extension.fake_services;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

/**
 * Serves the React single-page app from the third webapp context ({@code fake-services-app}). The
 * admin extenders in hivemodule.xml open it as
 * {@code /polarion/fake-services-app/ui/app/index.html?feature=<id>}; everything else about the
 * request handling comes from the generic servlet.
 */
public class FakeServicesAppServlet extends GenericUiServlet {

    @Serial
    private static final long serialVersionUID = 6893052734118250371L;

    public FakeServicesAppServlet() {
        super("fake-services-app");
    }
}
