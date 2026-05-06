package com.lockerkorea.ui.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel Report Generator for UI Test Cases
 * Generates formatted Excel report with colors matching the requested template
 */
public class ExcelReportGenerator {

    // Styles
    private CellStyle headerStyle;
    private CellStyle sectionHeaderStyle;
    private CellStyle passedStyle;
    private CellStyle failedStyle;
    private CellStyle skippedStyle;
    private CellStyle defaultStyle;

    public void generateReport(String filePath, List<TestCaseRow> testCases, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createStyles(workbook);

            Sheet sheet = workbook.createSheet(sheetName);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã TC", "Dữ liệu mẫu", "Các bước thực hiện chi tiết", "Kết Quả Mong Muốn", "Kết Quả Thực Tế", "Trạng Thái"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Add data rows
            int rowNum = 1;
            for (TestCaseRow testCase : testCases) {
                Row row = sheet.createRow(rowNum++);

                // Mã TC
                createCell(row, 0, testCase.getMaTC(), defaultStyle);

                // Dữ liệu mẫu
                createCell(row, 1, testCase.getDuLieuMau(), defaultStyle);

                // Các bước thực hiện chi tiết
                createCell(row, 2, testCase.getCacBuoc(), defaultStyle);

                // Kết Quả Mong Muốn
                createCell(row, 3, testCase.getKetQuaMongMuon(), defaultStyle);

                // Kết Quả Thực Tế
                createCell(row, 4, testCase.getKetQuaThucTe(), defaultStyle);

                // Trạng Thái (with color)
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(testCase.getTrangThai());

                switch (testCase.getTrangThai().toUpperCase()) {
                    case "PASSED":
                    case "PASS":
                        statusCell.setCellStyle(passedStyle);
                        break;
                    case "FAILED":
                    case "FAIL":
                        statusCell.setCellStyle(failedStyle);
                        break;
                    case "SKIPPED":
                    case "SKIP":
                        statusCell.setCellStyle(skippedStyle);
                        break;
                    default:
                        statusCell.setCellStyle(defaultStyle);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            System.out.println("Excel report generated: " + filePath);

        } catch (Exception e) {
            System.err.println("Failed to generate Excel report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createStyles(Workbook workbook) {
        // Header style (dark blue background, white text, bold)
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Section header style (light gray background)
        sectionHeaderStyle = workbook.createCellStyle();
        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setFontHeightInPoints((short) 11);
        sectionHeaderStyle.setFont(sectionFont);
        sectionHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Passed style (green background)
        passedStyle = workbook.createCellStyle();
        Font passedFont = workbook.createFont();
        passedFont.setColor(IndexedColors.DARK_GREEN.getIndex());
        passedFont.setBold(true);
        passedStyle.setFont(passedFont);
        passedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        passedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        passedStyle.setAlignment(HorizontalAlignment.CENTER);
        passedStyle.setBorderBottom(BorderStyle.THIN);
        passedStyle.setBorderTop(BorderStyle.THIN);
        passedStyle.setBorderLeft(BorderStyle.THIN);
        passedStyle.setBorderRight(BorderStyle.THIN);

        // Failed style (red background)
        failedStyle = workbook.createCellStyle();
        Font failedFont = workbook.createFont();
        failedFont.setColor(IndexedColors.DARK_RED.getIndex());
        failedFont.setBold(true);
        failedStyle.setFont(failedFont);
        failedStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        failedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        failedStyle.setAlignment(HorizontalAlignment.CENTER);
        failedStyle.setBorderBottom(BorderStyle.THIN);
        failedStyle.setBorderTop(BorderStyle.THIN);
        failedStyle.setBorderLeft(BorderStyle.THIN);
        failedStyle.setBorderRight(BorderStyle.THIN);

        // Skipped style (yellow background)
        skippedStyle = workbook.createCellStyle();
        Font skippedFont = workbook.createFont();
        skippedFont.setColor(IndexedColors.DARK_YELLOW.getIndex());
        skippedFont.setBold(true);
        skippedStyle.setFont(skippedFont);
        skippedStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        skippedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        skippedStyle.setAlignment(HorizontalAlignment.CENTER);
        skippedStyle.setBorderBottom(BorderStyle.THIN);
        skippedStyle.setBorderTop(BorderStyle.THIN);
        skippedStyle.setBorderLeft(BorderStyle.THIN);
        skippedStyle.setBorderRight(BorderStyle.THIN);

        // Default style
        defaultStyle = workbook.createCellStyle();
        defaultStyle.setAlignment(HorizontalAlignment.LEFT);
        defaultStyle.setVerticalAlignment(VerticalAlignment.TOP);
        defaultStyle.setWrapText(true);
        defaultStyle.setBorderBottom(BorderStyle.THIN);
        defaultStyle.setBorderTop(BorderStyle.THIN);
        defaultStyle.setBorderLeft(BorderStyle.THIN);
        defaultStyle.setBorderRight(BorderStyle.THIN);
    }

    /**
     * Test case row data holder
     */
    public static class TestCaseRow {
        private String maTC;
        private String duLieuMau;
        private String cacBuoc;
        private String ketQuaMongMuon;
        private String ketQuaThucTe;
        private String trangThai;

        public TestCaseRow(String maTC, String duLieuMau, String cacBuoc, String ketQuaMongMuon, String ketQuaThucTe, String trangThai) {
            this.maTC = maTC;
            this.duLieuMau = duLieuMau;
            this.cacBuoc = cacBuoc;
            this.ketQuaMongMuon = ketQuaMongMuon;
            this.ketQuaThucTe = ketQuaThucTe;
            this.trangThai = trangThai;
        }

        public String getMaTC() { return maTC; }
        public String getDuLieuMau() { return duLieuMau; }
        public String getCacBuoc() { return cacBuoc; }
        public String getKetQuaMongMuon() { return ketQuaMongMuon; }
        public String getKetQuaThucTe() { return ketQuaThucTe; }
        public String getTrangThai() { return trangThai; }
    }
}
