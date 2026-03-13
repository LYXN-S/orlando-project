package com.orlandoprestige.orlandoproject.orders.internal.repository;

import com.orlandoprestige.orlandoproject.orders.internal.domain.POAllocationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface POAllocationLineRepository extends JpaRepository<POAllocationLine, Long> {
    List<POAllocationLine> findAllByPoReviewId(Long poReviewId);
    void deleteAllByPoReviewId(Long poReviewId);
}
