package com.jobtracker.domain.company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(Long id);
    List<Company> findAll();
}
