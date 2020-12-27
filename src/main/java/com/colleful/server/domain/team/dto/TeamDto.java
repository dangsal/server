package com.colleful.server.domain.team.dto;

import com.colleful.server.domain.team.domain.Team;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

public class TeamDto {

    @Getter
    @Builder
    public static class Request {

        private final String teamName;
        private final String status;
    }

    @Getter
    public static class Response {

        private final Long id;
        private final LocalDateTime updatedAt;
        private final String teamName;
        private final String gender;
        private final String status;
        private final Long leaderId;

        public Response(Team team) {
            this.id = team.getId();
            this.updatedAt = team.getUpdatedAt();
            this.teamName = team.getTeamName();
            this.gender = team.getGender().name();
            this.status = team.getStatus().name();
            this.leaderId = team.getLeaderId();
        }
    }
}
