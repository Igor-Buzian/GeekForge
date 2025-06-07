package com.example.spring.service.firebase;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId; // Добавьте этот импорт
import com.google.cloud.storage.BlobInfo; // Добавьте этот импорт
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private final StorageClient storageClient;
    private final String BUCKET_NAME;

    @Autowired
    public FirebaseStorageService(StorageClient storageClient) {
        this.storageClient = storageClient;
        // Получаем имя бакета из инициализированного StorageClient
        this.BUCKET_NAME = storageClient.bucket().getName();
    }

    public String getBucketName() {
        return BUCKET_NAME;
    }



    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        String originalFileName = file.getOriginalFilename();
        // Генерируем уникальное имя файла, чтобы избежать коллизий
        // Важно: folderName должен заканчиваться на "/" если это папка
        String fileNameInStorage = folderName + UUID.randomUUID().toString() + "_" + originalFileName;

        // Загружаем файл
        // Метод create() из Firebase StorageClient
        Blob blob = storageClient.bucket().create(fileNameInStorage, file.getBytes(), file.getContentType());

        // Получаем публичный URL.
        // Для этого нужно сделать файл публичным в правилах Firebase Storage,
        // или использовать Signed URL, если требуется более строгий контроль доступа.
        // В тестовом режиме файлы обычно уже публичны для чтения.
        String publicUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                BUCKET_NAME,
                URLEncoder.encode(fileNameInStorage, StandardCharsets.UTF_8.toString()) // Кодируем путь
        );

        return publicUrl;
    }



    public boolean deleteFile(String filePathInBucket) {
        if (filePathInBucket == null || filePathInBucket.isEmpty()) {
            return false;
        }
        // Для удаления нужен BlobId, который включает имя бакета и полный путь к файлу.
        BlobId blobId = BlobId.of(BUCKET_NAME, filePathInBucket);
        return storageClient.bucket().getStorage().delete(blobId);
    }


    public String extractFilePathFromFirebaseUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            // Формируем базовый URL для текущего бакета
            String baseUrlPart = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/", BUCKET_NAME);

            if (imageUrl.startsWith(baseUrlPart)) {
                String pathWithQuery = imageUrl.substring(baseUrlPart.length());
                int queryIndex = pathWithQuery.indexOf("?");
                String encodedPath = (queryIndex != -1) ? pathWithQuery.substring(0, queryIndex) : pathWithQuery;
                // Декодируем URL, чтобы получить оригинальный путь (например, раскодировать %2F в /)
                return java.net.URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString());
            }
        } catch (Exception e) {
            System.err.println("Error extracting file path from Firebase URL: " + e.getMessage());
        }
        return null;
    }
}
