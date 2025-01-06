package br.com.featureflagsdkjava.infra.adapters.http;

import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.domain.ports.FeatureFlagQueryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("rest")
public class RestFeatureFlagQueryAdapter implements FeatureFlagQueryPort {

    private final FeatureFlagServiceClient serviceClient;

    public RestFeatureFlagQueryAdapter(FeatureFlagServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public List<Flag> findAll() {
        List<Flag> flagList = new ArrayList<>();

        Iterable<Flag> flags = serviceClient.findAll();
        flags.forEach(flagList::add);
        return flagList;
    }

    @Override
    public List<Flag> findFlagsByType(Flag.FlagType flagType) {
        return serviceClient.findFlagsByType(flagType);
    }

    @CircuitBreaker(name = "featureFlagServiceImpl", fallbackMethod = "findFlagByNameFallback")
    public Optional<Flag> findByFlagName(String flagName) {
        Flag flag = serviceClient.findByFlagName(flagName);

        if (flag == null) return Optional.empty();
        return Optional.of(flag);
    }

    public Optional<Flag> findFlagByNameFallback(String flagName, Throwable e) {
        log.error("Fallback triggered when trying to fetch flag {} due to: {}", flagName, e);
        return Optional.empty();
    }
}