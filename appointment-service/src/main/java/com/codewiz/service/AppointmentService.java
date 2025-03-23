package com.codewiz.service;

import com.codewiz.appointment.*;
import com.codewiz.doctor.DoctorDetailsRequest;
import com.codewiz.doctor.DoctorServiceGrpc;
import com.codewiz.model.Appointment;
import com.codewiz.patient.PatientDetailsRequest;
import com.codewiz.patient.PatientServiceGrpc;
import com.codewiz.repository.AppointmentRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AppointmentService extends AppointmentServiceGrpc.AppointmentServiceImplBase {

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceGrpc.DoctorServiceBlockingStub doctorServiceBlockingStub;
    private final PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub;

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorServiceGrpc.DoctorServiceBlockingStub doctorServiceBlockingStub, PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub) {
        this.appointmentRepository = appointmentRepository;
        this.doctorServiceBlockingStub = doctorServiceBlockingStub;
        this.patientServiceBlockingStub = patientServiceBlockingStub;
    }

    @Override
    public void bookAppointment(BookAppointmentRequest request, StreamObserver<BookAppointmentResponse> responseObserver) {
        try{
            var doctorResponse = doctorServiceBlockingStub.getDoctorDetails(DoctorDetailsRequest.newBuilder().setDoctorId(request.getDoctorId()).build());
            var patientResponse = patientServiceBlockingStub.getPatientDetails(PatientDetailsRequest.newBuilder().setPatientId(request.getPatientId()).build());
            var appointment = new Appointment(
                    null,
                    request.getPatientId(),
                    patientResponse.getFirstName() + " " + patientResponse.getLastName(),
                    request.getDoctorId(),
                    doctorResponse.getFirstName() + " " + doctorResponse.getLastName(),
                    doctorResponse.getLocation(),
                    LocalDate.parse(request.getAppointmentDate()),
                    LocalTime.parse(request.getAppointmentTime()),
                    request.getReason()
            );
            appointment = appointmentRepository.save(appointment);
            responseObserver.onNext(BookAppointmentResponse.newBuilder().setAppointmentId(appointment.id()).build());
        } catch (io.grpc.StatusRuntimeException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAppointmentAvailability(AppointmentAvailabilityRequest request, StreamObserver<AppointmentAvailabilityResponse> responseObserver) {
        List<LocalDateTime> hardcodedAppointments = Arrays.asList(
                LocalDateTime.of(2025, 4, 7, 9, 0),
                LocalDateTime.of(2025, 4, 8, 9, 30),
                LocalDateTime.of(2025, 4, 8, 10, 0),
                LocalDateTime.of(2025, 4, 8, 10, 30),
                LocalDateTime.of(2025, 4, 9, 11, 0),
                LocalDateTime.of(2025, 4, 11, 11, 30),
                LocalDateTime.of(2025, 4, 11, 13, 0),
                LocalDateTime.of(2025, 4, 12, 13, 30),
                LocalDateTime.of(2025, 4, 12, 14, 0),
                LocalDateTime.of(2025, 4, 13, 14, 30),
                LocalDateTime.of(2025, 5, 7, 9, 0),
                LocalDateTime.of(2025, 5, 8, 9, 30),
                LocalDateTime.of(2025, 5, 8, 10, 0),
                LocalDateTime.of(2025, 5, 8, 10, 30),
                LocalDateTime.of(2025, 5, 9, 11, 0),
                LocalDateTime.of(2025, 5, 11, 11, 30),
                LocalDateTime.of(2025, 5, 11, 13, 0),
                LocalDateTime.of(2025, 5, 12, 13, 30),
                LocalDateTime.of(2025, 5, 12, 14, 0),
                LocalDateTime.of(2025, 5, 13, 14, 30)

        );

        Random random = new Random();
        int i = 0;
        try {
            while (i < 100) {
                Collections.shuffle(hardcodedAppointments, random);
                var slots =   hardcodedAppointments.stream()
                        .limit(5)
                        .map(dateTime -> AppointmentSlot.newBuilder()
                                .setAppointmentDate(dateTime.toLocalDate().toString())
                                .setAppointmentTime(dateTime.toLocalTime().toString())
                                .setIsAvailable(true)
                                .build())
                        .collect(Collectors.toList());
                var response = AppointmentAvailabilityResponse.newBuilder()
                        .addAllResponses(slots)
                        .setAvailabilityAsOf(LocalDateTime.now().toString())
                        .build();
                responseObserver.onNext(response);
                Thread.sleep(2000);
                i++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            responseObserver.onCompleted();
        }
    }
}
