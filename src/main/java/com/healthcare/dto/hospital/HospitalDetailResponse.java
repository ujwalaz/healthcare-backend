package com.healthcare.dto.hospital;

import com.healthcare.dto.doctor.DoctorSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HospitalDetailResponse extends HospitalResponse {

    private List<DoctorSummaryResponse> doctors;
}
