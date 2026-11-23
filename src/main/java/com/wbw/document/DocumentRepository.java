package com.wbw.document;

import com.wbw.folder.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    long countByFolderId(Long folderId);

    /**
     * filter: ALL(전체) / UNFILED(폴더 없음) / FOLDER(folderId 폴더)
     * q: 제목 검색어 (소문자, '%...%' 형태, null이면 검색하지 않음)
     */
    @Query(value = """
            select d.id as id, d.title as title, f.id as folderId, f.name as folderName, d.preview as preview,
                   d.charsWithSpaces as charsWithSpaces, d.charsWithoutSpaces as charsWithoutSpaces,
                   d.createdAt as createdAt, d.updatedAt as updatedAt
            from Document d left join d.folder f
            where d.userId = :userId
              and (:filter = 'ALL' or (:filter = 'UNFILED' and f.id is null) or (:filter = 'FOLDER' and f.id = :folderId))
              and (:q is null or lower(d.title) like :q escape '!')
            """,
            countQuery = """
            select count(d.id)
            from Document d left join d.folder f
            where d.userId = :userId
              and (:filter = 'ALL' or (:filter = 'UNFILED' and f.id is null) or (:filter = 'FOLDER' and f.id = :folderId))
              and (:q is null or lower(d.title) like :q escape '!')
            """)
    Page<DocumentSummaryView> search(@Param("userId") Long userId,
                                     @Param("filter") String filter,
                                     @Param("folderId") Long folderId,
                                     @Param("q") String q,
                                     Pageable pageable);

    /** 폴더 이동. 편집 중인 글의 버전이 바뀌어 자동 저장이 충돌하지 않도록 버전/수정시각을 건드리지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Document d set d.folder = :folder where d.id = :id and d.userId = :userId")
    int moveToFolder(@Param("id") Long id, @Param("userId") Long userId, @Param("folder") Folder folder);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Document d where d.folder.id = :folderId")
    int deleteByFolderIdBulk(@Param("folderId") Long folderId);
}
