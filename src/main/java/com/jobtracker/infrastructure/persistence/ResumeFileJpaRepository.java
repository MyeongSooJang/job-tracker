package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResumeFileJpaRepository extends JpaRepository<ResumeFile, Long>, ResumeFileRepository {

    // 내용(bytea)을 읽지 않도록 id만 조회한다.
    @Override
    @Query("select r.id from ResumeFile r where r.objectKey is null")
    List<Long> findIdsNotMigrated();
}
