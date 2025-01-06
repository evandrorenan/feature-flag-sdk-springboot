package br.com.featureflagsdkjava.domain.openfeature.providers.customProvider;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@Slf4j
class OpenFeatureCustomProviderTest {
/*
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("When flag is disabled, should return default value")
    void whenFlagIsDisabledShouldReturnDefaultValue() {
        FlagDAO flagDAO = baseFlagDAO();
        flagDAO.setState(FlagDAO.State.DISABLED);
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", null);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals("defaultValue", result.getValue());
        assertEquals(Reason.ERROR.toString(), result.getReason());
    }

    @Test
    @DisplayName("When flag is invalid, should return default value")
    void whenFlagIsInvalidShouldReturnDefaultValue() {
        FlagDAO flagDAO = baseFlagDAO();
        flagDAO.setDefaultVariant(null);
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", null);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals("defaultValue", result.getValue());
        assertEquals(Reason.ERROR.toString(), result.getReason());

        flagDAO.setDefaultVariant("non_existent");

        result = provider.getStringEvaluation("FLAG2-1000", "defaultValue", null);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals("defaultValue", result.getValue());
        assertEquals(Reason.ERROR.toString(), result.getReason());

        flagDAO.setVariants(null);

        result = provider.getStringEvaluation("FLAG2-1000", "defaultValue", null);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals("defaultValue", result.getValue());
        assertEquals(Reason.ERROR.toString(), result.getReason());
    }

    @Test
    @DisplayName("When flag is static, shoud return default variant")
    void whenFlagIsStaticShouldReturnDefaultVariant() {
        FlagDAO flagDAO = baseFlagDAO();
        flagDAO.setTargeting(null);
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", null);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals(flagDAO.getVariants().get(flagDAO.getDefaultVariant()), result.getValue());
        assertEquals(Reason.STATIC.toString(), result.getReason());
    }

    @Test
    @DisplayName("When targeting is invalid, should return default value")
    void whenTargetingIsInvalidShouldReturnDefaultValue() throws JsonProcessingException {
        FlagDAO flagDAO = baseFlagDAO();
        flagDAO.setTargeting("{ \"if\": [ { \"in\": [ { \"var\": \"cdCpfCnpj\"}, \"1122233344\" ]}, \"true\" ]}" + "}");
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", buildContext());
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals("defaultValue", result.getValue());
        assertEquals(Reason.ERROR.toString(), result.getReason());
    }

    @Test
    @DisplayName("When has targeting but doesn't match, should return default variant")
    void whenHasTargetingButDoesntMatchShouldReturnDefaultVariant() throws JsonProcessingException {
        FlagDAO flagDAO = baseFlagDAO();
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        MutableContext context = buildContext();
        context.add("cdCpfCnpj", "1234567890");

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", context);
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals(flagDAO.getVariants().get(flagDAO.getDefaultVariant()), result.getValue());
        assertEquals(Reason.DEFAULT.toString(), result.getReason());
    }

    @Test
    @DisplayName("When targeting and match, should return defined variant")
    void whenTargetingAndMatchShouldReturnDefinedVariant() throws JsonProcessingException {
        FlagDAO flagDAO = baseFlagDAO();
        FeatureFlagRepository mockedRepo = mock(FeatureFlagRepository.class);
        DefaultFeatureFlagService defaultFeatureFlagService = new DefaultFeatureFlagService(mockedRepo, new FeatureFlagMapperImpl());

        when(mockedRepo.findByFlagName(any())).thenReturn(Optional.of(flagDAO));

        OpenFeatureCustomProvider provider =
                new OpenFeatureCustomProvider(defaultFeatureFlagService, new JsonLogic());

        ProviderEvaluation<String> result =
                provider.getStringEvaluation("FLAG2-1000", "defaultValue", buildContext());
        log.info("Result: {}, Reason: {}, variant: {}", result.getValue(), result.getReason(), result.getVariant());

        assertEquals(flagDAO.getVariants().get("true"), result.getValue());
        assertEquals(Reason.TARGETING_MATCH.toString(), result.getReason());
    }

    private MutableContext buildContext() throws JsonProcessingException {
        MutableContext context = new MutableContext();
        String body = """
                {
                  "email": "john.arnold@inge.com",
                  "cdCpfCnpj": "1122233344",
                  "sub": {
                    "nro": 123,
                    "boo": false,
                    "arr1": [ "aa", "bb" ],
                    "arr2": [ 1, 2 ],
                    "arr3": [ 1.0, 2.0 ],
                    "arr4": [ true, false ],
                    "arr5": [ null, "xx" ]
                  }
                }
                """;
        Map<String, Object> mapContext = mapper.readValue(body, new TypeReference<>() {});
        mapContext.forEach((key, value) -> {
            if (value.getClass().getSimpleName().equals("String")) {
                context.add(key, String.valueOf(value));
            }
        });
        return context;
    }

    private FlagDAO baseFlagDAO() {
        try {
            return mapper.readValue(jsonFlag(), FlagDAO.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing json flag", e);
            throw new RuntimeException(e);
        }
    }

    private static String jsonFlag() {
        return """
                {
                  "id" : 1,
                  "flagName" : "FLAG2-1000",
                  "flagType" : "STRING",
                  "state" : "ENABLED",
                  "defaultVariant" : "false",
                  "variants" : {
                    "true" : "release1",
                    "false" : ""
                  },
                  "targeting" : "{ \\"if\\": [ { \\"in\\": [ { \\"var\\": \\"cdCpfCnpj\\"}, \\"1122233344\\" ]}, \\"true\\" ]}"
                }
                """;
    }

 */
}