package meal_management.service;

import lombok.extern.slf4j.Slf4j;
import meal_management.entity.MealRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * 식사 기록을 엑셀(.xlsx) 파일로 만들어주는 서비스예요.
 *
 * 식사 현황 조회 화면의 "엑셀 다운로드"에서 쓰고, 추후 메일 발송 기능(기획설계서 기능 05)에서
 * 청구서 첨부 파일로도 재사용할 수 있게 "식사 기록 목록 → 엑셀 바이트" 변환만 담당해요.
 * (조회 조건/권한 처리는 호출하는 쪽 책임 — 컨트롤러가 VIEWER 필터까지 적용한 목록을 넘겨줘요.)
 *
 * 시트 구성: 날짜 | 회사명 | 팀명 | 중식 인원 | 석식 인원 | 총 인원 | 합계 금액
 * - 날짜 오름차순 (같은 날짜면 회사명 → 팀명 오름차순)
 * - 맨 아래 합계 행 (SUM 수식 — 받는 사람이 엑셀에서 검산할 수 있게)
 */
@Slf4j
@Service
public class MealRecordExcelService {

    private static final String SHEET_NAME = "식대내역";

    private static final String[] HEADERS = {
            "날짜", "회사명", "팀명", "중식 인원", "석식 인원", "총 인원", "합계 금액"
    };

    // 열 너비 (1글자 = 256 단위)
    // autoSizeColumn()은 서버에 한글 폰트가 없으면(Docker 이미지 등) 너비를 잘못 계산하거나
    // 느려질 수 있어서 고정값으로 지정해요.
    private static final int[] COLUMN_WIDTHS = {
            14 * 256, 18 * 256, 18 * 256, 11 * 256, 11 * 256, 11 * 256, 16 * 256
    };

    // 합계 수식을 넣을 열 (중식 인원 ~ 합계 금액)
    private static final int FIRST_SUM_COLUMN = 3;

    /**
     * 식사 기록 목록을 엑셀 파일(바이트 배열)로 변환
     * 넘겨받은 목록의 정렬 순서와 상관없이 날짜 오름차순으로 다시 정렬해서 써요.
     * (조회 API도 날짜 오름차순이지만, 같은 날짜 안의 순서까지 고정하려고 여기서 한 번 더 정렬해요.)
     */
    public byte[] createMealRecordExcel(List<MealRecord> records) {
        log.debug("식사 기록 엑셀 생성 시작: {}건", records.size());

        // 날짜 오름차순 → 같은 날짜면 회사명, 팀명 오름차순
        List<MealRecord> sorted = records.stream()
                .sorted(Comparator.comparing(MealRecord::getRecordDate)
                        .thenComparing(r -> r.getCompany().getCompanyName())
                        .thenComparing(r -> r.getCompanyTeam().getTeamName()))
                .toList();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Styles styles = new Styles(workbook);

            // 1행: 헤더
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(styles.header);
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
            }

            // 2행부터: 데이터
            int rowIndex = 1;
            for (MealRecord record : sorted) {
                Row row = sheet.createRow(rowIndex++);

                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(record.getRecordDate());
                dateCell.setCellStyle(styles.date);

                setTextCell(row, 1, record.getCompany().getCompanyName(), styles.text);
                setTextCell(row, 2, record.getCompanyTeam().getTeamName(), styles.text);
                setNumberCell(row, 3, record.getLunchCount(), styles.count);
                setNumberCell(row, 4, record.getDinnerCount(), styles.count);
                setNumberCell(row, 5, record.getTotalCount(), styles.count);
                setNumberCell(row, 6, record.getTotalAmount(), styles.amount);
            }

