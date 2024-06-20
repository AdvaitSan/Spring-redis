package practice.test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Async
    List<Student> findAll();

    @Async
    Optional<Student> findById(Long id);

    Student save(Student student);
}