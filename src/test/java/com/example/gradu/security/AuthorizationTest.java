package com.example.gradu.security;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthorizationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    StudentRepository studentRepository;   // 네 패키지에 맞게 import
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CurriculumRepository curriculumRepository;

    static final String A = "20000001";
    static final String B = "20000002";
    private static final String TEST_PW = "Test1234!";

    @BeforeEach
    void setupStudents() {
        ensureStudentExists(A, "테스트A");
        ensureStudentExists(B, "테스트B");
    }

    private void ensureStudentExists(String studentId, String name) {
        var opt = studentRepository.findById(studentId);
        if (opt.isPresent()) {
            return;
        }

        Student s = Student.builder()
                .studentId(studentId)
                .name(name)
                .password(passwordEncoder.encode(TEST_PW))
                .build();
        studentRepository.save(s);

        Curriculum major = Curriculum.builder()
                .student(s)
                .category(Category.MAJOR)
                .earnedCredits(0)
                .status(Curriculum.Status.FAIL)
                .build();
        curriculumRepository.save(major);

        // 필요하면 BSM, ELECTIVE 등 다른 카테고리도 같이 만들어도 됨
    }

    static Stream<AuthCase> authCases() {
        return Stream.of(
                // 1~3: summary 조회
                new AuthCase("me summary ok",
                        A, A, HttpMethod.GET,
                        "/api/v1/students/%s/summary", 200, false),

                new AuthCase("other summary forbidden",
                        A, B, HttpMethod.GET,
                        "/api/v1/students/%s/summary", 403, false),

                new AuthCase("anonymous summary unauthorized",
                        null, A, HttpMethod.GET,
                        "/api/v1/students/%s/summary", 401, false),

                // 4~6: 전체 과목 조회 /courses/all
                new AuthCase("me all courses ok",
                        A, A, HttpMethod.GET,
                        "/api/v1/students/%s/courses/all", 200, false),

                new AuthCase("other all courses forbidden",
                        A, B, HttpMethod.GET,
                        "/api/v1/students/%s/courses/all", 403, false),

                new AuthCase("anonymous all courses unauthorized",
                        null, A, HttpMethod.GET,
                        "/api/v1/students/%s/courses/all", 401, false),

                // 7~9: 카테고리별 과목 조회 /courses/categories/{category}
                // category는 예시로 MAJOR 고정
                new AuthCase("me category courses ok",
                        A, A, HttpMethod.GET,
                        "/api/v1/students/%s/courses/categories/MAJOR", 200, false),

                new AuthCase("other category courses forbidden",
                        A, B, HttpMethod.GET,
                        "/api/v1/students/%s/courses/categories/MAJOR", 403, false),

                new AuthCase("anonymous category courses unauthorized",
                        null, A, HttpMethod.GET,
                        "/api/v1/students/%s/courses/categories/MAJOR", 401, false),

                // 10~12: 커리큘럼 조회 /curriculum
                new AuthCase("me curriculum ok",
                        A, A, HttpMethod.GET,
                        "/api/v1/students/%s/curriculum", 200, false),

                new AuthCase("other curriculum forbidden",
                        A, B, HttpMethod.GET,
                        "/api/v1/students/%s/curriculum", 403, false),

                new AuthCase("anonymous curriculum unauthorized",
                        null, A, HttpMethod.GET,
                        "/api/v1/students/%s/curriculum", 401, false),

                // 13~15: 과목 추가 /courses (POST, body 필요)
                new AuthCase("me add course ok",
                        A, A, HttpMethod.POST,
                        "/api/v1/students/%s/courses", 200, true),

                new AuthCase("other add course forbidden",
                        A, B, HttpMethod.POST,
                        "/api/v1/students/%s/courses", 403, true),

                new AuthCase("anonymous add course unauthorized",
                        null, A, HttpMethod.POST,
                        "/api/v1/students/%s/courses", 401, true),

                // 16~18: 과목 일괄 추가 /courses/bulk (POST, body 필요)
                new AuthCase("me bulk courses ok",
                        A, A, HttpMethod.POST,
                        "/api/v1/students/%s/courses/bulk", 200, true),

                new AuthCase("other bulk courses forbidden",
                        A, B, HttpMethod.POST,
                        "/api/v1/students/%s/courses/bulk", 403, true),

                new AuthCase("anonymous bulk courses unauthorized",
                        null, A, HttpMethod.POST,
                        "/api/v1/students/%s/courses/bulk", 401, true),

                // 19~21: 과목 삭제 /courses/{courseId} (courseId=1 예시)
                new AuthCase("me delete course ok",
                        A, A, HttpMethod.DELETE,
                        "/api/v1/students/%s/courses/1", 200, false),

                new AuthCase("other delete course forbidden",
                        A, B, HttpMethod.DELETE,
                        "/api/v1/students/%s/courses/1", 403, false),

                new AuthCase("anonymous delete course unauthorized",
                        null, A, HttpMethod.DELETE,
                        "/api/v1/students/%s/courses/1", 401, false),

                // 22~24: 과목 수정 /courses/{courseId} (PATCH, body 필요)
                new AuthCase("me update course ok",
                        A, A, HttpMethod.PATCH,
                        "/api/v1/students/%s/courses/1", 200, true),

                new AuthCase("other update course forbidden",
                        A, B, HttpMethod.PATCH,
                        "/api/v1/students/%s/courses/1", 403, true),

                new AuthCase("anonymous update course unauthorized",
                        null, A, HttpMethod.PATCH,
                        "/api/v1/students/%s/courses/1", 401, true),

                // 25~27: summary 토글 수정 /summary/toggles (PATCH, body 필요)
                new AuthCase("me summary toggles ok",
                        A, A, HttpMethod.PATCH,
                        "/api/v1/students/%s/summary/toggles", 200, true),

                new AuthCase("other summary toggles forbidden",
                        A, B, HttpMethod.PATCH,
                        "/api/v1/students/%s/summary/toggles", 403, true),

                new AuthCase("anonymous summary toggles unauthorized",
                        null, A, HttpMethod.PATCH,
                        "/api/v1/students/%s/summary/toggles", 401, true),

                // 28~30: summary rebuild /summary/rebuild (POST, body 없음)
                new AuthCase("me summary rebuild ok",
                        A, A, HttpMethod.POST,
                        "/api/v1/students/%s/summary/rebuild", 200, false),

                new AuthCase("other summary rebuild forbidden",
                        A, B, HttpMethod.POST,
                        "/api/v1/students/%s/summary/rebuild", 403, false),

                new AuthCase("anonymous summary rebuild unauthorized",
                        null, A, HttpMethod.POST,
                        "/api/v1/students/%s/summary/rebuild", 401, false)
        );
    }

    private String buildBodyJson(String url, HttpMethod method) {

        // 1) 과목 단건 추가 /courses (CourseRequestDto)
        if (url.contains("/courses")
                && !url.contains("/bulk")
                && method == HttpMethod.POST) {

            return """
        {
          "name": "테스트 과목",
          "credit": 3,
          "category": "MAJOR",
          "designedCredit": 0,
          "isEnglish": false,
          "grade": "A+",
          "academicYear": 2025,
          "term": "1"
        }
        """;
        }

        // 2) 과목 일괄 추가 /courses/bulk (List<CourseBulkRequest>)
        if (url.contains("/courses/bulk") && method == HttpMethod.POST) {

            return """
        [
          {
            "name": "일괄 과목1",
            "credit": 3,
            "category": "MAJOR",
            "designedCredit": 0,
            "isEnglish": false,
            "grade": "A0",
            "academicYear": 2025,
            "term": "1"
          }
        ]
        """;
        }

        // 3) 과목 수정 /courses/{courseId} (CourseUpdateRequestDto)
        if (url.matches(".*/courses/\\d+") && method == HttpMethod.PATCH) {

            return """
        {
          "name": "수정된 과목명",
          "credit": 3,
          "category": "MAJOR",
          "designedCredit": 1,
          "isEnglish": true,
          "grade": "B+",
          "academicYear": 2025,
          "term": "1"
        }
        """;
        }

        // 4) summary 토글 수정 /summary/toggles (TogglesDto)
        if (url.contains("/summary/toggles") && method == HttpMethod.PATCH) {

            return """
        {
          "gradEnglishPassed": true,
          "deptExtraPassed": false
        }
        """;
        }

        // 기본 fallback
        return "{}";
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("authCases")
    void authorizationTests(AuthCase c) throws Exception {

        String url = c.urlTemplate().formatted(c.targetStudentId());

        MockHttpServletRequestBuilder req;

        HttpMethod m = c.method();   // ← 편의용 변수

        if (m == HttpMethod.GET) {
            req = get(url);
        } else if (m == HttpMethod.POST) {
            req = post(url);
        } else if (m == HttpMethod.PATCH) {
            req = patch(url);
        } else if (m == HttpMethod.DELETE) {
            req = delete(url);
        } else {
            throw new IllegalStateException("Unexpected value: " + m);
        }

        if (c.actorStudentId() != null) {
            String token = loginAndGetToken(c.actorStudentId());
            req.header("Authorization", "Bearer " + token);
        }

        if (c.withBody()) {
            req.contentType(MediaType.APPLICATION_JSON);
            req.content(buildBodyJson(url, m));
        }

        mockMvc.perform(req)
                .andExpect(result -> {
                    int actual = result.getResponse().getStatus();
                    int expected = c.expectedStatus();

                    if (expected == 200) {
                        assertNotEquals(401, actual, "should not be 401 Unauthorized");
                        assertNotEquals(403, actual, "should not be 403 Forbidden");
                    } else {
                        assertEquals(expected, actual);
                    }
                });
    }


    private String loginAndGetToken(String studentId) throws Exception {
        var loginReq = new StudentAuthRequestDto(studentId, TEST_PW, null, null, null);

        String json = mapper.writeValueAsString(loginReq);

        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        )
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("accessToken").asText();
    }
}
