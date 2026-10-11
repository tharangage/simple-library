package com.ascendion.roshan.simple_library.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(Enable.class);

    @EnableConfigurationProperties(LibraryProperties.class)
    static class Enable {
    }

    @Test
    void validate_zeroOrNegative_isRejected() {
        assertThat(validator.validate(new LibraryProperties(0))).hasSize(1);
        assertThat(validator.validate(new LibraryProperties(-2))).hasSize(1);
    }

    @Test
    void validate_one_isAccepted() {
        assertThat(validator.validate(new LibraryProperties(1))).isEmpty();
    }

    @Test
    void binding_noProperty_defaultsToThree() {
        runner.run(context -> assertThat(context.getBean(LibraryProperties.class).maxItemsPerBorrower()).isEqualTo(3));
    }

    @Test
    void binding_zero_makesTheApplicationFailToStart() {
        runner.withPropertyValues("app.library.max-items-per-borrower=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
