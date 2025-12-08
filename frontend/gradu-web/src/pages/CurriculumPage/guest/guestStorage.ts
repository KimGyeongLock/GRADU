// src/pages/CurriculumPage/guestStorage.ts
import type { CourseDto } from "./curriculumTypes";

const COURSES_KEY = "gradu_guest_courses_v1";
const TOGGLES_KEY = "gradu_guest_toggles_v1";

export function loadGuestCourses(): CourseDto[] {
  try {
    const raw = sessionStorage.getItem(COURSES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed as CourseDto[];
  } catch {
    return [];
  }
}

export function saveGuestCourses(courses: CourseDto[]) {
  try {
    sessionStorage.setItem(COURSES_KEY, JSON.stringify(courses));
  } catch {
    // ignore
  }
}

function nextId(courses: CourseDto[]): number {
  return courses.reduce((m, c) => (c.id > m ? c.id : m), 0) + 1;
}

// CourseDto에서 id만 뺀 형태 입력
export type GuestCourseInput = Omit<CourseDto, "id">;

export function addGuestCourse(input: GuestCourseInput): CourseDto {
  const courses = loadGuestCourses();
  const newCourse: CourseDto = {
    ...input,
    id: nextId(courses),
  };
  const merged = [...courses, newCourse];
  saveGuestCourses(merged);
  return newCourse;
}

export function bulkAddGuestCourses(inputs: GuestCourseInput[]): CourseDto[] {
  const courses = loadGuestCourses();
  let id = nextId(courses);
  const newCourses: CourseDto[] = inputs.map((i) => ({
    ...i,
    id: id++,
  }));
  saveGuestCourses([...courses, ...newCourses]);
  return newCourses;
}

export function updateGuestCourse(
  id: number,
  patch: Partial<GuestCourseInput>
): CourseDto | null {
  const courses = loadGuestCourses();
  const idx = courses.findIndex((c) => c.id === id);
  if (idx === -1) return null;
  const updated: CourseDto = { ...courses[idx], ...patch };
  courses[idx] = updated;
  saveGuestCourses(courses);
  return updated;
}

export function removeGuestCourse(id: number) {
  const list = loadGuestCourses();
  const next = list.filter((c) => c.id !== id);
  saveGuestCourses(next);
}


export function clearGuestCourses() {
  saveGuestCourses([]);
}

export type GuestToggles = {
  gradEnglishPassed: boolean;
  deptExtraPassed: boolean;
};

export function loadGuestToggles(): GuestToggles | null {
  try {
    const raw = sessionStorage.getItem(TOGGLES_KEY);
    if (!raw) return null;
    const obj = JSON.parse(raw);
    return {
      gradEnglishPassed: !!obj.gradEnglishPassed,
      deptExtraPassed: !!obj.deptExtraPassed,
    };
  } catch {
    return null;
  }
}

export function saveGuestToggles(toggles: GuestToggles) {
  try {
    sessionStorage.setItem(TOGGLES_KEY, JSON.stringify(toggles));
  } catch {
    // ignore
  }
}