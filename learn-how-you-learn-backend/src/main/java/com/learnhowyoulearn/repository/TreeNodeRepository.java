package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.TreeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreeNodeRepository extends JpaRepository<TreeNode, Long> {

    List<TreeNode> findByUserId(Long userId);

    Optional<TreeNode> findByUserIdAndParentIdIsNullAndNormalizedLabel(Long userId, String normalizedLabel);

    Optional<TreeNode> findByUserIdAndParentIdAndNormalizedLabel(Long userId, Long parentId, String normalizedLabel);

    @Query("SELECT t FROM TreeNode t WHERE t.userId = :userId AND t.lectureId IN :lectureIds")
    List<TreeNode> findByUserIdAndLectureIdIn(@Param("userId") Long userId, @Param("lectureIds") List<Long> lectureIds);

    @Query("SELECT t FROM TreeNode t WHERE t.id IN :ids")
    List<TreeNode> findAllByIdIn(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TreeNode t WHERE t.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
