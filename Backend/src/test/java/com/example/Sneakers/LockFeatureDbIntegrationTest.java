package com.example.Sneakers;

import com.example.Sneakers.controllers.LockFeatureController;
import com.example.Sneakers.dtos.LockFeatureDTO;
import com.example.Sneakers.models.LockFeature;
import com.example.Sneakers.repositories.LockFeatureRepository;
import com.example.Sneakers.services.LockFeatureService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.test.annotation.Commit;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Transactional
@Rollback
class LockFeatureDbIntegrationTest {
    /*
     * Integration test cho module Chuc nang khoa.
     *
     * Test goi truc tiep LockFeatureService va repository voi MySQL that.
     * Cac case create/update/delete co flush/findById/existsById la co CheckDB.
     * @Rollback dam bao lock_features tao trong test khong con trong DB sau test.
     *
     * Cac case kiem tra @PreAuthorize chi dung reflection doc annotation tren controller,
     * khong goi DB nen khong can rollback thuc te.
     *
     * Cach doc nhanh moi test:
     * - Arrange: tao feature hoac request DTO.
     * - Act: goi service/controller.
     * - Assert: kiem tra ket qua va query lai DB neu test co ghi du lieu.
     */
    @Autowired
    private LockFeatureService lockFeatureService;

    @Autowired
    private LockFeatureRepository lockFeatureRepository;

