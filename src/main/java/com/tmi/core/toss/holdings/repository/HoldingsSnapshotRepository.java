package com.tmi.core.toss.holdings.repository;

import com.tmi.core.toss.holdings.domain.HoldingsSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingsSnapshotRepository extends JpaRepository<HoldingsSnapshotEntity, Long> {
}
