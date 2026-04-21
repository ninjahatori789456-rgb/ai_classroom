package com.remoteclassroom.backend.repository;

import com.remoteclassroom.backend.model.Attendance;
import com.remoteclassroom.backend.model.LiveClass;
import com.remoteclassroom.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByLiveClassAndStudent(LiveClass liveClass, User student);

    List<Attendance> findByLiveClass(LiveClass liveClass);
}
