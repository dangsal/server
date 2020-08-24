package com.ocupid.server.repository;

import com.ocupid.server.domain.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> getAllByStatusOrderByUpdatedAtDesc(String status);
}
