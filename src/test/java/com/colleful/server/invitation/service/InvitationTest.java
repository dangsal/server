package com.colleful.server.invitation.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colleful.server.invitation.repository.InvitationRepository;
import com.colleful.server.team.domain.Team;
import com.colleful.server.team.domain.TeamStatus;
import com.colleful.server.team.service.TeamServiceForService;
import com.colleful.server.user.domain.Gender;
import com.colleful.server.user.domain.User;
import com.colleful.server.user.service.UserServiceForService;
import com.colleful.server.global.exception.ForbiddenBehaviorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InvitationTest {

    @InjectMocks
    private InvitationServiceImpl invitationServiceImpl;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private TeamServiceForService teamService;
    @Mock
    private UserServiceForService userService;

    @Test
    public void 초대() {
        when(teamService.getUserTeam(1L))
            .thenReturn(Team.builder()
                .id(1L)
                .leaderId(1L)
                .gender(Gender.MALE)
                .status(TeamStatus.PENDING)
                .build());
        when(userService.getUser(2L))
            .thenReturn(User.builder().id(2L).gender(Gender.MALE).build());

        invitationServiceImpl.invite(2L, 1L);

        verify(invitationRepository).save(any());
    }

    @Test
    public void 팀이_있는_사용자_초대() {
        when(teamService.getUserTeam(1L))
            .thenReturn(Team.builder()
                .id(1L)
                .leaderId(1L)
                .gender(Gender.MALE)
                .status(TeamStatus.PENDING)
                .build());
        when(userService.getUser(2L))
            .thenReturn(User.builder().id(2L).gender(Gender.MALE).teamId(2L).build());

        assertThatThrownBy(() -> invitationServiceImpl.invite(2L, 1L))
            .isInstanceOf(ForbiddenBehaviorException.class);
    }

    @Test
    public void 다른_성별_초대() {
        when(teamService.getUserTeam(1L))
            .thenReturn(Team.builder()
                .id(1L)
                .leaderId(1L)
                .gender(Gender.FEMALE)
                .status(TeamStatus.PENDING)
                .build());
        when(userService.getUser(2L))
            .thenReturn(User.builder().id(2L).gender(Gender.MALE).build());

        assertThatThrownBy(() -> invitationServiceImpl.invite(2L, 1L))
            .isInstanceOf(ForbiddenBehaviorException.class);
    }

    @Test
    public void 리더가_아닌_사용자가_초대() {
        when(teamService.getUserTeam(1L))
            .thenReturn(Team.builder()
                .id(1L)
                .leaderId(3L)
                .gender(Gender.MALE)
                .status(TeamStatus.PENDING)
                .build());

        assertThatThrownBy(() -> invitationServiceImpl.invite(2L, 1L))
            .isInstanceOf(ForbiddenBehaviorException.class);
    }
}
