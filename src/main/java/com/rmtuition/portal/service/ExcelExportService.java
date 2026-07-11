package com.rmtuition.portal.service;

import com.rmtuition.portal.model.QuizResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class ExcelExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);

    public byte[] exportQuizResultsToExcel(List<QuizResult> results, String quizTitle) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Results");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Leaderboard / Attempt Results: " + quizTitle);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Columns Header Row
            String[] columns = {"Student Name", "Student Email", "Score", "Total Questions", "Percentage (%)", "Submitted At"};
            Row headerRow = sheet.createRow(2);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Cell Styles for Data
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

            // Populate Data
            int rowIdx = 3;
            for (QuizResult result : results) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(result.getStudentName());
                row.createCell(1).setCellValue(result.getStudentEmail());
                
                Cell scoreCell = row.createCell(2);
                scoreCell.setCellValue(result.getScore());
                scoreCell.setCellStyle(centerStyle);

                Cell totalCell = row.createCell(3);
                totalCell.setCellValue(result.getTotalQuestions());
                totalCell.setCellStyle(centerStyle);

                double percent = (double) result.getScore() / result.getTotalQuestions() * 100;
                Cell percentCell = row.createCell(4);
                percentCell.setCellValue(Math.round(percent * 100.0) / 100.0);
                percentCell.setCellStyle(centerStyle);

                String formattedDate = result.getSubmittedAt() != null ? dateFormat.format(result.getSubmittedAt()) : "-";
                Cell dateCell = row.createCell(5);
                dateCell.setCellValue(formattedDate);
                dateCell.setCellStyle(centerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to generate Excel file: {}", e.getMessage());
            return new byte[0];
        }
    }
}
