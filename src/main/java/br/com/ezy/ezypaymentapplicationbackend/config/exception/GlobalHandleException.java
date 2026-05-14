package br.com.ezy.ezypaymentapplicationbackend.config.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;

@RestControllerAdvice
public class GlobalHandleException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseError> handleValidationError(MethodArgumentNotValidException exception, HttpServletRequest request){
        var status = HttpStatus.BAD_REQUEST;
        var details = exception.getBindingResult().getFieldErrors().stream().map(
                error -> error.getField() + ": " + error.getDefaultMessage()
        ).toList();

        return ResponseEntity.status(status).body(new ApiResponseError(Instant.now(), status.value(), status.getReasonPhrase(), "Validation Failed", request.getRequestURI(), details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseError> handleValidationError(Exception exception, HttpServletRequest request){
        var status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new ApiResponseError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        Collections.emptyList()));
    }



}
