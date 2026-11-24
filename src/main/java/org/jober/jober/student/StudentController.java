package org.jober.jober.student;

import jakarta.validation.Valid;

import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.common.web.PageResponse;
import org.jober.jober.student.dto.StudentResponse;
import org.jober.jober.student.dto.StudentUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public StudentResponse getMe(CurrentUser user) {
        return studentService.getMe(user);
    }

    @PutMapping("/me")
    public StudentResponse updateMe(@Valid @RequestBody StudentUpdateRequest request, CurrentUser user) {
        return studentService.updateMe(request, user);
    }

    @GetMapping
    public PageResponse<StudentResponse> search(@RequestParam(required = false) String q, Pageable pageable,
                                                CurrentUser user) {
        return PageResponse.from(studentService.search(q, pageable, user));
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id, CurrentUser user) {
        return studentService.getById(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, CurrentUser user) {
        studentService.delete(id, user);
    }
}
