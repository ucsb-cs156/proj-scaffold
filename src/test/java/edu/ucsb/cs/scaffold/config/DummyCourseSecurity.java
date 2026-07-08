package edu.ucsb.cs.scaffold.config;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;

@TestComponent
public class DummyCourseSecurity {

  @Autowired CourseRepository courseRepository;

  @Autowired RosterStudentRepository rosterStudentRepository;

  @Autowired ConceptRepository conceptRepository;

  @Autowired ConceptEdgeRepository conceptEdgeRepository;

  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public Course loadCourse(Long courseId) {
    /*
    This method simply exists to add the preauthorization annotation so that the method can be tested directly.
     */
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public boolean nullTest(Long courseId) {
    if (courseRepository.findById(courseId).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public Course loadCourseInstructor(Long courseId) {
    /*
    This method simply exists to add the preauthorization annotation so that the method can be tested directly.
     */
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public boolean nullTestInstructor(Long courseId) {
    if (courseRepository.findById(courseId).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  public RosterStudent loadRosterStudent(Long id) {
    return rosterStudentRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
  }

  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #id)")
  public Concept loadConcept(Long id) {
    return conceptRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(Concept.class, id));
  }

  @PreAuthorize("@CourseSecurity.hasConceptEdgeManagementPermissions(#root, #id)")
  public ConceptEdge loadConceptEdge(Long id) {
    return conceptEdgeRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(ConceptEdge.class, id));
  }

  @Bean
  public static RoleHierarchy loadedRoleHierarchy() {
    return SecurityConfig.roleHierarchy();
  }
}
