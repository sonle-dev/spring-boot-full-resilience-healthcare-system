package com.example.appointmentservice.dto;

public class AppointmentRequest {
    private String patientName;
    private Long doctorId;
    private String reason;

    public AppointmentRequest() {
    }

    public AppointmentRequest(String patientName, Long doctorId, String reason) {
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.reason = reason;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}