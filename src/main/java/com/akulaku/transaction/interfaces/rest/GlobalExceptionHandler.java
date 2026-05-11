package com.akulaku.transaction.interfaces.rest;

import com.akulaku.transaction.domain.exception.AccountInactiveException;
import com.akulaku.transaction.domain.exception.AccountNotFoundException;
import com.akulaku.transaction.domain.exception.DomainException;
import com.akulaku.transaction.domain.exception.DuplicateExternalReferenceException;
import com.akulaku.transaction.domain.exception.IdempotencyConflictException;
import com.akulaku.transaction.domain.exception.IdempotencyInProgressException;
import com.akulaku.transaction.domain.exception.InsufficientLimitException;
import com.akulaku.transaction.interfaces.rest.dto.FieldErrorItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 Problem Details for HTTP APIs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://errors.akulaku.transaction-service/";

    @ExceptionHandler(InsufficientLimitException.class)
    public ResponseEntity<ProblemDetail> insufficientLimit(InsufficientLimitException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient-limit", "Insufficient credit limit", ex.getMessage(), req);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> accountNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "account-not-found", "Credit account not found", ex.getMessage(), req);
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ProblemDetail> accountInactive(AccountInactiveException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "account-inactive", "Credit account is inactive", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateExternalReferenceException.class)
    public ResponseEntity<ProblemDetail> duplicateExternalRef(DuplicateExternalReferenceException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "duplicate-external-ref", "Duplicate external reference", ex.getMessage(), req);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> idempotencyConflict(IdempotencyConflictException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "idempotency-conflict", "Idempotency key conflict",
            "This idempotency key was already used with a different request payload.", req);
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ProblemDetail> idempotencyInProgress(IdempotencyInProgressException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "idempotency-in-progress",
            "Request already in progress", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "bad-request", "Invalid request", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
            .toList();
        ProblemDetail pd = baseProblem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
            "One or more fields are invalid.", req);
        pd.setProperty("errors", errors);
        return problemResponse(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
            .map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
        ProblemDetail pd = baseProblem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
            "One or more constraints were violated.", req);
        pd.setProperty("errors", errors);
        return problemResponse(pd);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> missingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        String msg = "Required request header '" + ex.getHeaderName() + "' is not present";
        return respond(HttpStatus.BAD_REQUEST, "missing-header", "Missing header", msg, req);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> domainFallback(DomainException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "domain-error", "Domain error", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> fallback(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = baseProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal",
            "Internal server error",
            "An unexpected error occurred.",
            req
        );
        return problemResponse(pd);
    }

    private ResponseEntity<ProblemDetail> respond(
        HttpStatus status,
        String typeSuffix,
        String title,
        String detail,
        HttpServletRequest req
    ) {
        ProblemDetail pd = baseProblem(status, typeSuffix, title, detail, req);
        return problemResponse(pd);
    }

    private ProblemDetail baseProblem(HttpStatus status, String typeSuffix, String title, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + typeSuffix));
        pd.setTitle(title);
        if (req.getRequestURI() != null) {
            pd.setInstance(URI.create(req.getRequestURI()));
        }
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    private ResponseEntity<ProblemDetail> problemResponse(ProblemDetail pd) {
        HttpStatus status = HttpStatus.valueOf(pd.getStatus());
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
    }
}
