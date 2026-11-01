package com.example.appointmentservice.service;

import com.example.appointmentservice.dto.ApiResponseError;
import com.example.appointmentservice.dto.AppointmentRequest;
import com.example.appointmentservice.entity.Appointment;
import com.example.appointmentservice.repository.AppointmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final RestTemplate restTemplate;
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(RestTemplate restTemplate, AppointmentRepository appointmentRepository) {
        this.restTemplate = restTemplate;
        this.appointmentRepository = appointmentRepository;
    }

    @RateLimiter(name = "appointmentFullResilience", fallbackMethod = "rateLimiterFallback")
    @CircuitBreaker(name = "appointmentFullResilience", fallbackMethod = "generalFallback")
    @Retry(name = "appointmentFullResilience", fallbackMethod = "generalFallback")
    public ResponseEntity<?> createAppointment(AppointmentRequest request) {
        log.info("Dang kiem tra lich bac si qua Doctor-Service (ID: {})...", request.getDoctorId());

        String doctorServiceUrl = "http://localhost:8082/api/doctors/" + request.getDoctorId() + "/schedule";
        String doctorResponse = restTemplate.getForObject(doctorServiceUrl, String.class);

        // Goi thanh cong -> Luu vao PostgreSQL voi trang thai CONFIRMED
        Appointment appointment = new Appointment(
                request.getPatientName(),
                request.getDoctorId(),
                request.getReason(),
                "CONFIRMED"
        );
        appointmentRepository.save(appointment);

        return ResponseEntity.ok("Dat lich thanh cong: " + doctorResponse + " (Appointment ID: " + appointment.getId() + ")");
    }

    // Fallback rieng cho RateLimiter khi client gui qua so luong request cho phep (HTTP 429)
    public ResponseEntity<?> rateLimiterFallback(AppointmentRequest request, RequestNotPermitted ex) {
        log.warn("RateLimiter chan request tu benh nhan: {}", request.getPatientName());
        ApiResponseError error = new ApiResponseError(
                "Ban da gui qua nhieu yeu cau trong thoi gian ngan. Vui long thu lai sau!",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests"
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    // Fallback tong hop: Xu ly khi CircuitBreaker mo hoac sau 3 lan Retry that bai (HTTP 503)
    public ResponseEntity<?> generalFallback(AppointmentRequest request, Throwable ex) {
        log.error("Khong the ket noi toi Doctor-Service. Luu lich hen voi trang thai PENDING. Nguyen nhan: {}", ex.getMessage());

        // Van luu don vao PostgreSQL o trang thai PENDING theo yeu cau de bai
        Appointment pendingAppointment = new Appointment(
                request.getPatientName(),
                request.getDoctorId(),
                request.getReason(),
                "PENDING"
        );
        appointmentRepository.save(pendingAppointment);

        ApiResponseError error = new ApiResponseError(
                "He thong dang gap su co ket noi den Doctor-Service. Lich hen cua ban da duoc ghi nhan o trang thai PENDING (Ma don: " + pendingAppointment.getId() + ")!",
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}