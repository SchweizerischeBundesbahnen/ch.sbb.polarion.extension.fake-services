package ch.sbb.polarion.extension.fake_services.rest.model.opentext;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.InputStream;
import java.util.Map;

/**
 * A file uploaded through the fake OpenText API.
 *
 * @param params every form field of the uploading request except the file itself, recorded as received. Nothing is
 *               filtered out, so a caller can assert on fields this fake knows nothing about.
 */
public record Upload(String fileName, String nodeId, String parentId, long size, String ticket, boolean versionUpdate, Map<String, String> params) {

    @SneakyThrows
    public static Upload fromValues(String fileName, String nodeId, String parentId, InputStream size, String ticket, boolean versionUpdate, Map<String, String> params) {
        return new Upload(fileName, nodeId, parentId, size == null ? 0 : IOUtils.copy(size, NullOutputStream.INSTANCE), ticket, versionUpdate, params);
    }

}
