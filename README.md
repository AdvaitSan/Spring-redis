<h1>Redis Spring Application using Lettuce and Maven</h1>
This is a Spring-based application that utilizes Redis as a caching layer to store key-value pairs, providing faster response times by reducing the load on the underlying database. 
The application uses Lettuce as the Redis client and is built using Maven.

### cluster command
docker exec -it redis1 redis-cli --cluster create redis1:6379 redis2:6379 redis3:6379 redis4:6379 redis5:6379 redis6:6379 --cluster-replicas 1 --cluster-yes
### master connection:
docker exec -it redis1 redis-cli -c 172.20.0.7:6379

 Student Service
================

#### Description

* Provides CRUD operations for students
* Uses Redis caching to improve performance
* Implemented using Spring Data Redis and Lettuce Redis client

#### Methods

##### `getAllStudents()`
Retrieves all students from Redis cache or database

##### `getStudentById(Long id)`
Retrieves a student by ID from Redis cache or database

##### `createStudent(Student student)`
Creates a new student and updates Redis cache

##### `updateStudent(Long id, Student student)`
Updates a student and updates Redis cache

##### `deleteStudent(Long id)`
Deletes a student and updates Redis cache

##### `clearCache()`
Clears all cached students

#### Redis Caching
--------------

* Uses Redis to cache student data for faster retrieval
* Cache keys:
	+ `students:all`: All students
	+ `student:<id>`: Individual student by ID
* Cache expiration: 60 seconds

#### Error Handling
----------------

* Catches and re-throws exceptions as `RuntimeException` with a descriptive message
