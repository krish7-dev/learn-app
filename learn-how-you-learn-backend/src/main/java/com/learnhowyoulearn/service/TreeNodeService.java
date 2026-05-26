package com.learnhowyoulearn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.response.TreeNodeDetailResponse;
import com.learnhowyoulearn.dto.response.TreeNodeResponse;
import com.learnhowyoulearn.entity.*;
import com.learnhowyoulearn.entity.LectureStatus;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.*;
import com.learnhowyoulearn.util.TopicNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeNodeService {

    private static final List<String> STATUS_PRIORITY = List.of(
            "WEAK_AREA", "REVISION_DUE", "MASTERED", "REVISION_DONE",
            "LEARNING", "NOTES_GENERATED", "TRANSCRIPT_ADDED", "NO_NOTES"
    );

    private final TreeNodeRepository treeNodeRepository;
    private final TopicRepository topicRepository;
    private final LectureRepository lectureRepository;
    private final LectureTopicRepository lectureTopicRepository;
    private final CourseRepository courseRepository;
    private final WeakAreaRepository weakAreaRepository;
    private final RevisionItemRepository revisionItemRepository;
    private final TopicNormalizer topicNormalizer;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upsertTreePath(Long userId, List<String> path, Long topicId) {
        if (path == null || path.isEmpty()) return;

        Long parentId = null;
        for (int i = 0; i < path.size(); i++) {
            String label = path.get(i);
            String normLabel = topicNormalizer.normalize(label);
            boolean isLeaf = (i == path.size() - 1);
            String nodeType = isLeaf ? "CONCEPT" : (i == 0 ? "ROOT" : "INTERMEDIATE");

            TreeNode node = findOrCreate(userId, parentId, label, normLabel, nodeType, i);

            if (isLeaf) {
                if (node.getTopicId() != null && !node.getTopicId().equals(topicId)) {
                    log.warn("Tree node {} already linked to topic {}; skipping link to topic {}",
                            node.getId(), node.getTopicId(), topicId);
                } else {
                    node.setTopicId(topicId);
                    node.setNodeType("CONCEPT");
                    treeNodeRepository.save(node);
                }
            }
            parentId = node.getId();
        }
    }

    @Transactional
    public void createShellNode(Long userId, String courseTitle, String moduleName,
                                Long lectureId, String lectureTitle, int position) {
        // ROOT
        String courseNorm = topicNormalizer.normalize(courseTitle);
        TreeNode root = findOrCreate(userId, null, courseTitle, courseNorm, "ROOT", 0);

        // INTERMEDIATE
        String moduleNorm = topicNormalizer.normalize(moduleName);
        TreeNode module = findOrCreate(userId, root.getId(), moduleName, moduleNorm, "INTERMEDIATE", 0);

        // SHELL — idempotent: skip if lectureId already linked
        String shellNorm = topicNormalizer.normalize(lectureTitle);
        Optional<TreeNode> existing = treeNodeRepository
                .findByUserIdAndParentIdAndNormalizedLabel(userId, module.getId(), shellNorm);

        if (existing.isPresent()) {
            TreeNode node = existing.get();
            if (node.getLectureId() != null && node.getLectureId().equals(lectureId)) {
                return; // already exists
            }
        }

        // Only create a new shell if one with this normalized label doesn't already exist under the module
        if (existing.isEmpty()) {
            TreeNode shell = TreeNode.builder()
                    .userId(userId)
                    .parentId(module.getId())
                    .label(lectureTitle)
                    .normalizedLabel(shellNorm)
                    .nodeType("SHELL")
                    .lectureId(lectureId)
                    .topicId(null)
                    .position(position)
                    .build();
            treeNodeRepository.save(shell);
        }
    }

    @Transactional
    public Map<String, Integer> reset(Long userId) {
        treeNodeRepository.deleteByUserId(userId);
        int conceptCount = backfill(userId);
        return Map.of("shellsCreated", 0, "conceptsCreated", conceptCount);
    }

    @Transactional
    public int backfill(Long userId) {
        // Bulk-load all data needed for backfill
        List<LectureTopic> lectureTopics = lectureTopicRepository.findAllByUserId(userId);
        if (lectureTopics.isEmpty()) return 0;

        Set<Long> lectureIds = lectureTopics.stream().map(LectureTopic::getLectureId).collect(Collectors.toSet());
        Set<Long> topicIds   = lectureTopics.stream().map(LectureTopic::getTopicId).collect(Collectors.toSet());

        Map<Long, Lecture> lectureMap = lectureRepository.findAllByUser(userId).stream()
                .filter(l -> lectureIds.contains(l.getId()))
                .collect(Collectors.toMap(Lecture::getId, l -> l));

        Map<Long, Topic> topicMap = topicRepository.findAllByUserId(userId).stream()
                .filter(t -> topicIds.contains(t.getId()))
                .collect(Collectors.toMap(Topic::getId, t -> t));

        Set<Long> courseIds = lectureMap.values().stream()
                .map(Lecture::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> courseTitleMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));

        // Deduplicate: per topicId, prefer the LectureTopic that has a stored treePath
        Map<Long, LectureTopic> bestByTopic = new LinkedHashMap<>();
        for (LectureTopic lt : lectureTopics) {
            bestByTopic.compute(lt.getTopicId(), (k, existing) -> {
                if (existing == null) return lt;
                if (existing.getTreePath() == null && lt.getTreePath() != null) return lt;
                return existing;
            });
        }

        int created = 0;
        for (Map.Entry<Long, LectureTopic> entry : bestByTopic.entrySet()) {
            Long topicId = entry.getKey();
            LectureTopic lt = entry.getValue();
            Topic topic = topicMap.get(topicId);
            Lecture lecture = lectureMap.get(lt.getLectureId());
            if (topic == null || lecture == null) continue;

            String courseTitle = lecture.getCourseId() != null
                    ? courseTitleMap.getOrDefault(lecture.getCourseId(), "Course")
                    : "Course";

            List<String> path = null;
            if (lt.getTreePath() != null) {
                try {
                    List<String> parsed = objectMapper.readValue(lt.getTreePath(), new TypeReference<List<String>>() {});
                    if (!parsed.isEmpty()) {
                        path = new ArrayList<>(parsed);
                        path.set(0, courseTitle);
                    }
                } catch (Exception ignored) {}
            }
            if (path == null) {
                path = List.of(courseTitle, topic.getName());
            }

            upsertTreePath(userId, path, topicId);
            created++;
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<TreeNodeResponse> getFullTree(Long userId) {
        List<TreeNode> allNodes = treeNodeRepository.findByUserId(userId);
        if (allNodes.isEmpty()) return Collections.emptyList();

        // Bulk-load all related data — no N+1 queries
        Map<Long, Topic> topicMap = topicRepository.findAllByUserId(userId)
                .stream().collect(Collectors.toMap(Topic::getId, t -> t));

        Map<Long, Lecture> lectureMap = lectureRepository.findAllByUser(userId)
                .stream().collect(Collectors.toMap(Lecture::getId, l -> l));

        LocalDateTime now = LocalDateTime.now();
        List<WeakArea> allWeakAreas = weakAreaRepository
                .findByUserIdAndStatusOrderBySeverityDescCreatedAtDesc(userId, "ACTIVE");
        List<RevisionItem> pendingRevisions = revisionItemRepository
                .findAllByUserIdAndStatus(userId, "PENDING");

        Map<Long, List<WeakArea>> weakByTopic = allWeakAreas.stream()
                .filter(w -> w.getTopicId() != null)
                .collect(Collectors.groupingBy(WeakArea::getTopicId));

        Map<Long, List<WeakArea>> weakByLecture = allWeakAreas.stream()
                .filter(w -> w.getLectureId() != null)
                .collect(Collectors.groupingBy(WeakArea::getLectureId));

        Map<Long, List<RevisionItem>> revisionByTopic = pendingRevisions.stream()
                .filter(r -> r.getTopicId() != null && r.getDueAt() != null && r.getDueAt().isBefore(now))
                .collect(Collectors.groupingBy(RevisionItem::getTopicId));

        // Build node responses keyed by id
        Map<Long, TreeNodeResponse> responseMap = new LinkedHashMap<>();
        for (TreeNode node : allNodes) {
            responseMap.put(node.getId(), computeLeafStats(node, topicMap, lectureMap,
                    weakByTopic, weakByLecture, revisionByTopic));
        }

        // Build parent-child relationships
        Map<Long, List<Long>> childrenByParent = new LinkedHashMap<>();
        for (TreeNode node : allNodes) {
            if (node.getParentId() != null) {
                childrenByParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node.getId());
            }
        }

        // Compute depth of each node for bottom-up aggregation
        Map<Long, Long> parentOf = allNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.toMap(TreeNode::getId, TreeNode::getParentId));

        Map<Long, Integer> depthMap = new HashMap<>();
        for (TreeNode node : allNodes) {
            int depth = 0;
            Long current = node.getId();
            while (parentOf.containsKey(current)) {
                depth++;
                current = parentOf.get(current);
                if (depth > 50) break; // guard against cycles
            }
            depthMap.put(node.getId(), depth);
        }

        // Sort descending by depth so leaves are processed before their parents
        List<Long> processOrder = allNodes.stream()
                .map(TreeNode::getId)
                .sorted(Comparator.comparingInt((Long id) -> depthMap.getOrDefault(id, 0)).reversed())
                .collect(Collectors.toList());

        // Aggregate children stats into parents
        for (Long nodeId : processOrder) {
            List<Long> children = childrenByParent.get(nodeId);
            if (children == null || children.isEmpty()) continue;

            List<TreeNodeResponse> childResponses = children.stream()
                    .map(responseMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(TreeNodeResponse::getPosition))
                    .collect(Collectors.toList());

            TreeNodeResponse parent = responseMap.get(nodeId);
            if (parent == null) continue;

            String aggregateStatus = childResponses.stream()
                    .map(TreeNodeResponse::getStatus)
                    .min(Comparator.comparingInt(s -> {
                        int idx = STATUS_PRIORITY.indexOf(s);
                        return idx == -1 ? STATUS_PRIORITY.size() : idx;
                    }))
                    .orElse("NO_NOTES");

            double avgProgress = childResponses.stream()
                    .mapToDouble(TreeNodeResponse::getProgressPercent).average().orElse(0.0);

            int totalWeak = childResponses.stream().mapToInt(TreeNodeResponse::getWeakAreaCount).sum();
            int totalRevision = childResponses.stream().mapToInt(TreeNodeResponse::getRevisionDueCount).sum();
            int totalLectures = childResponses.stream().mapToInt(TreeNodeResponse::getLinkedLectureCount).sum();

            TreeNodeResponse updated = TreeNodeResponse.builder()
                    .id(parent.getId())
                    .label(parent.getLabel())
                    .nodeType(parent.getNodeType())
                    .topicId(parent.getTopicId())
                    .lectureId(parent.getLectureId())
                    .position(parent.getPosition())
                    .status(aggregateStatus)
                    .progressPercent(Math.round(avgProgress * 10.0) / 10.0)
                    .weakAreaCount(totalWeak)
                    .revisionDueCount(totalRevision)
                    .linkedLectureCount(totalLectures)
                    .children(childResponses)
                    .build();

            responseMap.put(nodeId, updated);
        }

        // Collect root nodes (parentId == null) sorted by position
        return allNodes.stream()
                .filter(n -> n.getParentId() == null)
                .sorted(Comparator.comparingInt(TreeNode::getPosition))
                .map(n -> responseMap.get(n.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TreeNodeDetailResponse getNodeDetail(Long userId, Long nodeId) {
        TreeNode node = treeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree node not found: " + nodeId));
        if (!node.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Tree node not found: " + nodeId);
        }

        LocalDateTime now = LocalDateTime.now();
        TreeNodeDetailResponse.TreeNodeDetailResponseBuilder builder = TreeNodeDetailResponse.builder()
                .id(node.getId())
                .label(node.getLabel())
                .nodeType(node.getNodeType());

        String status = "NO_NOTES";
        double progress = 0.0;
        int weakCount = 0;
        int revCount = 0;

        if (node.getTopicId() != null) {
            Topic topic = topicRepository.findById(node.getTopicId()).orElse(null);
            if (topic != null) {
                List<WeakArea> weakAreas = weakAreaRepository
                        .findByUserIdAndTopicIdAndStatus(userId, topic.getId(), "ACTIVE");
                List<RevisionItem> dueRevisions = revisionItemRepository
                        .findAllByUserIdAndStatus(userId, "PENDING").stream()
                        .filter(r -> node.getTopicId().equals(r.getTopicId())
                                && r.getDueAt() != null && r.getDueAt().isBefore(now))
                        .toList();

                weakCount = weakAreas.size();
                revCount = dueRevisions.size();
                progress = topic.getMasteryScore();

                String topicStatus = topic.getStatus();
                if (!weakAreas.isEmpty()) {
                    status = "WEAK_AREA";
                } else if (!dueRevisions.isEmpty()) {
                    status = "REVISION_DUE";
                } else if ("MASTERED".equals(topicStatus)) {
                    status = "MASTERED";
                } else {
                    boolean hasDoneRevision = revisionItemRepository
                            .findAllByUserIdAndStatus(userId, "COMPLETED").stream()
                            .anyMatch(r -> node.getTopicId().equals(r.getTopicId()));
                    if (hasDoneRevision) {
                        status = "REVISION_DONE";
                    } else if ("LEARNING".equals(topicStatus) || "REVISING".equals(topicStatus)) {
                        status = "LEARNING";
                    } else {
                        status = "NOTES_GENERATED";
                    }
                }

                LocalDate nextRevDue = dueRevisions.stream()
                        .map(r -> r.getDueAt().toLocalDate())
                        .min(LocalDate::compareTo)
                        .orElse(null);

                String severity = weakAreas.stream().findFirst().map(WeakArea::getSeverity).orElse(null);

                builder.topicId(topic.getId())
                        .topicName(topic.getName())
                        .topicStatus(topic.getStatus())
                        .masteryScore(topic.getMasteryScore())
                        .nextRevisionDue(nextRevDue)
                        .weakAreaSeverity(severity);

                // Find the lecture this topic was taught in
                lectureTopicRepository.findByUserIdAndTopicId(userId, topic.getId())
                        .stream().findFirst()
                        .ifPresent(lt -> lectureRepository.findById(lt.getLectureId()).ifPresent(lec ->
                                builder.lectureId(lec.getId())
                                       .lectureTitle(lec.getTitle())
                                       .contentStatus(lec.getContentStatus())
                                       .estimatedMinutes(lec.getEstimatedMinutes())
                        ));
            }
        } else if (node.getLectureId() != null) {
            Lecture lecture = lectureRepository.findById(node.getLectureId()).orElse(null);
            if (lecture != null) {
                List<WeakArea> weakAreas = weakAreaRepository
                        .findByUserIdAndLectureIdAndStatus(userId, lecture.getId(), "ACTIVE");
                weakCount = weakAreas.size();

                String contentStatus = lecture.getContentStatus();
                if (LectureStatus.COMPLETED == lecture.getStatus()) {
                    status = "MASTERED";
                    progress = 100.0;
                } else if ("NOTES_GENERATED".equals(contentStatus)) {
                    status = "NOTES_GENERATED";
                    progress = 60.0;
                } else if ("TRANSCRIPT_ADDED".equals(contentStatus)) {
                    status = "TRANSCRIPT_ADDED";
                    progress = 30.0;
                } else {
                    status = "NO_NOTES";
                    progress = 0.0;
                }

                builder.lectureId(lecture.getId())
                        .lectureTitle(lecture.getTitle())
                        .contentStatus(contentStatus)
                        .estimatedMinutes(lecture.getEstimatedMinutes());
            }
        }

        return builder
                .status(status)
                .progressPercent(progress)
                .weakAreaCount(weakCount)
                .revisionDueCount(revCount)
                .build();
    }

    private TreeNodeResponse computeLeafStats(TreeNode node,
                                               Map<Long, Topic> topicMap,
                                               Map<Long, Lecture> lectureMap,
                                               Map<Long, List<WeakArea>> weakByTopic,
                                               Map<Long, List<WeakArea>> weakByLecture,
                                               Map<Long, List<RevisionItem>> revisionByTopic) {
        String status = "NO_NOTES";
        double progress = 0.0;
        int weakCount = 0;
        int revCount = 0;
        int lectureCount = 0;

        if (node.getTopicId() != null) {
            Topic topic = topicMap.get(node.getTopicId());
            if (topic != null) {
                List<WeakArea> weakAreas = weakByTopic.getOrDefault(topic.getId(), List.of());
                List<RevisionItem> dueRevisions = revisionByTopic.getOrDefault(topic.getId(), List.of());
                weakCount = weakAreas.size();
                revCount = dueRevisions.size();
                progress = topic.getMasteryScore();

                String topicStatus = topic.getStatus();
                if (!weakAreas.isEmpty()) {
                    status = "WEAK_AREA";
                } else if (!dueRevisions.isEmpty()) {
                    status = "REVISION_DUE";
                } else if ("MASTERED".equals(topicStatus)) {
                    status = "MASTERED";
                } else if ("LEARNING".equals(topicStatus) || "REVISING".equals(topicStatus)) {
                    status = "LEARNING";
                } else {
                    status = "NOTES_GENERATED";
                }
            }
            if (node.getLectureId() != null) lectureCount = 1;
        } else if (node.getLectureId() != null) {
            Lecture lecture = lectureMap.get(node.getLectureId());
            lectureCount = 1;
            if (lecture != null) {
                weakCount = weakByLecture.getOrDefault(lecture.getId(), List.of()).size();
                String contentStatus = lecture.getContentStatus();
                if (LectureStatus.COMPLETED == lecture.getStatus()) {
                    status = "MASTERED"; progress = 100.0;
                } else if ("NOTES_GENERATED".equals(contentStatus)) {
                    status = "NOTES_GENERATED"; progress = 60.0;
                } else if ("TRANSCRIPT_ADDED".equals(contentStatus)) {
                    status = "TRANSCRIPT_ADDED"; progress = 30.0;
                } else {
                    status = "NO_NOTES"; progress = 0.0;
                }
            }
        }

        return TreeNodeResponse.builder()
                .id(node.getId())
                .label(node.getLabel())
                .nodeType(node.getNodeType())
                .topicId(node.getTopicId())
                .lectureId(node.getLectureId())
                .position(node.getPosition())
                .status(status)
                .progressPercent(progress)
                .weakAreaCount(weakCount)
                .revisionDueCount(revCount)
                .linkedLectureCount(lectureCount)
                .children(new ArrayList<>())
                .build();
    }

    private TreeNode findOrCreate(Long userId, Long parentId, String label, String normLabel,
                                   String nodeType, int position) {
        Optional<TreeNode> existing = parentId == null
                ? treeNodeRepository.findByUserIdAndParentIdIsNullAndNormalizedLabel(userId, normLabel)
                : treeNodeRepository.findByUserIdAndParentIdAndNormalizedLabel(userId, parentId, normLabel);

        return existing.orElseGet(() -> treeNodeRepository.save(
                TreeNode.builder()
                        .userId(userId)
                        .parentId(parentId)
                        .label(label)
                        .normalizedLabel(normLabel)
                        .nodeType(nodeType)
                        .position(position)
                        .build()
        ));
    }
}
