package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.comment.LoteComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoteCommentRepository extends JpaRepository<LoteComment, UUID> {
    List<LoteComment> findByLoteOrderByCreatedAtAsc(String lote);
}
