package com.learnhowyoulearn.mapper;

import com.learnhowyoulearn.dto.response.CourseResponse;
import com.learnhowyoulearn.dto.response.CourseSummaryResponse;
import com.learnhowyoulearn.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseSummaryResponse toSummary(Course course) {
        return CourseSummaryResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .goal(course.getGoal())
                .status(course.getStatus())
                .moduleOrder(course.getModuleOrder())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
