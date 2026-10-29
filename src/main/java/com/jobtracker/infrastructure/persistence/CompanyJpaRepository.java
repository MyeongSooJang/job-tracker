package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<Company, Long>, CompanyRepository {
}
