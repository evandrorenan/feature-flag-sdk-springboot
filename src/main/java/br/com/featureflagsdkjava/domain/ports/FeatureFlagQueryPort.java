package br.com.featureflagsdkjava.domain.ports;

import br.com.featureflagsdkjava.domain.model.Flag;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagQueryPort {
    List<Flag> findAll();

    List<Flag> findFlagsByType(Flag.FlagType flagType);

    Optional<Flag> findByFlagName(String flagName);
}
