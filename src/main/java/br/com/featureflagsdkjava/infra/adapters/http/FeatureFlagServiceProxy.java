package br.com.featureflagsdkjava.infra.adapters.http;

import br.com.featureflagsdkjava.domain.model.Flag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface FeatureFlagServiceProxy {
    @GetExchange("/flags")
    List<Flag> findAll();

    @GetExchange("/flags/byType")
    List<Flag> findFlagsByType(Flag.Type flagType);

    @GetExchange("/flags/{flagName}")
    Flag findByFlagName(@PathVariable("flagName") String flagName);
}