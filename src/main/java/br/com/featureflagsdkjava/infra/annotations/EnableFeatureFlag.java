package br.com.featureflagsdkjava.infra.annotations;

import br.com.featureflagsdkjava.domain.application.openfeature.OpenFeatureConfig;
import br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider.OpenFeatureCustomProvider;
import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.infra.adapters.http.HttpInterfaceConfig;
import br.com.featureflagsdkjava.infra.adapters.http.RestFeatureFlagQueryAdapter;
import br.com.featureflagsdkjava.infra.config.JsonLogicConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan("br.com.featureflagsdkjava")
@Import({
        Flag.class,
        OpenFeatureConfig.class,
        OpenFeatureCustomProvider.class,
        JsonLogicConfig.class,
        HttpInterfaceConfig.class,
        RestFeatureFlagQueryAdapter.class
})
public @interface EnableFeatureFlag {}
