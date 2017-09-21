package com.abstratt.simpleblobstore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.abstratt.blobstore.BlobMetadata;
import com.abstratt.blobstore.BlobStoreException;
import com.abstratt.blobstore.IBlobStore;

public class SimpleBlobStore implements IBlobStore {
    private Path basePath;

    public SimpleBlobStore(Path basePath) {
        this.basePath = basePath;
    }
    
    @Override
    public BlobMetadata setContents(String token, InputStream contents) throws BlobStoreException {
        Objects.requireNonNull(token, () -> "null token");
        Path blobFile = getBlobFile(token);
        long newSize;
        try {
            Files.copy(contents, blobFile, StandardCopyOption.REPLACE_EXISTING);
            newSize = Files.size(blobFile);
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
        BlobMetadata originalMetadata = getMetadata(token);
        BlobMetadata newMetadata = new BlobMetadata(originalMetadata.getToken(), originalMetadata.getOriginalName(), originalMetadata.getContentType(), newSize);
        saveMetadata(originalMetadata);
        return newMetadata;
    }

    @Override
    public BlobMetadata addBlob(String originalName, String contentType) {
        String token = UUID.randomUUID().toString();
        Path blobFile = getBlobFile(token);
        try {
            Files.createDirectories(blobFile.getParent());
            Files.createFile(blobFile);
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
        BlobMetadata blobMetadata = new BlobMetadata(token, originalName, contentType, 0);
        saveMetadata(blobMetadata);
        return blobMetadata;
    }

    private void saveMetadata(BlobMetadata blobMetadata) {
        Map<String, Object> asMap = new LinkedHashMap<>();
        asMap.put("contentType", blobMetadata.getContentType());
        asMap.put("contentLength", blobMetadata.getContentLength());
        asMap.put("token", blobMetadata.getToken());
        asMap.put("originalName", blobMetadata.getOriginalName());
        Collection<String> metadataLines = asMap.entrySet().stream().map(e -> (e.getKey() + "=" + e.getValue())).collect(Collectors.toList());
        try {
            Files.write(getMetadataFile(blobMetadata.getToken()), metadataLines);
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public InputStream getContents(String token) {
        try {
            return Files.newInputStream(getBlobFile(token));
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public BlobMetadata getMetadata(String token) {
        Path metadataFile = getMetadataFile(token);
        List<String> metadataLines;
        try {
            metadataLines = Files.readAllLines(metadataFile);
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
        Map<Object, Object> asMap = metadataLines.stream().map(it -> it.split("=")).collect(Collectors.toMap(it -> it[0], it -> it[1]));
        String contentType = (String) asMap.get("contentType");
        String originalName = (String) asMap.get("originalName");
        long contentLength = Long.parseLong((String) asMap.get("contentLength"));
        return new BlobMetadata(token, originalName, contentType, contentLength);
    }


    @Override
    public void deleteBlob(String token) {
        try {
            Files.delete(getBlobFile(token));
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    private Path getBlobFile(String token) {
        return getNamespacePath().resolve(token + ".data");
    }

    private Path getNamespacePath() {
        // we ignore the namespace, everybody goes under the same parent dir
        return getBasePath();
    }

    private Path getMetadataFile(String token) {
        return getNamespacePath().resolve(token + ".meta");
    }

    public Path getBasePath() {
        return basePath;
    }
}
