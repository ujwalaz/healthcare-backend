package com.healthcare.repository;

import com.healthcare.entity.PmrEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PmrEntryRepository extends JpaRepository<PmrEntry, Long> {

    Page<PmrEntry> findByPmrId(Long pmrId, Pageable pageable);
}
