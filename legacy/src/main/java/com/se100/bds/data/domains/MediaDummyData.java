package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.repositories.domains.property.MediaRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class MediaDummyData {

    private final MediaRepository mediaRepository;
    private final PropertyRepository propertyRepository;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    private final List<String> mediaUrl = List.of(
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916112/thiet-ke-nha-2-tang-hien-dai-mat-tien-8m_nwrk53.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916112/tim-hieu-ve-cac-loai-nha-o-viet-nam_ncnvbx.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/thiet-ke-nha-2-tang-7_bvlaoj.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/thiet-ke-khach-san-kieu-phap-4-sao-17-tang-vietpear-hotel-nguy-nga-trang-le-tai-vung-tau-1_xeocok.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/sanh-khach-san-vinpearl-2_bnobcx.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/SYOfzNGeyBcBbHYZXmXiabZgsataur_pisg8g.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/nha-pho-905_hwlok3.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916111/nhung-trai-nghiem-chi-co-o-khach-san-5-sao-1_qh9bkc.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916110/nha-cap-4-1345_qatysq.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916110/4_bzcyyr.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916109/nha-mai-nhat-1-tang-WC094-01_y3ku1s.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916109/mau-nha-4-tang-mat-tien-5m-36_acju4q.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916109/anh-tu-nha-1-tang_1663902340.jpg_msefbf.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916108/nha-mai-nhat-3-tang_lluyob.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916108/1_vm4ua1.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916108/anh-nha-2_vny29v.jpg"
    );

    public void createDummy() {
        createDummyMedia();
    }

    /**
     * Get a random media URL from the predefined list
     * @return random URL string
     */
    private String getRandomMediaUrl() {
        int randomIndex = random.nextInt(mediaUrl.size());
        return mediaUrl.get(randomIndex);
    }

    private void createDummyMedia() {
        log.info("Creating dummy media files");

        List<Property> properties = propertyRepository.findAll();
        if (properties.isEmpty()) {
            log.warn("Cannot create media - no properties found");
            return;
        }

        List<Media> mediaList = new ArrayList<>();

        // Create 3-5 media files for each property
        for (Property property : properties) {
            int mediaCount = 3 + random.nextInt(3); // 3-5 media files

            for (int i = 0; i < mediaCount; i++) {
                Constants.MediaTypeEnum mediaType;
                String mimeType;

                if (i < mediaCount - 1) {
                    // First few are images
                    mediaType = Constants.MediaTypeEnum.IMAGE;
                    mimeType = "image/jpeg";
                } else {
                    // Last one is either video or document
                    mediaType = random.nextBoolean() ? Constants.MediaTypeEnum.VIDEO : Constants.MediaTypeEnum.DOCUMENT;
                    mimeType = mediaType == Constants.MediaTypeEnum.VIDEO ? "video/mp4" : "application/pdf";
                }

                LocalDateTime createdAt = timeGenerator.getRandomTimeAfter(property.getCreatedAt(), null);
                LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, null);

                Media media = Media.builder()
                        .property(property)
                        .mediaType(mediaType)
                        .fileName(String.format("property_%s_%s_%d.%s",
                                property.getId(),
                                mediaType.name().toLowerCase(),
                                i + 1,
                                getFileExtension(mimeType)))
                        .filePath(getRandomMediaUrl())
                        .mimeType(mimeType)
                        .documentType(mediaType == Constants.MediaTypeEnum.DOCUMENT ? "Property Certificate" : null)
                        .build();

                media.setCreatedAt(createdAt);
                media.setUpdatedAt(updatedAt);
                mediaList.add(media);
            }
        }

        mediaRepository.saveAll(mediaList);
        log.info("Saved {} media files to database", mediaList.size());
    }

    private String getFileExtension(String mimeType) {
        switch (mimeType) {
            case "image/jpeg": return "jpg";
            case "video/mp4": return "mp4";
            case "application/pdf": return "pdf";
            default: return "bin";
        }
    }
}

