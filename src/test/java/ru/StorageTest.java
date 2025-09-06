package ru;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.config.TestContainersConfig;
import ru.dto.ResourceResponse;
import ru.exception.ResourceAlreadyExistsException;
import ru.exception.ResourceNotFoundException;
import ru.service.StorageService;
import ru.util.PathUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class StorageTest {
    private static final String BUCKET = "user-files";
    private static final int USER_ID = 123;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
        String userDirectory = PathUtils.getUserDirectory(USER_ID);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(userDirectory)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .contentType("application/x-directory")
                            .build());
        } catch (Exception ignored) {}
    }

    @AfterEach
    void cleanUp() {
        try {
            storageService.delete(USER_ID, "");
        } catch (Exception ignored) {}
    }

    @Nested
    class CreateDirectoryTests {
        @Test
        void createDirectory_Success() {
            ResourceResponse response = storageService.createDirectory(USER_ID, "test-dir");
            assertNotNull(response);
            assertEquals("test-dir/", response.name());
        }

        @Test
        void createDirectory_AlreadyExists() {
            storageService.createDirectory(USER_ID, "test-dir");
            assertThatThrownBy(() -> storageService.createDirectory(USER_ID, "test-dir"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void createNestedDirectory_Success() {
            ResourceResponse responseParent = storageService.createDirectory(USER_ID, "parent/");
            ResourceResponse responseChild = storageService.createDirectory(USER_ID, "parent/child/");
            assertNotNull(responseParent);
            assertNotNull(responseChild);
            assertEquals("parent/", responseParent.name());
            assertEquals("child/", responseChild.name());
        }
    }

    @Nested
    class FileTests {
        @Test
        void uploadAndStatFile_Success() throws Exception {
            String filePath = "upload/test.txt";
            storageService.createDirectory(USER_ID, "upload");

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(PathUtils.getFullPath(USER_ID, filePath))
                    .stream(new ByteArrayInputStream("hello".getBytes()), "hello".length(), -1)
                    .contentType("text/plain")
                    .build());

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(BUCKET).object(PathUtils.getFullPath(USER_ID, filePath)).build());

            assertEquals(5, stat.size());
        }
    }

    @Nested
    class MoveTests {
        @Test
        void moveDirectory_Success() throws Exception {
            String from = "dir-to-move/";
            String to = "moved-dir/";

            storageService.createDirectory(USER_ID, from);
            String filePath = from + "test.txt";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(PathUtils.getFullPath(USER_ID, filePath))
                    .stream(new ByteArrayInputStream("hi".getBytes()), 2, -1)
                    .contentType("text/plain")
                    .build());

            ResourceResponse response = storageService.move(USER_ID, from, to);
            assertEquals("moved-dir", response.name());

            assertThrows(ErrorResponseException.class, () ->
                    minioClient.statObject(StatObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(PathUtils.getFullPath(USER_ID, filePath)).build()));
        }

        @Test
        void moveNonExistingDirectory_ShouldFail() {
            assertThatThrownBy(() -> storageService.move(USER_ID, "not-exists/", "new-dir/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void deleteEmptyDirectory_Success() {
            storageService.createDirectory(USER_ID, "empty-dir");
            storageService.delete(USER_ID, "empty-dir");

            assertThatThrownBy(() -> minioClient.statObject(
                    StatObjectArgs.builder().bucket(BUCKET).object(PathUtils.getFullPath(USER_ID, "empty-dir/")).build()
            )).isInstanceOf(ErrorResponseException.class);
        }

        @Test
        void deleteDirectoryWithFiles_Success() throws Exception {
            String dir = "with-files/";
            storageService.createDirectory(USER_ID, dir);
            String filePath = dir + "f1.txt";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(PathUtils.getFullPath(USER_ID, filePath))
                    .stream(new ByteArrayInputStream("abc".getBytes()), 3, -1)
                    .contentType("text/plain")
                    .build());

            storageService.delete(USER_ID, dir);

            assertThatThrownBy(() -> minioClient.statObject(
                    StatObjectArgs.builder().bucket(BUCKET).object(PathUtils.getFullPath(USER_ID, filePath)).build()
            )).isInstanceOf(ErrorResponseException.class);
        }

        @Test
        void deleteNonExistingDirectory_ShouldFail() {
            assertThatThrownBy(() -> storageService.delete(USER_ID, "not-exists"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class SearchTests {
        @Test
        void searchFileByName_Success() throws Exception {
            String dir = "search-test/";
            storageService.createDirectory(USER_ID, dir);

            String match = dir + "my-file.txt";
            String other = dir + "doc.pdf";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(PathUtils.getFullPath(USER_ID, match))
                    .stream(new ByteArrayInputStream("data".getBytes()), 4, -1)
                    .contentType("text/plain")
                    .build());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(PathUtils.getFullPath(USER_ID, other))
                    .stream(new ByteArrayInputStream("data".getBytes()), 4, -1)
                    .contentType("application/pdf")
                    .build());

            List<ResourceResponse> results = storageService.search(USER_ID, "my");
            assertEquals(1, results.size());
            assertEquals("my-file.txt", results.get(0).name());
        }

        @Test
        void searchNoResults_ShouldReturnEmptyList() {
            List<ResourceResponse> results = storageService.search(USER_ID, "zzz");
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    class StressTests {
        @Test
        void createAndDeleteManyFiles() throws Exception {
            String dir = "bulk/";
            storageService.createDirectory(USER_ID, dir);

            for (int i = 0; i < 500; i++) {
                String filePath = dir + "file-" + i + ".txt";
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(PathUtils.getFullPath(USER_ID, filePath))
                        .stream(new ByteArrayInputStream("x".getBytes()), 1, -1)
                        .contentType("text/plain")
                        .build());
            }

            storageService.delete(USER_ID, dir);

            assertThrows(ErrorResponseException.class, () ->
                    minioClient.statObject(StatObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(PathUtils.getFullPath(USER_ID, dir + "file-1.txt")).build()));
        }

        @Test
        void parallelOperations_ShouldNotBreak() throws Exception {
            try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                String dir = "parallel/";
                storageService.createDirectory(USER_ID, dir);

                Runnable upload = () -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            String filePath = dir + "file-" + i + ".txt";
                            minioClient.putObject(PutObjectArgs.builder()
                                    .bucket(BUCKET)
                                    .object(PathUtils.getFullPath(USER_ID, filePath))
                                    .stream(new ByteArrayInputStream("y".getBytes()), 1, -1)
                                    .contentType("text/plain")
                                    .build());
                        }
                    } catch (Exception e) {
                        fail("Upload failed: " + e.getMessage());
                    }
                };

                executor.submit(upload);
                executor.submit(() -> storageService.search(USER_ID, "file"));
                executor.submit(() -> storageService.delete(USER_ID, dir));

                executor.shutdown();
            }
        }
    }
}
