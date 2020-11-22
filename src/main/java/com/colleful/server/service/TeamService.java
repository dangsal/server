package com.colleful.server.service;

import com.colleful.server.domain.Team;
import com.colleful.server.domain.TeamMember;
import com.colleful.server.domain.TeamStatus;
import com.colleful.server.domain.User;
import com.colleful.server.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberService teamMemberService;

    public TeamService(TeamRepository teamRepository,
        TeamMemberService teamMemberService) {
        this.teamRepository = teamRepository;
        this.teamMemberService = teamMemberService;
    }

    public Boolean createTeam(Team team) {
        try {
            teamRepository.save(team);
            TeamMember member = new TeamMember(team, team.getLeader());
            return teamMemberService.addMember(member);
        } catch (Exception e) {
            return false;
        }
    }

    public Page<Team> getAllTeams(Pageable pageable) {
        return teamRepository.findAll(pageable);
    }

    public Optional<Team> getTeamInfo(Long id) {
        return teamRepository.findById(id);
    }

    public Page<Team> getAllReadyTeams(Pageable pageable) {
        return teamRepository.findAllByStatusOrderByUpdatedAtDesc(pageable, TeamStatus.READY);
    }

    public Page<Team> searchTeams(Pageable pageable, String teamName) {
        return teamRepository
            .findAllByStatusAndTeamNameContainingOrderByUpdatedAtDesc(pageable,
                TeamStatus.READY, teamName);
    }

    public List<Team> getAllTeamsByLeader(User leader) {
        return teamRepository.findAllByLeader(leader);
    }

    public Boolean changeTeamInfo(Team team, String teamName) {
        try {
            team.setTeamName(teamName);
            teamRepository.save(team);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean updateTeamStatus(Team team, TeamStatus status) {
        try {
            team.setStatus(status);
            teamRepository.save(team);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Boolean deleteTeam(Team team) {
        try {
            if (!clearMatch(team)) {
                return false;
            }
            teamRepository.deleteById(team.getId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean clearMatch(Team team) {
        try {
            if (team.getMatchedTeam() != null) {
                team.getMatchedTeam().setMatchedTeam(null);
                teamRepository.save(team);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Boolean saveMatchInfo(Team sender, Team receiver){
        try {
            sender.setMatchedTeam(receiver);
            sender.setStatus(TeamStatus.MATCHED);
            receiver.setMatchedTeam(sender);
            receiver.setStatus(TeamStatus.MATCHED);
            teamRepository.save(sender);
            teamRepository.save(receiver);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}