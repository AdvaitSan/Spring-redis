package practice.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private RedisTemplate<String, Student> redisTemplate;

    public List<Student> getAllStudents() {
        List<Student> cachedStudents = (List<Student>) redisTemplate.opsForValue().get("students:all");
        if (cachedStudents != null) {
            // Return cached data
            return cachedStudents;
        } else {
            // Retrieve data from database
            List<Student> students = studentRepository.findAll();
            // Cache the data
            redisTemplate.opsForValue().set("students:all", (Student) students, 60); // Cache for 1 minute
            return students;
        }
    }

    public Student getStudentById(Long id) {
        Student cachedStudent = redisTemplate.opsForValue().get("student:" + id);
        if (cachedStudent != null) {
            // Return cached data
            return cachedStudent;
        } else {
            // Retrieve data from database
            Student student = studentRepository.findById(id).orElseThrow();
            // Cache the data
            redisTemplate.opsForValue().set("student:" + id, student, 60); // Cache for 1 minute
            return student;
        }
    }

    public Student createStudent(Student student) {
        Student savedStudent = studentRepository.save(student);
        // Cache the data
        redisTemplate.opsForValue().set("student:" + savedStudent.getId(), savedStudent, 60); // Cache for 1 minute
        return savedStudent;
    }

    public Student updateStudent(Long id, Student student) {
        student.setId(id);
        Student updatedStudent = studentRepository.save(student);
        // Cache the data
        redisTemplate.opsForValue().set("student:" + updatedStudent.getId(), updatedStudent, 60); // Cache for 1 minute
        return updatedStudent;
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
        redisTemplate.delete("student:" + id);
    }

    @Async
    public void clearCache() {
        redisTemplate.delete("students:all");
        redisTemplate.delete("student:*");
    }
}