package com.wbw.share;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {

    Optional<Share> findByToken(String token);

    List<Share> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    @Query("select s from Share s join fetch s.document d where s.id = :id and d.userId = :userId")
    Optional<Share> findByIdAndOwner(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Share s where s.document.id = :documentId")
    int deleteByDocumentIdBulk(@Param("documentId") Long documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Share s where s.document.id in (select d.id from Document d where d.folder.id = :folderId)")
    int deleteByFolderIdBulk(@Param("folderId") Long folderId);
}
