package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.studylog.StudyLog;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyLogJpaRepository extends JpaRepository<StudyLog, Long>, StudyLogRepository {

    @Override
    @Query("select s from StudyLog s where s.userId = :userId and s.studyDate between :from and :to "
            + "order by s.studyDate desc, s.id desc")
    List<StudyLog> findInPeriod(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct s.studyDate from StudyLog s where s.userId = :userId order by s.studyDate desc")
    List<LocalDate> findStudyDatesByUserId(@Param("userId") Long userId);

    @Query("select count(distinct s.studyDate) from StudyLog s "
            + "where s.userId = :userId and s.studyDate between :from and :to")
    long countStudyDatesBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
