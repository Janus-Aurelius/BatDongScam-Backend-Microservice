package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.repositories.domains.appointment.AppointmentRepository;
import com.se100.bds.repositories.domains.contract.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewDummyData {

    private final AppointmentRepository appointmentRepository;
    private final ContractRepository contractRepository;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new  TimeGenerator();

    public void createDummy() {
        createDummyReviews();
    }

    private void createDummyReviews() {
        log.info("Creating dummy reviews");

        int appointmentReviewCount = 0;
        int contractReviewCount = 0;

        // Create reviews for completed appointments
        List<Appointment> appointments = appointmentRepository.findAll();
        if (!appointments.isEmpty()) {
            for (Appointment appointment : appointments) {
                if ("COMPLETED".equals(appointment.getStatus()) && random.nextDouble() < 0.6) { // 60% of completed appointments get reviews
                    appointment.setRating((short) (3 + random.nextInt(3))); // Rating 3-5
                    appointment.setComment(generateAppointmentReviewComment());

                    LocalDateTime reviewTime = timeGenerator.getRandomTimeAfter(appointment.getUpdatedAt().isBefore(LocalDateTime.now()) ? appointment.getUpdatedAt() : LocalDateTime.now().minusDays(1), LocalDateTime.now());
                    appointment.setUpdatedAt(reviewTime);

                    appointmentRepository.save(appointment);
                    appointmentReviewCount++;
                }
            }
        }

        // Create reviews for completed contracts
        List<Contract> contracts = contractRepository.findAll();
        if (!contracts.isEmpty()) {
            for (Contract contract : contracts) {
                if (contract.getStatus() == com.se100.bds.utils.Constants.ContractStatusEnum.COMPLETED && random.nextDouble() < 0.7) { // 70% of completed contracts get reviews
                    contract.setRating((short) (3 + random.nextInt(3))); // Rating 3-5
                    contract.setComment(generateContractReviewComment());

                    LocalDateTime reviewTime = timeGenerator.getRandomTimeAfter(contract.getUpdatedAt().isBefore(LocalDateTime.now()) ? contract.getUpdatedAt() : LocalDateTime.now().minusDays(1), LocalDateTime.now());
                    contract.setUpdatedAt(reviewTime);

                    contractRepository.save(contract);
                    contractReviewCount++;
                }
            }
        }

        log.info("Added {} appointment reviews and {} contract reviews to database", appointmentReviewCount, contractReviewCount);
    }

    private String generateAppointmentReviewComment() {
        String[] comments = {
                "Nhân viên rất chuyên nghiệp, hiểu biết rõ về bất động sản",
                "Buổi xem nhà được tổ chức tốt và đúng giờ",
                "Nhân viên hỗ trợ nhiệt tình và trả lời mọi thắc mắc",
                "Trải nghiệm tốt, sẽ giới thiệu cho người khác",
                "Bất động sản đúng như mô tả ban đầu",
                "Nhân viên lịch sự và chuyên nghiệp trong suốt quá trình",
                "Dịch vụ xuất sắc, rất hài lòng với buổi xem nhà"
        };
        return comments[random.nextInt(comments.length)];
    }

    private String generateContractReviewComment() {
        String[] comments = {
                "Quy trình giao dịch diễn ra suôn sẻ từ đầu đến cuối",
                "Rất hài lòng với toàn bộ quá trình làm hợp đồng",
                "Nhân viên xử lý mọi việc rất chuyên nghiệp",
                "Trải nghiệm tuyệt vời, rất đáng để giới thiệu",
                "Tất cả giấy tờ được xử lý nhanh chóng và hiệu quả",
                "Toàn bộ quy trình minh bạch và công bằng",
                "Dịch vụ xuất sắc trong suốt thời gian hợp đồng"
        };
        return comments[random.nextInt(comments.length)];
    }
}
