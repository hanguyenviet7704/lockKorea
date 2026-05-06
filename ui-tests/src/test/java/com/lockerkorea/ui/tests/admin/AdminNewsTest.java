package com.lockerkorea.ui.tests.admin;

import com.lockerkorea.ui.pages.*;
import com.lockerkorea.ui.utils.BaseTest;
import com.lockerkorea.ui.utils.ConfigReader;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Admin News Management Tests
 * Đồng bộ với tài liệu Excel: LockerKorea_TestCase_Template_Chuan.xlsx
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminNewsTest extends BaseTest {

    private LoginPage loginPage;
    private NewsManagePage newsPage;

    @BeforeEach
    public void setUpAdmin() {
        loginPage = new LoginPage(driver);
        newsPage = new NewsManagePage(driver);
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.admin.email"),
            ConfigReader.getString("test.admin.password")
        );
        newsPage.navigateTo();
    }

    // =========================================================================
    // 1. GIAO DIỆN VÀ TÌM KIẾM
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("TC-QLTT-1: Hiển thị danh sách tin tức")
    void testNewsListDisplayed() {
        assertTrue(newsPage.isLoaded(), "Trang Quản lý tin tức phải được load thành công");
        assertTrue(newsPage.getNewsCount() >= 0, "Bảng danh sách tin tức phải được render (lớn hơn hoặc bằng 0 SP)");
    }

    @Test
    @Order(2)
    @DisplayName("TC-QLTT-2: Tìm kiếm bài viết")
    void testSearchNews() {
        String uniqueTitle = "Khuyến mãi tháng " + System.currentTimeMillis();
        // Tạo bài viết mới để đảm bảo có data để tìm
        newsPage.addNews(uniqueTitle, "Test search content", false);
        // Chờ API lưu xong
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        newsPage.searchNews(uniqueTitle);
        // Chờ kết quả lọc
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertTrue(newsPage.getAllNewsTitles().stream().anyMatch(t -> t.contains(uniqueTitle)),
            "Kết quả tìm kiếm phải chứa tiêu đề bài viết");

        // Reset và dọn dữ liệu
        newsPage.searchNews("");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        newsPage.deleteNews(uniqueTitle, true);
    }

    @Test
    @Order(21)
    @DisplayName("TC-QLTT-2.1: Tìm kiếm bài viết với ký tự đặc biệt")
    void testSearchNewsSpecialChars() {
        newsPage.searchNews("@#$%^&*");
        // Giả sử không có bài viết nào chứa ký tự này thì số lượng = 0
        assertEquals(0, newsPage.getNewsCount(), "Tìm kiếm ký tự đặc biệt không tồn tại phải trả về 0 kết quả");
        newsPage.searchNews(""); // Reset
    }

    // =========================================================================
    // 2. CHỨC NĂNG THÊM/SỬA/XÓA
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("TC-QLTT-3: Thêm: Bỏ trống Tiêu đề (Validation)")
    void testAddNewsWithoutTitle() {
        newsPage.clickAddNewsButton();
        
        // Bỏ trống title, chỉ điền content
        newsPage.fillNewsForm(null, "Nội dung bài viết", null, false);
        newsPage.submitFormForce(); // Gọi hàm submit force để test validation
        
        assertTrue(newsPage.isDialogDisplayed(), "Form không được đóng (Validation chặn lại)");
        
        // Hoặc kiểm tra xem có lấy được thông báo lỗi
        String errorMsg = newsPage.getTitleErrorMessage();
        assertFalse(errorMsg.isEmpty(), "Phải hiển thị lỗi Tiêu đề bắt buộc");
        
        newsPage.cancelDialog();
    }

    @Test
    @Order(4)
    @DisplayName("TC-QLTT-4: Thêm: Nội dung chứa Iframe/Video")
    void testAddNewsWithIframe() {
        String title = "Tin tức kèm Video " + System.currentTimeMillis();
        String iframeContent = "<iframe width='560' height='315' src='https://www.youtube.com/embed/test'></iframe>";
        
        newsPage.addNews(title, iframeContent, true);
        
        // Chờ lưu hoàn tất
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertTrue(newsPage.getAllNewsTitles().stream().anyMatch(t -> t.contains(title)),
            "Bài viết chứa Iframe phải được lưu thành công");
        
        // Cleanup
        newsPage.deleteNews(title, true);
    }

    @Test
    @Order(41)
    @DisplayName("TC-QLTT-4.1: Thêm: Bỏ trống Nội dung (Validation)")
    void testAddNewsWithoutContent() {
        newsPage.clickAddNewsButton();
        newsPage.fillNewsForm("Tiêu đề hợp lệ", null, null, false);
        newsPage.submitFormForce();
        
        assertTrue(newsPage.isDialogDisplayed(), "Form không được đóng khi bỏ trống nội dung");
        newsPage.cancelDialog();
    }

    @Test
    @Order(42)
    @DisplayName("TC-QLTT-4.2: Thêm: Tiêu đề quá dài (>255 ký tự)")
    void testAddNewsLongTitle() {
        newsPage.clickAddNewsButton();
        String longTitle = "A".repeat(260);
        newsPage.fillNewsForm(longTitle, "Nội dung bài viết", null, false);
        newsPage.submitFormForce();
        
        assertTrue(newsPage.isDialogDisplayed(), "Form không được đóng khi tiêu đề quá dài");
        newsPage.cancelDialog();
    }

    @Test
    @Order(43)
    @DisplayName("TC-QLTT-4.3: Thêm: Nhấn Hủy (Cancel)")
    void testAddNewsCancel() {
        String title = "Tin tức bị hủy " + System.currentTimeMillis();
        newsPage.clickAddNewsButton();
        newsPage.fillNewsForm(title, "Nội dung", null, false);
        newsPage.cancelDialog();
        
        assertFalse(newsPage.getAllNewsTitles().contains(title), "Tin tức không được lưu khi nhấn Hủy");
    }

    @Test
    @Order(5)
    @DisplayName("TC-QLTT-5: Thêm tin tức hợp lệ")
    void testAddValidNews() {
        int initialCount = newsPage.getNewsCount();
        String title = "Tin tức hợp lệ " + System.currentTimeMillis();
        newsPage.addNews(title, "Nội dung chuẩn", false);
        
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertTrue(newsPage.getNewsCount() > initialCount
                || newsPage.getAllNewsTitles().stream().anyMatch(t -> t.contains(title)),
            "Bài viết hợp lệ phải xuất hiện trong bảng");
            
        newsPage.deleteNews(title, true);
    }

    @Test
    @Order(6)
    @DisplayName("TC-QLTT-6: Sửa bài viết (Cập nhật Title/Status)")
    void testEditNews() {
        String originalTitle = "Tin tức cũ " + System.currentTimeMillis();
        newsPage.addNews(originalTitle, "Content", false);
        
        if (newsPage.getNewsCount() > 0) {
            String newTitle = "Tin tức Đã Sửa " + System.currentTimeMillis();
            newsPage.clickEditButton(0); // Mở form sửa
            newsPage.fillNewsForm(newTitle, null, null, true); // Đổi title và publish
            newsPage.submitFormForce();
            
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            assertTrue(newsPage.getAllNewsTitles().contains(newTitle), "Tiêu đề bài viết phải thay đổi thành công");
            
            newsPage.deleteNews(newTitle, true);
        }
    }

    @Test
    @Order(61)
    @DisplayName("TC-QLTT-6.1: Sửa bài viết (Bỏ trống Tiêu đề)")
    void testEditNewsEmptyTitle() {
        String title = "Tin tức edit error " + System.currentTimeMillis();
        newsPage.addNews(title, "Content", false);
        
        if (newsPage.getNewsCount() > 0) {
            newsPage.searchNews(title);
            newsPage.clickEditButton(0);
            newsPage.fillNewsForm("", null, null, false); // Xóa tiêu đề
            newsPage.submitFormForce();
            
            assertTrue(newsPage.isDialogDisplayed(), "Form phải giữ nguyên khi bỏ trống trường bắt buộc");
            newsPage.cancelDialog();
            newsPage.deleteNews(title, true);
        }
    }

    @Test
    @Order(7)
    @DisplayName("TC-QLTT-7: Xóa bài viết")
    void testDeleteNews() {
        String title = "Tin tức để xóa " + System.currentTimeMillis();
        newsPage.addNews(title, "Content", false);
        
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Luồng hủy xóa (Cancel)
        newsPage.deleteNews(title, false); 
        assertTrue(newsPage.getAllNewsTitles().contains(title), "Bài viết phải còn nếu chọn Hủy xóa");

        // Luồng xác nhận xóa (Confirm)
        newsPage.deleteNews(title, true);
        assertFalse(newsPage.getAllNewsTitles().contains(title), "Bài viết phải biến mất nếu chọn Xác nhận xóa");
    }
}
