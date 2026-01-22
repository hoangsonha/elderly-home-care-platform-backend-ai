package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateElderlyProfileRequest {

    String name;

    @JsonProperty("birth_year")
    Integer birthYear;

    String gender;

    LocationRequest location;

    Double weight;

    Double height;

    @JsonProperty("medical_conditions")
    MedicalCondition medicalConditions;

    @JsonProperty("independence_level")
    List<IndependenceActivity> independenceLevel;

    @JsonProperty("care_needs")
    CareNeed careNeeds;

    List<String> hobbies;

    @JsonProperty("favorite_activities")
    List<String> favoriteActivities;

    @JsonProperty("favorite_food")
    List<String> favoriteFood;

    @JsonProperty("emergency_contacts")
    List<EmergencyContactRequest> emergencyContacts;

    @JsonProperty("health_status")
    String healthStatus;

    @JsonProperty("health_note")
    String healthNote;

    String note;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CareNeed {
        @JsonProperty("age")
        List<Integer> age; // [minAge, maxAge] ví dụ: [18, 28]

        String gender;

        Integer experience;

        @JsonProperty("rating")
        List<Integer> rating; // [minRating, maxRating] ví dụ: [3, 5]
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MedicalCondition {
        @JsonProperty("underlying_diseases")
        List<String> underlyingDiseases; // bệnh nền

        @JsonProperty("special_conditions")
        List<String> specialConditions; // tình trạng đặc biệt

        @JsonProperty("allergies")
        List<String> allergies; // dị ứng

        @JsonProperty("medications")
        List<MedicationRequest> medications; // thuốc đang sử dụng
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IndependenceActivity {
        String activity; // "ăn uống", "tắm rửa", "vệ sinh", "di chuyển", "mặc quần áo"
        String level; // "Tự lập", "Cần hỗ trợ", "Phụ thuộc"
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MedicationRequest {
        String name;
        String dosage;
        String frequency;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EmergencyContactRequest {
        String name;
        String relationship;
        String phone;
    }
}
