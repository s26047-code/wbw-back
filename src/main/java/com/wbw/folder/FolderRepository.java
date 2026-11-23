package com.wbw.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);

    @Query("select coalesce(max(f.sortOrder), -1) from Folder f where f.userId = :userId")
    int findMaxSortOrder(@Param("userId") Long userId);

    List<Folder> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    /** [폴더 ID, 글 개수] 목록 */
    @Query("""
            select d.folder.id, count(d.id) from Document d
            where d.userId = :userId and d.folder is not null
            group by d.folder.id
            """)
    List<Object[]> countDocumentsByFolder(@Param("userId") Long userId);
}
