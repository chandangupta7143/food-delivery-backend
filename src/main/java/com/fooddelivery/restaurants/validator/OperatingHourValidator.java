package com.fooddelivery.restaurants.validator;

import com.fooddelivery.restaurants.dto.OperatingHourRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Documented
@Constraint(validatedBy = OperatingHourValidator.OperatingHourValidatorImpl.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperatingHourValidator {
    String message() default "Close time must be after open time";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class OperatingHourValidatorImpl implements ConstraintValidator<OperatingHourValidator, OperatingHourRequest> {
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        @Override
        public boolean isValid(OperatingHourRequest request, ConstraintValidatorContext context) {
            if (request == null || request.getOpenTime() == null || request.getCloseTime() == null) {
                return true; // Let @NotBlank handle nulls
            }

            try {
                LocalTime openTime = LocalTime.parse(request.getOpenTime(), TIME_FORMATTER);
                LocalTime closeTime = LocalTime.parse(request.getCloseTime(), TIME_FORMATTER);
                
                boolean isValid = closeTime.isAfter(openTime);
                if (!isValid) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Close time must be after open time")
                           .addPropertyNode("closeTime")
                           .addConstraintViolation();
                }
                return isValid;
            } catch (DateTimeParseException e) {
                return true; // Let @Pattern handle format errors
            }
        }
    }
}
