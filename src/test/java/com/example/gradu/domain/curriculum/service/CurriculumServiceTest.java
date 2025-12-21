package com.example.gradu.domain.curriculum.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.student.StudentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock CurriculumRepository curriculumRepository;
    @Mock StudentRepository studentRepository;

    @InjectMocks CurriculumService service;

    @Captor ArgumentCaptor<List<Curriculum>> curriculumListCaptor;

    @Test
    void initializeForStudent_whenStudentNotFound_throws() {
        // given
        long studentId = 1L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.initializeForStudent(studentId))
                .isInstanceOf(StudentException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verify(curriculumRepository, never()).saveAll(anyList());
    }

    @Test
    void initializeForStudent_savesCurriculumForAllCategories() {
        // given
        long studentId = 1L;

        // Student는 엔티티 빌더/생성자 형태를 몰라도 되게 mock으로 처리
        Student student = mock(Student.class);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // when
        service.initializeForStudent(studentId);

        // then
        verify(curriculumRepository).saveAll(curriculumListCaptor.capture());
        List<Curriculum> saved = curriculumListCaptor.getValue();

        // Category 전체 개수만큼 생성됐는지
        assertThat(saved).hasSize(Category.values().length);

        // 전부 같은 학생을 참조하는지 + earnedCredits=0 인지
        assertThat(saved).allSatisfy(cur -> {
            assertThat(cur.getStudent()).isSameAs(student);
            assertThat(cur.getEarnedCredits()).isEqualTo(0);
            assertThat(cur.getCategory()).isNotNull();
            // recalcStatus() 내부 로직은 여기서 단정하기 애매하니
            // 최소한 호출로 인해 예외 없이 생성됐다는 것만 확인하는 정도로 충분
        });
    }

    @Test
    void getCurriculumsByStudentId_returnsRepositoryResult() {
        // given
        long studentId = 1L;
        Curriculum c1 = mock(Curriculum.class);
        Curriculum c2 = mock(Curriculum.class);

        when(curriculumRepository.findByStudentId(studentId)).thenReturn(List.of(c1, c2));

        // when
        List<Curriculum> result = service.getCurriculumsByStudentId(studentId);

        // then
        assertThat(result).containsExactly(c1, c2);
        verify(curriculumRepository).findByStudentId(studentId);
    }

    @Test
    void removeForStudent_deletesByStudentId() {
        // given
        long studentId = 1L;

        // when
        service.removeForStudent(studentId);

        // then
        verify(curriculumRepository).deleteByStudentId(studentId);
    }
}