            // 마지막 행: 합계 (A~C 병합)
            Row totalRow = sheet.createRow(rowIndex);
            for (int i = 0; i < FIRST_SUM_COLUMN; i++) {
                totalRow.createCell(i).setCellStyle(styles.totalLabel);
            }
            totalRow.getCell(0).setCellValue("합계");
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, FIRST_SUM_COLUMN - 1));

            for (int col = FIRST_SUM_COLUMN; col < HEADERS.length; col++) {
                Cell cell = totalRow.createCell(col);
                cell.setCellStyle(col == HEADERS.length - 1 ? styles.totalAmount : styles.totalCount);
                if (sorted.isEmpty()) {
                    cell.setCellValue(0);
                } else {
                    // 엑셀 수식은 1부터 세는 행 번호 → 데이터는 2행 ~ (rowIndex)행
                    String column = columnLetter(col);
                    cell.setCellFormula("SUM(" + column + "2:" + column + rowIndex + ")");
                }
            }

            // 헤더 고정 (스크롤해도 1행이 보이게)
            sheet.createFreezePane(0, 1);

            // 수식 결과를 미리 계산해서 파일에 저장해요.
            // 안 하면 메일 미리보기/모바일 뷰어처럼 수식을 계산하지 않는 프로그램에서 합계가 0이나 빈칸으로 보여요.
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            workbook.write(out);
            log.debug("식사 기록 엑셀 생성 완료: {}건, {}bytes", sorted.size(), out.size());
            return out.toByteArray();

        } catch (IOException e) {
            // 여기서는 로그를 남기지 않고 원인(e)을 담아 던지기만 해요.
            // 호출한 쪽(컨트롤러, 추후 메일 발송)이 요청 정보와 함께 log.error로 한 번만 남겨요
            // — 여기서도 찍으면 같은 스택트레이스가 로그에 두 번 나와서 읽기 어려워져요.
            throw new RuntimeException("엑셀 파일 생성에 실패했습니다: " + records.size() + "건", e);
        }
    }

    private void setTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumberCell(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        // 숫자 셀로 넣어야 받는 쪽에서 엑셀로 합계/계산을 할 수 있어요 ("12,000원" 같은 문자열 X)
        cell.setCellValue(value == null ? 0 : value);
        cell.setCellStyle(style);
    }

    /**
     * 0부터 시작하는 열 번호 → 엑셀 열 문자 (0 → A, 6 → G)
     * 이 시트는 열이 7개뿐이라 한 글자만 처리해요.
     */
    private String columnLetter(int col) {
        return String.valueOf((char) ('A' + col));
    }

    /**
     * 셀 스타일 모음
     * 스타일은 워크북마다 만들어야 하고, 셀마다 새로 만들면 엑셀의 스타일 개수 제한(약 64,000개)에
     * 걸릴 수 있어서 한 번만 만들어 재사용해요.
     */
    private static class Styles {
        final CellStyle header;
        final CellStyle text;
        final CellStyle date;
        final CellStyle count;
        final CellStyle amount;
        final CellStyle totalLabel;
        final CellStyle totalCount;
        final CellStyle totalAmount;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            header = bordered(workbook);
            header.setFont(boldFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            text = bordered(workbook);

            date = bordered(workbook);
            date.setAlignment(HorizontalAlignment.CENTER);
            date.setDataFormat(format.getFormat("yyyy-mm-dd"));

            count = bordered(workbook);
            count.setDataFormat(format.getFormat("#,##0"));

            amount = bordered(workbook);
            amount.setDataFormat(format.getFormat("#,##0\"원\""));

            totalLabel = bordered(workbook);
            totalLabel.setFont(boldFont);
            totalLabel.setAlignment(HorizontalAlignment.CENTER);
            totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalCount = bordered(workbook);
            totalCount.cloneStyleFrom(count);
            totalCount.setFont(boldFont);
            totalCount.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalCount.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalAmount = bordered(workbook);
            totalAmount.cloneStyleFrom(amount);
            totalAmount.setFont(boldFont);
            totalAmount.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalAmount.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        private static CellStyle bordered(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }
    }
}
