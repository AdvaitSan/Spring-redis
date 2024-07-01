package practice.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RedisTemplate<String, List<Student>> redisTemplate;

    RedisClient redisClient = RedisClient.create("redis://localhost:6379");
    StatefulRedisConnection<String, String> connection = redisClient.connect();

    public Student[] getAllStudents() {
        try {
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            String cachedStudentsJson = connection.sync().get("students:all");
            if (cachedStudentsJson != null) {
                ObjectMapper mapper = new ObjectMapper();
                Student[] cachedStudents = mapper.readValue(cachedStudentsJson, Student[].class);
                return cachedStudents; // Return cached array if available
            }

            List<Student> students = studentRepository.findAll();
            ObjectMapper mapper = new ObjectMapper();
            String studentsJson = mapper.writeValueAsString(students);
            connection.sync().set("students:all", studentsJson);
            connection.sync().expire("students:all", 60); // Set TTL to 60 seconds
            return students.toArray(new Student[0]); // Convert List to array
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving students", e);
        }
    }

    public Student getStudentById(Long id) {
        ObjectMapper mapper = new ObjectMapper(); // Declare and initialize the ObjectMapper
        try {
            String studentJson = connection.sync().get("student:" + id);
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
                    connection.sync().set("student:" + id, studentJsonString);
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
            students.add(savedStudent); // Add the new student to the list

            ObjectMapper mapper = new ObjectMapper();
            String studentJson = mapper.writeValueAsString(savedStudent);
            connection.sync().set("student:"+savedStudent.getId(), studentJson);

            String studentsJson = mapper.writeValueAsString(students);
            connection.sync().set("students:all", studentsJson);

            // Check and evict cache if size exceeds limit
            evictIfCacheSizeExceeds();

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
            redisTemplate.opsForValue().set("student:" + updatedStudent.getId(), Collections.singletonList(updatedStudent), 60, TimeUnit.SECONDS);

            // Check and evict cache if size exceeds limit
            evictIfCacheSizeExceeds();

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

            // Check and evict cache if size exceeds limit
            evictIfCacheSizeExceeds();
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

    private void evictIfCacheSizeExceeds() {
        try {
            long currentSize = redisClient.connect().sync().dbsize(); // Get current Redis DB size
            long maxSize = 1000; // Maximum size threshold in bytes

            if (currentSize > maxSize) {
                // Implement LRU eviction strategy
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> allKeysWithTTL = redisClient.connect().sync().dump(); // Get all keys with TTL

                List<String> keysToEvict = allKeysWithTTL.entrySet().stream()
                        .sorted(Comparator.comparingLong(entry -> Long.parseLong(entry.getValue().substring(0, entry.getValue().indexOf(',')))))
                        .limit(allKeysWithTTL.size() - 100)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                connection.sync().del(keysToEvict.toArray(new String[0]));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error evicting cache based on size", e);
        }
    }
}
