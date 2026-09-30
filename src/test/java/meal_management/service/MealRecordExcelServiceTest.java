package meal_management.service;

import meal_management.entity.Company;
import meal_management.entity.CompanyTeam;
import meal_management.entity.MealRecord;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MealRecordExcelService 단위 테스트
 * DB/스프링 컨텍스트 없이 엔티티를 직접 만들어서, 생성된 엑셀을 다시 읽어 검증해요.
 */
class MealRecordExcelServiceTest {

    private final MealRecordExcelService service = new MealRecordExcelService();

    @Test
    void 헤더_열_구성이_요구사항과_같아요() throws IOException {
        Sheet sheet = readSheet(service.createMealRecordExcel(List.of()));

        Row header = sheet.getRow(0);
        assertThat(header.getCell(0).getStringCellValue()).isEqualTo("날짜");
        assertThat(header.getCell(1).getStringCellValue()).isEqualTo("회사명");
        assertThat(header.getCell(2).getStringCellValue()).isEqualTo("팀명");
        assertThat(header.getCell(3).getStringCellValue()).isEqualTo("중식 인원");
        assertThat(header.getCell(4).getStringCellValue()).isEqualTo("석식 인원");
        assertThat(header.getCell(5).getStringCellValue()).isEqualTo("총 인원");
        assertThat(header.getCell(6).getStringCellValue()).isEqualTo("합계 금액");
    }

    @Test
    void 날짜_오름차순으로_정렬되고_같은_날짜는_회사명_팀명_순이에요() throws IOException {
        // 일부러 뒤섞인 순서로 넘겨요
        List<MealRecord> records = List.of(
                record("2026-09-30", "대박유통", "영업팀", 3, 0, 21_000),
                record("2026-09-15", "(주)복수", "관리팀", 4, 2, 42_000),
                record("2026-09-01", "대박유통", "생산팀", 10, 5, 110_000),
                record("2026-09-30", "(주)복수", "관리팀", 5, 1, 42_000)
        );

        Sheet sheet = readSheet(service.createMealRecordExcel(records));

        assertThat(dateAt(sheet, 1)).isEqualTo("2026-09-01");
        assertThat(dateAt(sheet, 2)).isEqualTo("2026-09-15");
        assertThat(dateAt(sheet, 3)).isEqualTo("2026-09-30");
        assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("(주)복수");
        assertThat(dateAt(sheet, 4)).isEqualTo("2026-09-30");
        assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("대박유통");
    }

    @Test
    void 맨_아래_합계_행에_인원과_금액_합계가_들어가요() throws IOException {
        List<MealRecord> records = List.of(
                record("2026-09-01", "대박유통", "생산팀", 10, 5, 110_000),
                record("2026-09-02", "대박유통", "생산팀", 8, 2, 76_000)
        );

        Sheet sheet = readSheet(service.createMealRecordExcel(records));

        // 헤더 1행 + 데이터 2행 → 합계는 4번째 행(index 3)
        Row total = sheet.getRow(3);
        assertThat(total.getCell(0).getStringCellValue()).isEqualTo("합계");

        // 받는 사람이 검산할 수 있게 SUM 수식으로 들어가 있어요
        assertThat(total.getCell(3).getCellType()).isEqualTo(CellType.FORMULA);
        assertThat(total.getCell(3).getCellFormula()).isEqualTo("SUM(D2:D3)");

        // 수식 결과가 미리 계산되어 저장돼 있어요 (메일 미리보기 등에서 0으로 안 보이게)
        assertThat(total.getCell(3).getNumericCellValue()).isEqualTo(18);       // 중식
        assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(7);        // 석식
        assertThat(total.getCell(5).getNumericCellValue()).isEqualTo(25);       // 총 인원
        assertThat(total.getCell(6).getNumericCellValue()).isEqualTo(186_000);  // 합계 금액
    }

    @Test
    void 금액과_인원은_문자열이_아니라_숫자_셀이에요() throws IOException {
        Sheet sheet = readSheet(service.createMealRecordExcel(List.of(
                record("2026-09-01", "대박유통", "생산팀", 10, 5, 110_000)
        )));

        Row row = sheet.getRow(1);
        assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(row.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(row.getCell(6).getNumericCellValue()).isEqualTo(110_000);
    }

    @Test
    void 기록이_없어도_헤더와_0원_합계_행이_있는_파일을_만들어요() throws IOException {
        Sheet sheet = readSheet(service.createMealRecordExcel(List.of()));

        Row total = sheet.getRow(1);
        assertThat(total.getCell(0).getStringCellValue()).isEqualTo("합계");
        assertThat(total.getCell(6).getNumericCellValue()).isEqualTo(0);
    }

    // ========================
    // 테스트 헬퍼
    // ========================

    private MealRecord record(String date, String companyName, String teamName,
                              int lunch, int dinner, int amount) {
        Company company = new Company();
        company.setCompanyName(companyName);

        CompanyTeam team = new CompanyTeam();
        team.setTeamName(teamName);

        MealRecord record = new MealRecord();
        record.setRecordDate(LocalDate.parse(date));
        record.setCompany(company);
        record.setCompanyTeam(team);
        record.setLunchCount(lunch);
        record.setDinnerCount(dinner);
        record.setTotalCount(lunch + dinner);
        record.setTotalAmount(amount);
        return record;
    }

    private Sheet readSheet(byte[] excel) throws IOException {
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel));
        return workbook.getSheetAt(0);
    }

    private String dateAt(Sheet sheet, int rowIndex) {
        return sheet.getRow(rowIndex).getCell(0).getLocalDateTimeCellValue().toLocalDate().toString();
    }
}
