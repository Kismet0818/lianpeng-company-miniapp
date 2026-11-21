package com.example.lpsx.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.OssService;

class OssControllerTest {

    private OssController controller;
    private AtomicReference<String> uploadedFolder;

    @BeforeEach
    void setUp() {
        uploadedFolder = new AtomicReference<>();
        OssService ossService = (OssService) Proxy.newProxyInstance(
                OssService.class.getClassLoader(), new Class<?>[] { OssService.class },
                (proxy, method, args) -> {
                    if ("uploadBatch".equals(method.getName())) {
                        uploadedFolder.set((String) args[1]);
                        return List.of("https://example.test/temp-miniapp-icons/icon.png");
                    }
                    if ("toString".equals(method.getName())) return "OssServiceTestProxy";
                    throw new UnsupportedOperationException(method.getName());
                });
        controller = new OssController();
        ReflectionTestUtils.setField(controller, "ossService", ossService);
    }

    @Test
    void temporaryBatchAcceptsImagesAndUsesFixedFolder() {
        MultipartFile image = new MockMultipartFile(
                "files", "icon.png", "image/png", new byte[] { 1, 2, 3 });

        Result<List<String>> result = controller.uploadTemporaryIcons(List.of(image));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        assertThat(uploadedFolder.get()).isEqualTo("temp-miniapp-icons");
    }

    @Test
    void temporaryBatchRejectsNonImageBeforeUpload() {
        MultipartFile pdf = new MockMultipartFile(
                "files", "icon.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        Result<List<String>> result = controller.uploadTemporaryIcons(List.of(pdf));

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(uploadedFolder.get()).isNull();
    }
}
