package com.example.gradu;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("CI에서 전체 컨텍스트 로딩은 환경 의존성이 커서 제외")
@SpringBootTest
@ActiveProfiles("test")
class GraduApplicationTests {

    @Test
    void contextLoads() {
    }

}
