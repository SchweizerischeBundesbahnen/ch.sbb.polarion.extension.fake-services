package ch.sbb.polarion.extension.fake_services.rest.model.opentext;

import java.util.Map;

/**
 * A container created through the fake OpenText API.
 *
 * @param params every query parameter of the creating request, recorded as received. Nothing is filtered out, so a
 *               caller can assert on parameters this fake knows nothing about.
 */
public record Container(String id, String name, String nodeId, String folder, String ticket, Map<String, String> params) {
}
