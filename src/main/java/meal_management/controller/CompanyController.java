package meal_management.controller;

import lombok.RequiredArgsConstructor;
import meal_management.entity.Company;
import meal_management.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 회사 관련 API를 처리하는 컨트롤러예요.
 * Vue.js 설정 화면에서 회사 목록 조회, 등록, 수정 요청이 오면 여기서 받아요.
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 회사 목록 조회 API
     * GET /api/companies
     *
     * 응답 예시:
     * [
     *   {
     *     "id": 1,
     *     "companyName": "대박유통",
     *     "businessNumber": "1234567890",
     *     "contactEmail": "test@test.com",
     *     "isActive": true
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<Company>> getCompanies() {
        return ResponseEntity.ok(companyService.getActiveCompanies());
    }

    /**
     * 회사 단건 조회 API
     * GET /api/companies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    /**
     * 회사 등록 API
     * POST /api/companies
     *
     * 요청 예시:
     * {
     *   "companyName": "대박유통",
     *   "businessNumber": "1234567890",
     *   "contactEmail": "test@test.com"
     * }
     */
    @PostMapping
    public ResponseEntity<Company> createCompany(@RequestBody Company company) {
        return ResponseEntity.ok(companyService.createCompany(company));
    }

    /**
     * 회사 수정 API
     * PUT /api/companies/{id}
     *
     * 요청 예시:
     * {
     *   "companyName": "대박유통(수정)",
     *   "businessNumber": "1234567890",
     *   "contactEmail": "new@test.com"
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(
            @PathVariable Long id,
            @RequestBody Company company) {
        return ResponseEntity.ok(companyService.updateCompany(id, company));
    }

    /**
     * 회사 삭제 API (소프트 딜리트)
     * DELETE /api/companies/{id}
     * 실제로 삭제하지 않고 is_active를 false로 변경해요.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok().build();
    }
}