    @Test
    void testGetAllFeatures() {
        // Test Case ID: TC-IT-LOCK-001
        // CheckDB: insert 2 lock_features that, service doc lai bang repository.
        // Rollback: @Transactional + @Rollback xoa du lieu test sau khi test ket thuc.
        LockFeature active = saveFeature("Van tay", true);
        LockFeature inactive = saveFeature("Ma so", false);

        List<LockFeatureDTO> result = lockFeatureService.getAllFeatures();

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(active.getId())));
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(inactive.getId())));
    }

    @Test
    void testGetActiveFeatures() {
        // Test Case ID: TC-IT-LOCK-002
        // Muc tieu: danh sach active chi tra ve feature co isActive=true.
        // CheckDB: tao 1 active va 1 inactive trong bang lock_features.
        // Rollback: ca 2 record test bi rollback.
        LockFeature active = saveFeature("The tu", true);
        LockFeature inactive = saveFeature("Remote khoa", false);

        List<LockFeatureDTO> result = lockFeatureService.getActiveFeatures();

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(active.getId())));
        assertFalse(result.stream().anyMatch(item -> item.getId().equals(inactive.getId())));
    }

    @Test
    void testGetFeatureByIdSuccess() {
        // Test Case ID: TC-IT-LOCK-003
        // Muc tieu: lay chi tiet feature theo ID thanh cong.
        // CheckDB: tao feature that va service doc lai theo id.
        // Rollback: feature test bi rollback.
        LockFeature feature = saveFeature("Remote", true);

        LockFeatureDTO result = lockFeatureService.getFeatureById(feature.getId());

        assertEquals(feature.getId(), result.getId());
        assertEquals(feature.getName(), result.getName());
    }

    @Test
    void testGetFeatureByIdNotFound() {
        // Test Case ID: TC-IT-LOCK-004
        // Muc tieu: ID khong ton tai phai bao loi Feature not found.
        // CheckDB: service query DB nhung khong tim thay ID.
        // Rollback: khong tao du lieu moi.
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> lockFeatureService.getFeatureById(999_999_999L));

        assertEquals("Feature not found", exception.getMessage());
    }

    @Test
    void testCreateFeatureSuccess() {
        // Test Case ID: TC-IT-LOCK-005
        // Muc tieu: tao feature hop le thanh cong.
        // CheckDB: createFeature ghi DB, flush va findById de xac nhan du lieu persist.
        // Rollback: feature test bi rollback.
        // Arrange: tao request hop le.
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name(unique("Bluetooth"))
                .description("Mo khoa bang Bluetooth")
                .isActive(true)
                .build();

        // Act: create feature va flush de day ngay xuong DB.
        LockFeatureDTO result = lockFeatureService.createFeature(request);
        lockFeatureRepository.flush();

        // Assert: doc lai DB xem record vua tao da persist dung chua.
        LockFeature persisted = lockFeatureRepository.findById(result.getId()).orElseThrow();
        assertEquals(request.getName(), persisted.getName());
        assertEquals(true, persisted.getIsActive());
    }

    @Test
    void testUpdateFeatureSuccess() {
        // Test Case ID: TC-IT-LOCK-006
        // Muc tieu: update name/description/isActive thanh cong.
        // CheckDB: tao feature that, update, flush va query lai DB.
        // Rollback: record tao/update bi rollback.
        LockFeature feature = saveFeature("Ten cu", false);
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name(unique("NFC"))
                .description("Mo khoa bang NFC")
                .isActive(true)
                .build();

        LockFeatureDTO result = lockFeatureService.updateFeature(feature.getId(), request);
        lockFeatureRepository.flush();

        LockFeature persisted = lockFeatureRepository.findById(result.getId()).orElseThrow();
        assertEquals(request.getName(), persisted.getName());
        assertEquals(true, persisted.getIsActive());
    }

    @Test
    void testDeleteFeatureSuccess() {
        // Test Case ID: TC-IT-LOCK-007
        // Muc tieu: xoa feature thanh cong.
        // CheckDB: tao feature that, delete, flush va existsById=false.
        // Rollback: transaction test rollback nen DB that van sach sau test.
        LockFeature feature = saveFeature("Xoa feature", true);

        lockFeatureService.deleteFeature(feature.getId());
        lockFeatureRepository.flush();

        assertFalse(lockFeatureRepository.existsById(feature.getId()));
    }

    @Test
    void testUpdateFeatureNotFound() {
        // Test Case ID: TC-IT-LOCK-008
        // Muc tieu: update ID khong ton tai phai bao loi.
        // CheckDB: service query DB theo ID khong ton tai.
        // Rollback: khong tao du lieu moi.
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name("NFC")
                .description("Mo khoa bang NFC")
                .isActive(true)
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> lockFeatureService.updateFeature(999_999_999L, request));

        assertEquals("Feature not found", exception.getMessage());
    }

    @Test
    void testCreateFeatureEmptyNameBug() {
        // Test Case ID: TC-IT-LOCK-009
        // Expected: name rong phai bi chan validation. Source hien tai van save nen test fail that neu bug con ton tai.
        // Actual neu test fail: createFeature van luu record co name rong.
        // Root cause: service/DTO chua co @NotBlank hoac check StringUtils.hasText(name).
        // CheckDB: co goi createFeature va flush de bat loi DB/validation neu co.
        // Rollback: record loi neu duoc luu se bi rollback.
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name("")
                .description("Mo ta")
                .isActive(true)
                .build();

        assertThrows(RuntimeException.class, () -> {
            lockFeatureService.createFeature(request);
            lockFeatureRepository.flush();
        });
    }

    @Test
    void testCreateFeatureNullActiveBug() {
        // Test Case ID: TC-IT-LOCK-010
        // Expected: isActive null phai bi chan validation hoac default ro rang.
        // Actual neu test fail: createFeature van tao record khi isActive null.
        // Root cause: DTO/service chua bat buoc isActive hoac chua gan default ro rang.
        // CheckDB: flush xuong DB de xac nhan loi co bi chan hay khong.
        // Rollback: record test bi rollback.
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name(unique("Mat ma"))
                .description("Mo khoa bang mat ma")
                .isActive(null)
                .build();

        assertThrows(RuntimeException.class, () -> {
            lockFeatureService.createFeature(request);
            lockFeatureRepository.flush();
        });
    }

    @Test
    void testCreateFeatureDuplicateNameBug() {
        // Test Case ID: TC-IT-LOCK-011
        // Expected: ten trung phai bi chan. Source hien tai cho tao trung nen test fail that neu bug con ton tai.
        // Actual neu test fail: DB/service cho phep tao 2 feature cung name.
        // Root cause: thieu unique constraint hoac check existsByName truoc khi save.
        // CheckDB: tao record dau tien that, sau do thu tao record thu hai trung name.
        // Rollback: ca 2 record test bi rollback.
        String duplicatedName = unique("Van tay trung");
        saveFeature(duplicatedName, true);
        LockFeatureDTO request = LockFeatureDTO.builder()
                .name(duplicatedName)
                .description("Trung ten")
                .isActive(true)
                .build();

        assertThrows(RuntimeException.class, () -> {
            lockFeatureService.createFeature(request);
            lockFeatureRepository.flush();
        });
    }

    @Test
    void testUserCannotCreateFeature() throws Exception {
        // Test Case ID: TC-IT-LOCK-012
        // Muc tieu: controller createFeature phai gioi han quyen ADMIN.
        // CheckDB: khong dung DB, chi doc annotation @PreAuthorize bang reflection.
        // Rollback: khong can vi khong ghi DB.
        Method method = LockFeatureController.class.getMethod("createFeature", LockFeatureDTO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ROLE_ADMIN')", preAuthorize.value());
    }

    @Test
    void testUserCannotUpdateFeature() throws Exception {
        // Test Case ID: TC-IT-LOCK-013
        // Muc tieu: controller updateFeature phai gioi han quyen ADMIN.
        // CheckDB: khong dung DB, chi kiem tra annotation.
        // Rollback: khong can.
        Method method = LockFeatureController.class.getMethod("updateFeature", Long.class, LockFeatureDTO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ROLE_ADMIN')", preAuthorize.value());
    }

    @Test
    void testUserCannotDeleteFeature() throws Exception {
        // Test Case ID: TC-IT-LOCK-014
        // Muc tieu: controller deleteFeature phai gioi han quyen ADMIN.
        // CheckDB: khong dung DB, chi kiem tra annotation.
        // Rollback: khong can.
        Method method = LockFeatureController.class.getMethod("deleteFeature", Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ROLE_ADMIN')", preAuthorize.value());
    }

    @Test
    void testTransactionalRollbackReallyRemovesDbRows() {
        // Test Case ID: TC-IT-LOCK-015
        // Evidence: ket thuc transaction test bang rollback roi query lai DB that.
        // Muc tieu: chung minh rollback that su xoa du lieu test khoi MySQL.
        // CheckDB: insert feature, assert exists, ket thuc transaction rollback, query existsById lai.
        // Rollback: dung TestTransaction de rollback ngay trong test thay vi doi ket thuc test.
        LockFeature feature = saveFeature("Rollback evidence", true);
        Long featureId = feature.getId();
        assertTrue(lockFeatureRepository.existsById(featureId));

        TestTransaction.flagForRollback();
        TestTransaction.end();

        assertFalse(lockFeatureRepository.existsById(featureId));
    }

    private LockFeature saveFeature(String prefix, boolean active) {
        // Helper tao lock_features that trong DB va flush ngay.
        LockFeature feature = LockFeature.builder()
                .name(unique(prefix))
                .description("Feature DB rollback")
                .isActive(active)
                .build();
        return lockFeatureRepository.saveAndFlush(feature);
    }

    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
}
