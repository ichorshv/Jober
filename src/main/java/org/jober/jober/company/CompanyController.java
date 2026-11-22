
package org.jober.jober.company;

import jakarta.validation.Valid;

import org.jober.jober.company.dto.CompanyCreateRequest;
import org.jober.jober.company.dto.CompanyResponse;
import org.jober.jober.company.dto.CompanyUpdateRequest;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.common.web.PageResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public PageResponse<CompanyResponse> getAll(
            Pageable pageable) {

        return PageResponse.from(
                companyService.getAll(pageable));
    }

    @GetMapping("/me")
    public CompanyResponse getOwn(CurrentUser user) {
        user.requireRole(
                org.jober.jober.common.enums.UserRole.COMPANY);

        return companyService.getById(user.id());
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(
            @PathVariable Long id) {

        return companyService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @Valid @RequestBody CompanyCreateRequest request,
            CurrentUser user) {

        return companyService.create(request, user);
    }

    @PutMapping("/me")
    public CompanyResponse updateOwn(
            @Valid @RequestBody CompanyUpdateRequest request,
            CurrentUser user) {

        return companyService.updateOwn(request, user);
    }

    @PutMapping("/{id}")
    public CompanyResponse updateById(
            @PathVariable Long id,
            @Valid @RequestBody CompanyUpdateRequest request,
            CurrentUser user) {

        return companyService.updateById(id, request, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            CurrentUser user) {

        companyService.delete(id, user);
    }
}
