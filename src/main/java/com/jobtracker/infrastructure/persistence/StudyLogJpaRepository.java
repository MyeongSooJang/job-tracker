package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.domain.studylog.StudyLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyLogJpaRepository extends JpaRepository<StudyLog, Long>, StudyLogRepository {

    @Query("select distinct s.studyDate from StudyLog s where s.userId = :userId order by s.studyDate desc")
    List<LocalDate> findStudyDatesByUserId(@Param("userId") Long userId);

    @Query("select count(distinct s.studyDate) from StudyLog s "
            + "where s.userId = :userId and s.studyDate between :from and :to")
    long countStudyDatesBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
