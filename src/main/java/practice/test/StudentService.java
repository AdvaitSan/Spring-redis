package practice.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RedisTemplate<String, List<Student>> redisTemplate;

    @Cacheable(value = "students", key = "'all'")
    public List<Student> getAllStudents() {
        try {
            List<Student> cachedStudents = redisTemplate.opsForValue().get("students:all");
            if (cachedStudents != null) {
                return cachedStudents; // Return cached list if available
            }

            List<Student> students = studentRepository.findAll();
            redisTemplate.opsForValue().set("students:all", students, 60, TimeUnit.SECONDS);
            return students;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving students", e);
        }
    }

    @Cacheable(value = "students", key = "#id")
    public Student getStudentById(Long id) {
        try {
            Student cachedStudent = (Student) redisTemplate.opsForValue().get("student:" + id);
            if (cachedStudent != null) {
                return cachedStudent;
            }

            Student student = studentRepository.findById(id).orElse(null);
            if (student != null) {
                redisTemplate.opsForValue().set("student:" + id, Collections.singletonList(student), 60, TimeUnit.SECONDS);
            }
            return student;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving student by ID", e);
        }
    }

    @Transactional
    @CachePut(value = "students", key = "'all'")
    public Student createStudent(Student student) {
        try {
            Student savedStudent = studentRepository.save(student);
            // Update the cache entry for "students:all" to include the new student
            List<Student> students = studentRepository.findAll();
            students.add(savedStudent); // Add the new student to the list
            redisTemplate.opsForValue().set("students:all", students, 60, TimeUnit.SECONDS);
            return savedStudent; // Return the created student details
        } catch (Exception e) {
            throw new RuntimeException("Error creating student", e);
        }
    }



    @Transactional
    @CachePut(value = "students", key = "#id")
    public Student updateStudent(Long id, Student student) {
        try {
            student.setId(id);
            Student updatedStudent = studentRepository.save(student);
            redisTemplate.opsForValue().set("student:" + updatedStudent.getId(), Collections.singletonList(updatedStudent), 60, TimeUnit.SECONDS);
            return updatedStudent;
        } catch (Exception e) {
            throw new RuntimeException("Error updating student", e);
        }
    }

    @Transactional
    @CacheEvict(value = "students", key = "'all'")
    public void deleteStudent(Long id) {
        try {
            studentRepository.deleteById(id);
            // Fetch all students after deletion to ensure cache consistency
            List<Student> students = studentRepository.findAll();
            redisTemplate.opsForValue().set("students:all", students, 60, TimeUnit.SECONDS);
            redisTemplate.delete("student:" + id);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting student", e);
        }
    }

    @CacheEvict(value = "students", allEntries = true)
    public void clearCache() {
        try {
            // This method clears all cached students
        } catch (Exception e) {
            throw new RuntimeException("Error clearing cache", e);
        }
    }
}
