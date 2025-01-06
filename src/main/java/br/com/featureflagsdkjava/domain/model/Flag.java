package br.com.featureflagsdkjava.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a feature flag.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Flag {
   private Long id;
   private String flagName;
   private State state;
   private FlagType flagType;
   private String defaultVariant;
   private Map<String, String> variants;
   private String targeting;

   /**
    * Enum representing the types of feature flags.
    */
   public enum FlagType {
      BOOLEAN, STRING, NUMBER, OBJECT
   }

   /**
    * Enum representing the state of a feature flag.
    */
   public enum State {
      ENABLED, DISABLED
   }
}