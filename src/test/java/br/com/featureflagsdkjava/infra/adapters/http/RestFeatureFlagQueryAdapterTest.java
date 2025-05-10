package br.com.featureflagsdkjava.infra.adapters.http;

import br.com.featureflagsdkjava.domain.model.Flag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestFeatureFlagQueryAdapterTest {

    @Mock
    private FeatureFlagServiceProxy serviceClient;

    @InjectMocks
    private RestFeatureFlagQueryAdapter restFeatureFlagQueryAdapter;

    @Test
    void findAll_shouldReturnListOfFlags_whenServiceClientReturnsFlags() {
        // Arrange
        Flag flag1 = Flag.builder().type(Flag.Type.BOOLEAN).build();
        Flag flag2 = Flag.builder().type(Flag.Type.STRING).build();
        List<Flag> expectedFlags = Arrays.asList(flag1, flag2);
        when(serviceClient.findAll()).thenReturn(expectedFlags);

        // Act
        List<Flag> actualFlags = restFeatureFlagQueryAdapter.findAll();

        // Assert
        assertEquals(expectedFlags.size(), actualFlags.size());
        assertTrue(actualFlags.containsAll(expectedFlags));
        verify(serviceClient, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenServiceClientReturnsNoFlags() {
        // Arrange
        when(serviceClient.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Flag> actualFlags = restFeatureFlagQueryAdapter.findAll();

        // Assert
        assertTrue(actualFlags.isEmpty());
        verify(serviceClient, times(1)).findAll();
    }

    @Test
    void findFlagsByType_shouldReturnListOfFlagsOfType_whenServiceClientReturnsMatchingFlags() {
        // Arrange
        Flag flag1 = Flag.builder().type(Flag.Type.STRING).build();
        Flag flag2 = Flag.builder().type(Flag.Type.STRING).build();

        List<Flag> expectedFlags = Arrays.asList(flag1, flag2);
        Flag.Type flagType = Flag.Type.STRING;

        when(serviceClient.findFlagsByType(flagType)).thenReturn(expectedFlags);

        // Act
        List<Flag> actualFlags = restFeatureFlagQueryAdapter.findFlagsByType(flagType);

        // Assert
        assertEquals(expectedFlags.size(), actualFlags.size());
        assertTrue(actualFlags.containsAll(expectedFlags));
        actualFlags.forEach(flag -> assertEquals(flagType, flag.getType()));
        verify(serviceClient, times(1)).findFlagsByType(flagType);
    }

    @Test
    void findFlagsByType_shouldReturnEmptyList_whenServiceClientReturnsNoMatchingFlags() {
        // Arrange
        Flag.Type flagType = Flag.Type.BOOLEAN;
        when(serviceClient.findFlagsByType(flagType)).thenReturn(Collections.emptyList());

        // Act
        List<Flag> actualFlags = restFeatureFlagQueryAdapter.findFlagsByType(flagType);

        // Assert
        assertTrue(actualFlags.isEmpty());
        verify(serviceClient, times(1)).findFlagsByType(flagType);
    }

    @Test
    void findByFlagName_shouldReturnOptionalWithFlag_whenServiceClientReturnsFlag() {
        // Arrange
        String flagName = "testFlag";
        Flag expectedFlag = Flag.builder().name(flagName).build();
        when(serviceClient.findByFlagName(flagName)).thenReturn(expectedFlag);

        // Act
        Optional<Flag> actualFlagOptional = restFeatureFlagQueryAdapter.findByFlagName(flagName);

        // Assert
        assertTrue(actualFlagOptional.isPresent());
        assertEquals(expectedFlag, actualFlagOptional.get());
        verify(serviceClient, times(1)).findByFlagName(flagName);
    }

    @Test
    void findByFlagName_shouldReturnEmptyOptional_whenServiceClientReturnsNull() {
        // Arrange
        String flagName = "testFlag";
        when(serviceClient.findByFlagName(flagName)).thenReturn(null);

        // Act
        Optional<Flag> actualFlagOptional = restFeatureFlagQueryAdapter.findByFlagName(flagName);

        // Assert
        assertTrue(actualFlagOptional.isEmpty());
        verify(serviceClient, times(1)).findByFlagName(flagName);
    }

    @Test
    void findFlagByNameFallback_shouldReturnEmptyOptionalAndLogError() {
        // Arrange
        String flagName = "testFlag";
        Throwable exception = new RuntimeException("Service unavailable");

        // Act
        Optional<Flag> actualFlagOptional = restFeatureFlagQueryAdapter.findFlagByNameFallback(flagName, exception);

        // Assert
        assertTrue(actualFlagOptional.isEmpty());
    }
}