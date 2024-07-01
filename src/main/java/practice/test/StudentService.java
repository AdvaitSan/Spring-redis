package practice.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
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
    private RedisTemplate<String, String> template;

    private final ObjectMapper mapper = new ObjectMapper();

    public Student[] getAllStudents() {
        try {
            String cachedStudentsJson = template.opsForValue().get("students:all");
            if (cachedStudentsJson != null) {
                Student[] cachedStudents = mapper.readValue(cachedStudentsJson, Student[].class);
                return cachedStudents; // Return cached array if available
            }

            List<Student> students = studentRepository.findAll();
            String studentsJson = mapper.writeValueAsString(students);
            template.opsForValue().set("students:all", studentsJson);
            template.expire("students:all", 60, TimeUnit.SECONDS); // Set TTL to 60 seconds
            return students.toArray(new Student[0]); // Convert List to array
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving students", e);
        }
    }

    public Student getStudentById(Long id) {
        try {
            String studentJson = template.opsForValue().get("student:" + id);
            if (studentJson != null) {
                Student student = mapper.readValue(studentJson, Student.class);
                return student;
            } else {
                // If not found in Redis, retrieve from database
                Student student = studentRepository.findById(id).orElse(null);
                if (student == null) {
                    // If student ID does not exist, return null
                    return null;
                } else {
                    String studentJsonString = mapper.writeValueAsString(student);
                    template.opsForValue().set("student:" + id, studentJsonString);
                    return student;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving student by ID", e);
        }
    }

    @Transactional
    public Student createStudent(Student student) {
        try {
            Student savedStudent = studentRepository.save(student);
            // Update the cache entry for "students:all" to include the new student
            List<Student> students = studentRepository.findAll();
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
            connectionFactory.afterPropertiesSet();

            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setDefaultSerializer(StringRedisSerializer.UTF_8);
            template.afterPropertiesSet();

            students.add(savedStudent); // Add the new student to the list

            String studentJson = mapper.writeValueAsString(savedStudent);
            template.opsForValue().set("student:" + savedStudent.getId(), studentJson);

            String studentsJson = mapper.writeValueAsString(students);
            template.opsForValue().set("students:all", studentsJson);

            return savedStudent; // Return the created student details
        } catch (Exception e) {
            throw new RuntimeException("Error creating student", e);
        }
    }

    @Transactional
    public Student updateStudent(Long id, Student student) {
        try {
            student.setId(id);
            Student updatedStudent = studentRepository.save(student);
            template.opsForValue().set("student:" + updatedStudent.getId(), mapper.writeValueAsString(updatedStudent), 60, TimeUnit.SECONDS);
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
            template.opsForValue().set("students:all", mapper.writeValueAsString(students), 60, TimeUnit.SECONDS);
            template.delete("student:" + id);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting student", e);
        }
    }

    @CacheEvict(value = "students", allEntries = true)
    public void clearCache() {
        try {
            // This method clears all cached students
            template.delete("students:all");
            template.keys("student:*").forEach(key -> template.delete(key));
        } catch (Exception e) {
            throw new RuntimeException("Error clearing cache", e);
        }
    }
}