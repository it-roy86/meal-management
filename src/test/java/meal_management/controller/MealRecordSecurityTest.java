package meal_management.controller;

import meal_management.config.SecurityConfig;
import meal_management.entity.Company;
import meal_management.entity.CompanyTeam;
import meal_management.entity.MealRecord;
import meal_management.service.MealRecordExcelService;
import meal_management.service.MealRecordService;
import meal_management.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 식사 기록 쓰기 API(등록/수정/삭제)의 역할별 권한 테스트예요.
 * 화면에서 버튼을 숨기는 것과 별개로, 서버(SecurityConfig)가 직접 요청을 막는지 확인해요.
 * - POST(등록): ADMIN, OPERATOR
 * - PUT(수정), DELETE(삭제): ADMIN
 */
@WebMvcTest(MealRecordController.class)
@Import(SecurityConfig.class)
class MealRecordSecurityTest {

    private static final String CREATE_BODY =
            "{\"recordDate\":\"2026-11-16\",\"companyId\":1,\"companyTeamId\":1,\"lunchCount\":1,\"dinnerCount\":0}";
    private static final String UPDATE_BODY = "{\"lunchCount\":1,\"dinnerCount\":0}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealRecordService mealRecordService;

    @MockitoBean
    private MealRecordExcelService mealRecordExcelService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 권한을 통과했을 때 컨트롤러가 정상 응답할 수 있도록 가짜 기록을 돌려줘요.
        Company company = new Company();
        company.setCompanyName("테스트회사");
        CompanyTeam team = new CompanyTeam();
        team.setTeamName("테스트팀");
        MealRecord record = new MealRecord();
        record.setId(1L);
        record.setRecordDate(LocalDate.of(2026, 11, 16));
        record.setCompany(company);
        record.setCompanyTeam(team);
        record.setLunchCount(1);
        record.setDinnerCount(0);
        record.setTotalCount(1);
        record.setTotalAmount(0);

        when(mealRecordService.createMealRecord(any(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(record);
        when(mealRecordService.updateMealRecord(anyLong(), anyInt(), anyInt())).thenReturn(record);
        when(mealRecordService.deleteMealRecord(anyLong())).thenReturn(record);
    }

    // ===== VIEWER: 쓰기 전부 거부 =====

    @Test
    @WithMockUser(roles = "VIEWER")
    void VIEWER는_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/api/meal-records").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void VIEWER는_수정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/meal-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void VIEWER는_삭제할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/meal-records/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ===== OPERATOR: 등록만 가능 =====

    @Test
    @WithMockUser(roles = "OPERATOR")
    void OPERATOR는_등록할_수_있다() throws Exception {
        mockMvc.perform(post("/api/meal-records").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void OPERATOR는_수정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/meal-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void OPERATOR는_삭제할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/meal-records/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ===== ADMIN: 전부 가능 =====

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN은_등록할_수_있다() throws Exception {
        mockMvc.perform(post("/api/meal-records").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN은_수정할_수_있다() throws Exception {
        mockMvc.perform(put("/api/meal-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN은_삭제할_수_있다() throws Exception {
        mockMvc.perform(delete("/api/meal-records/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
