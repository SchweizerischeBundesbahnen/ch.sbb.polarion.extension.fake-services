package ch.sbb.polarion.extension.fake_services.rest.model.webhook;

import java.util.Map;

/**
 * A call an exporter made to the fake webhook.
 *
 * @param key           the bucket the call was recorded in, so parallel runs do not read each other's calls
 * @param authorization the Authorization header as received, or null where the caller sent none. It is what proves
 *                      which vault record an exporter resolved and how it encoded it
 * @param exportParams  the {@code exportParams} field, as received
 * @param html          the {@code html} field, as received. Kept whole: a chain of webhooks is asserted by what the
 *                      second one was given
 * @param params        every other field of the request, recorded as received. Nothing is filtered by name, so a
 *                      caller can assert on fields this fake knows nothing about
 */
public record WebhookCall(String key, String authorization, String exportParams, String html, int htmlLength, Map<String, String> params) {

    public static WebhookCall fromValues(String key, String authorization, String exportParams, String html, Map<String, String> params) {
        return new WebhookCall(key, authorization, exportParams, html, html == null ? 0 : html.length(), params);
    }
}
