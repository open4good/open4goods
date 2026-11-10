package org.open4goods.nudgerfrontapi.service.css;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.shopping.css.v1.CssProductInput;
import com.google.shopping.css.v1.CssProductInputsServiceClient;
import com.google.shopping.css.v1.CssProductInputsServiceSettings;
import com.google.shopping.css.v1.DeleteCssProductInputRequest;
import com.google.shopping.css.v1.InsertCssProductInputRequest;

/**
 * Real client of the Google CSS API (GOU-2081), based on {@link CssProductInputsServiceClient}. Created only when
 * {@code front.css.sync.enabled=true} and {@code front.css.sync.simulated=false} (see {@link CssSyncConfig}).
 * <p>
 * The service account is read from {@code service-account-path} or {@code service-account-json}, never from the
 * repository.
 * </p>
 */
public class GoogleCssProductsClient implements CssProductsClient, DisposableBean {

    private final CssProductInputsServiceClient client;
    private final String parent;

    /**
     * @param sync synchronisation settings (account, service account, scope)
     * @throws IllegalStateException when the account or the service account is missing or unreadable
     */
    public GoogleCssProductsClient(CssProperties.Sync sync) {
        if (!StringUtils.hasText(sync.getAccountId())) {
            throw new IllegalStateException("front.css.sync.account-id is required when the CSS client is enabled");
        }
        this.parent = "accounts/" + sync.getAccountId().trim();
        try (InputStream credentialsStream = credentialsStream(sync)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(List.of(sync.getScope()));
            this.client = CssProductInputsServiceClient.create(CssProductInputsServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create the Google CSS client", e);
        }
    }

    private static InputStream credentialsStream(CssProperties.Sync sync) throws IOException {
        if (StringUtils.hasText(sync.getServiceAccountPath())) {
            return new FileInputStream(sync.getServiceAccountPath());
        }
        if (StringUtils.hasText(sync.getServiceAccountJson())) {
            return new ByteArrayInputStream(sync.getServiceAccountJson().getBytes(StandardCharsets.UTF_8));
        }
        throw new IllegalStateException(
                "front.css.sync.service-account-path or service-account-json is required when the CSS client is enabled");
    }

    @Override
    public void insert(CssProductInput input) {
        try {
            client.insertCssProductInput(InsertCssProductInputRequest.newBuilder().setParent(parent)
                    .setCssProductInput(input).build());
        } catch (ApiException e) {
            throw new CssClientException(e.getStatusCode().getCode().name(),
                    "CSS insert " + input.getRawProvidedId() + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String rawProvidedId, String contentLanguage, String feedLabel) {
        // Resource id of a CssProductInput: contentLanguage~feedLabel~rawProvidedId
        String name = parent + "/cssProductInputs/" + contentLanguage + "~" + feedLabel + "~" + rawProvidedId;
        try {
            client.deleteCssProductInput(DeleteCssProductInputRequest.newBuilder().setName(name).build());
        } catch (ApiException e) {
            throw new CssClientException(e.getStatusCode().getCode().name(),
                    "CSS delete " + rawProvidedId + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        client.close();
    }
}
