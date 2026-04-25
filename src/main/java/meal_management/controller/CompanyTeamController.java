package meal_management.controller;

import lombok.RequiredArgsConstructor;
import meal_management.entity.CompanyTeam;
import meal_management.service.CompanyTeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 팀 관련 API를 처리하는 컨트롤러예요.
 * Vue.js 설정 화면에서 팀 목록 조회, 등록, 수정 요청이 오면 여기서 받아요.
 * URL 구조: /api/companies/{companyId}/teams
 * 팀은 반드시 특정 회사에 속하기 때문에 회사 ID가 URL에 포함돼요.
 */
@RestController
@RequestMapping("/api/companies/{companyId}/teams")
@RequiredArgsConstructor
public class CompanyTeamController {

    private final CompanyTeamService companyTeamService;

    /**
     * 팀 목록 조회 API
     * GET /api/companies/{companyId}/teams
     *
     * 응답 예시:
     * [
     *   {
     *     "id": 1,
     *     "teamName": "개발팀",
     *     "lunchPrice": 6500,
     *     "dinnerPrice": 7000,
     *     "isActive": true
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<CompanyTeam>> getTeams(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyTeamService.getTeamsByCompany(companyId));
    }

    /**
     * 팀 등록 API
     * POST /api/companies/{companyId}/teams
     *
     * 요청 예시:
     * {
     *   "teamName": "개발팀",
     *   "lunchPrice": 6500,
     *   "dinnerPrice": 7000
     * }
     */
    @PostMapping
    public ResponseEntity<CompanyTeam> createTeam(
            @PathVariable Long companyId,
            @RequestBody CompanyTeam team) {
        return ResponseEntity.ok(companyTeamService.createTeam(companyId, team));
    }

    /**
     * 팀 수정 API
     * PUT /api/companies/{companyId}/teams/{id}
     *
     * 요청 예시:
     * {
     *   "teamName": "개발팀(수정)",
     *   "lunchPrice": 7000,
     *   "dinnerPrice": 8000
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompanyTeam> updateTeam(
            @PathVariable Long companyId,
            @PathVariable Long id,
            @RequestBody CompanyTeam team) {
        return ResponseEntity.ok(companyTeamService.updateTeam(id, team));
    }

    /**
     * 팀 삭제 API (소프트 딜리트)
     * DELETE /api/companies/{companyId}/teams/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long companyId,
            @PathVariable Long id) {
        companyTeamService.deleteTeam(id);
        return ResponseEntity.ok().build();
    }
}