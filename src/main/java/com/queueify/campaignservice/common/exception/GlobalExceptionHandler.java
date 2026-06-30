package com.queueify.campaignservice.common.exception;

import com.queueify.campaignservice.authentication.exception.UserAlreadyExistsException;
import com.queueify.campaignservice.common.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException exception, HttpServletRequest request){

        List<ValidationError> errors = new ArrayList<>() ;

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                errors,
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                System.currentTimeMillis()

        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request){

        List<ValidationError> errors = new ArrayList<>() ;

        exception.getBindingResult().getAllErrors().forEach((error)->{
            String field = ((FieldError) error).getField();
            String message = ((FieldError) error).getDefaultMessage();
            errors.add(new ValidationError(field, message));
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errors,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getBindingResult().toString(),
                request.getRequestURI(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

    }


}
