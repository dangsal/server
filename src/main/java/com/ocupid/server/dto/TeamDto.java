package com.ocupid.server.dto;

import com.ocupid.server.domain.Team;
import com.ocupid.server.domain.TeamMember;
import com.ocupid.server.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import lombok.Getter;

public class TeamDto {

    @Getter
    private static class SimplifiedUser {

        private final Long id;
        private final String nickname;
        private final Integer age;
        private final String gender;
        private final String college;

        public SimplifiedUser(User user) {
            this.id = user.getId();
            this.nickname = user.getNickname();
            this.age = Calendar.getInstance().get(Calendar.YEAR) - user.getBirthYear() + 1;
            this.gender = user.getGender();
            this.college = user.getCollege();
        }
    }

    @Getter
    public static class Request {

        private String teamName;

        public Team toEntity(User leader) {
            Team team = new Team();
            team.setTeamName(teamName);
            team.setGender(leader.getGender());
            team.setCollege(leader.getCollege());
            team.setStatus("pending");
            team.setLeader(leader);
            return team;
        }
    }

    @Getter
    public static class Response {

        private final Long id;
        private final LocalDateTime updatedAt;
        private final String teamName;
        private final Integer headcount;
        private final String gender;
        private final String college;
        private final String status;
        private final Long leaderId;
        private final Double averageAge;
        private final List<SimplifiedUser> members;

        public Response(Team team) {
            Integer sumOfAge = 0;
            this.id = team.getId();
            this.updatedAt = team.getUpdatedAt();
            this.teamName = team.getTeamName();
            this.headcount = team.getMembers().size();
            this.gender = team.getGender();
            this.college = team.getCollege();
            this.status = team.getStatus();
            this.leaderId = team.getLeader().getId();
            this.members = new ArrayList<>();
            for (TeamMember member : team.getMembers()) {
                SimplifiedUser simplifiedUser = new SimplifiedUser(member.getMember());
                sumOfAge += simplifiedUser.getAge();
                members.add(simplifiedUser);
            }
            this.averageAge = sumOfAge.doubleValue() / team.getMembers().size();
        }
    }
}
