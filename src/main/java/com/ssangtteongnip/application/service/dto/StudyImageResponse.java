package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.image.StudyImage;

public record StudyImageResponse(String url) {

    public static StudyImageResponse from(StudyImage image) {
        return new StudyImageResponse(image.publicPath());
    }
}
