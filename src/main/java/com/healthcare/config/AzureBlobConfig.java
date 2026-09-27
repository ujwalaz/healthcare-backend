package com.healthcare.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

    @Value("${app.azure.storage.connection-string}")
    private String connectionString;

    @Value("${app.azure.storage.container-name}")
    private String containerName;

   @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Provision the configured container during application startup. Without this,
     * the first document upload fails with ContainerNotFound when the storage
     * account has not been initialized yet.
     */
    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        containerClient.createIfNotExists();
        return containerClient;
    }
}
