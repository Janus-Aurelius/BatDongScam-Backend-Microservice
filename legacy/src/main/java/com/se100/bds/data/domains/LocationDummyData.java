package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.location.District;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.repositories.domains.location.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class LocationDummyData {

    private final CityRepository cityRepository;
    private Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    private List<String> locationUrl = List.of(
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916992/vietnam_huxruo.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916991/ho-chi-minh-city-at-night-22c7df816ce4493eb0e86cf54fe03309_zjmiye.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916990/pexels-huy-nguyen-748440234-19838813_lv1qpx.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916989/things-to-do-hoi-an-japanese-bridge_s9rbwz.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916989/Hoi-An-Ancient-Town-at-Night_kxmkpo.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916988/shutterstock-362736344_tu5efq.webp",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916986/Ho-Chi-Minh-City_w17xsn.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916985/Hoi-An_ivg4ow.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916984/Hoi-An-Vietnam_okhhze.webp",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916984/da-nang-things-to-do-hand-of-god-golden-bridge_zlwqzj.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916983/a4fd6b39-16b6-4230-bcf1-155a0d9a72c1_ankgji.avif",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916983/Defining-Vietnam-and-its-Major-Cities-132162474_zxz0ir.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766916983/da-nang-vietnam-que-faire-en-2-jours-1_vouffm.jpg"
    );

    private String getRandomMediaUrl() {
        int randomIndex = random.nextInt(locationUrl.size());
        return locationUrl.get(randomIndex);
    }


    public void createDummy() {
        if (any()) {
            log.info("Locations already exist, skipping dummy data creation");
            return;
        }
        createDummyLocations();
    }

    public boolean any() {
        return cityRepository.count() > 0;
    }

    private void createDummyLocations() {
        log.info("Creating Vietnam location data (2025 administrative divisions)");

        List<City> cities = new ArrayList<>();

        // ========== TP. HỒ CHÍ MINH ==========
        City hcmc = createCity("Thành phố Hồ Chí Minh", "Thành phố trực thuộc trung ương, trung tâm kinh tế lớn nhất Việt Nam",
                new BigDecimal("2095.50"), new BigDecimal("150000000"), 9000000);

        // Quận 1
        District q1 = createDistrict(hcmc, "Quận 1",
                new BigDecimal("7.73"), new BigDecimal("250000000"), 204000);
        q1.getWards().add(createWard(q1, "Phường Bến Nghé", new BigDecimal("0.89"), new BigDecimal("280000000"), 18000));
        q1.getWards().add(createWard(q1, "Phường Bến Thành", new BigDecimal("0.64"), new BigDecimal("270000000"), 15000));
        q1.getWards().add(createWard(q1, "Phường Nguyễn Thái Bình", new BigDecimal("0.52"), new BigDecimal("260000000"), 12000));
        q1.getWards().add(createWard(q1, "Phường Cô Giang", new BigDecimal("0.48"), new BigDecimal("265000000"), 11000));
        q1.getWards().add(createWard(q1, "Phường Phạm Ngũ Lão", new BigDecimal("0.55"), new BigDecimal("255000000"), 13000));
        hcmc.getDistricts().add(q1);

        // Quận 3
        District q3 = createDistrict(hcmc, "Quận 3",
                new BigDecimal("4.90"), new BigDecimal("220000000"), 188000);
        q3.getWards().add(createWard(q3, "Phường 1", new BigDecimal("0.45"), new BigDecimal("230000000"), 15000));
        q3.getWards().add(createWard(q3, "Phường 2", new BigDecimal("0.38"), new BigDecimal("225000000"), 12000));
        q3.getWards().add(createWard(q3, "Phường 3", new BigDecimal("0.42"), new BigDecimal("228000000"), 14000));
        q3.getWards().add(createWard(q3, "Phường 4", new BigDecimal("0.41"), new BigDecimal("222000000"), 13000));
        q3.getWards().add(createWard(q3, "Phường 5", new BigDecimal("0.39"), new BigDecimal("224000000"), 12500));
        hcmc.getDistricts().add(q3);

        // Quận Thủ Đức (sau sáp nhập Quận 2, 9, Thủ Đức)
        District thuDuc = createDistrict(hcmc, "Thành phố Thủ Đức",
                new BigDecimal("211.53"), new BigDecimal("180000000"), 1200000);
        thuDuc.getWards().add(createWard(thuDuc, "Phường Thảo Điền", new BigDecimal("3.2"), new BigDecimal("220000000"), 25000));
        thuDuc.getWards().add(createWard(thuDuc, "Phường An Phú", new BigDecimal("4.5"), new BigDecimal("210000000"), 35000));
        thuDuc.getWards().add(createWard(thuDuc, "Phường Bình An", new BigDecimal("2.8"), new BigDecimal("200000000"), 22000));
        thuDuc.getWards().add(createWard(thuDuc, "Phường Bình Trưng Đông", new BigDecimal("5.2"), new BigDecimal("185000000"), 42000));
        thuDuc.getWards().add(createWard(thuDuc, "Phường Hiệp Bình Chánh", new BigDecimal("6.8"), new BigDecimal("175000000"), 55000));
        thuDuc.getWards().add(createWard(thuDuc, "Phường Linh Đông", new BigDecimal("4.3"), new BigDecimal("165000000"), 38000));
        hcmc.getDistricts().add(thuDuc);

        // Quận 7
        District q7 = createDistrict(hcmc, "Quận 7",
                new BigDecimal("35.76"), new BigDecimal("160000000"), 350000);
        q7.getWards().add(createWard(q7, "Phường Tân Phong", new BigDecimal("2.1"), new BigDecimal("170000000"), 28000));
        q7.getWards().add(createWard(q7, "Phường Tân Phú", new BigDecimal("3.8"), new BigDecimal("165000000"), 42000));
        q7.getWards().add(createWard(q7, "Phường Phú Mỹ", new BigDecimal("4.2"), new BigDecimal("175000000"), 45000));
        q7.getWards().add(createWard(q7, "Phường Tân Hưng", new BigDecimal("2.5"), new BigDecimal("168000000"), 32000));
        hcmc.getDistricts().add(q7);

        // Quận Bình Thạnh
        District binhThanh = createDistrict(hcmc, "Quận Bình Thạnh",
                new BigDecimal("20.76"), new BigDecimal("140000000"), 450000);
        binhThanh.getWards().add(createWard(binhThanh, "Phường 1", new BigDecimal("0.75"), new BigDecimal("150000000"), 18000));
        binhThanh.getWards().add(createWard(binhThanh, "Phường 2", new BigDecimal("0.68"), new BigDecimal("148000000"), 16000));
        binhThanh.getWards().add(createWard(binhThanh, "Phường 3", new BigDecimal("0.82"), new BigDecimal("152000000"), 20000));
        binhThanh.getWards().add(createWard(binhThanh, "Phường 5", new BigDecimal("0.71"), new BigDecimal("145000000"), 17000));
        binhThanh.getWards().add(createWard(binhThanh, "Phường 6", new BigDecimal("0.79"), new BigDecimal("147000000"), 19000));
        hcmc.getDistricts().add(binhThanh);

        // Quận Tân Bình
        District tanBinh = createDistrict(hcmc, "Quận Tân Bình",
                new BigDecimal("22.38"), new BigDecimal("135000000"), 450000);
        tanBinh.getWards().add(createWard(tanBinh, "Phường 1", new BigDecimal("0.65"), new BigDecimal("142000000"), 16000));
        tanBinh.getWards().add(createWard(tanBinh, "Phường 2", new BigDecimal("0.58"), new BigDecimal("140000000"), 15000));
        tanBinh.getWards().add(createWard(tanBinh, "Phường 3", new BigDecimal("0.72"), new BigDecimal("145000000"), 18000));
        tanBinh.getWards().add(createWard(tanBinh, "Phường 4", new BigDecimal("0.68"), new BigDecimal("138000000"), 17000));
        hcmc.getDistricts().add(tanBinh);

        cities.add(hcmc);

        // ========== HÀ NỘI ==========
        City hanoi = createCity("Thành phố Hà Nội", "Thủ đô của nước Cộng hòa Xã hội Chủ nghĩa Việt Nam",
                new BigDecimal("3359.82"), new BigDecimal("120000000"), 8500000);

        // Quận Ba Đình
        District baDinh = createDistrict(hanoi, "Quận Ba Đình",
                new BigDecimal("9.21"), new BigDecimal("180000000"), 221000);
        baDinh.getWards().add(createWard(baDinh, "Phường Điện Biên", new BigDecimal("0.42"), new BigDecimal("190000000"), 12000));
        baDinh.getWards().add(createWard(baDinh, "Phường Đội Cấn", new BigDecimal("0.38"), new BigDecimal("185000000"), 11000));
        baDinh.getWards().add(createWard(baDinh, "Phường Giảng Võ", new BigDecimal("0.45"), new BigDecimal("188000000"), 13000));
        baDinh.getWards().add(createWard(baDinh, "Phường Kim Mã", new BigDecimal("0.48"), new BigDecimal("192000000"), 14000));
        hanoi.getDistricts().add(baDinh);

        // Quận Hoàn Kiếm
        District hoanKiem = createDistrict(hanoi, "Quận Hoàn Kiếm",
                new BigDecimal("5.29"), new BigDecimal("250000000"), 143000);
        hoanKiem.getWards().add(createWard(hoanKiem, "Phường Hàng Bạc", new BigDecimal("0.25"), new BigDecimal("280000000"), 8000));
        hoanKiem.getWards().add(createWard(hoanKiem, "Phường Hàng Gai", new BigDecimal("0.22"), new BigDecimal("275000000"), 7000));
        hoanKiem.getWards().add(createWard(hoanKiem, "Phường Hàng Trống", new BigDecimal("0.28"), new BigDecimal("270000000"), 9000));
        hoanKiem.getWards().add(createWard(hoanKiem, "Phường Cửa Đông", new BigDecimal("0.24"), new BigDecimal("265000000"), 7500));
        hanoi.getDistricts().add(hoanKiem);

        // Quận Cầu Giấy
        District cauGiay = createDistrict(hanoi, "Quận Cầu Giấy",
                new BigDecimal("12.04"), new BigDecimal("160000000"), 235000);
        cauGiay.getWards().add(createWard(cauGiay, "Phường Dịch Vọng", new BigDecimal("0.95"), new BigDecimal("170000000"), 18000));
        cauGiay.getWards().add(createWard(cauGiay, "Phường Dịch Vọng Hậu", new BigDecimal("1.02"), new BigDecimal("172000000"), 19000));
        cauGiay.getWards().add(createWard(cauGiay, "Phường Nghĩa Đô", new BigDecimal("0.88"), new BigDecimal("165000000"), 16000));
        cauGiay.getWards().add(createWard(cauGiay, "Phường Yên Hòa", new BigDecimal("1.15"), new BigDecimal("168000000"), 21000));
        hanoi.getDistricts().add(cauGiay);

        // Quận Đống Đa
        District dongDa = createDistrict(hanoi, "Quận Đống Đa",
                new BigDecimal("9.96"), new BigDecimal("155000000"), 366000);
        dongDa.getWards().add(createWard(dongDa, "Phường Cát Linh", new BigDecimal("0.52"), new BigDecimal("162000000"), 15000));
        dongDa.getWards().add(createWard(dongDa, "Phường Hàng Bột", new BigDecimal("0.48"), new BigDecimal("160000000"), 14000));
        dongDa.getWards().add(createWard(dongDa, "Phường Khâm Thiên", new BigDecimal("0.55"), new BigDecimal("165000000"), 16000));
        dongDa.getWards().add(createWard(dongDa, "Phường Láng Hạ", new BigDecimal("0.58"), new BigDecimal("168000000"), 17000));
        hanoi.getDistricts().add(dongDa);

        // Quận Hai Bà Trưng
        District haiBaTrung = createDistrict(hanoi, "Quận Hai Bà Trưng",
                new BigDecimal("10.09"), new BigDecimal("150000000"), 322000);
        haiBaTrung.getWards().add(createWard(haiBaTrung, "Phường Bạch Đằng", new BigDecimal("0.62"), new BigDecimal("158000000"), 16000));
        haiBaTrung.getWards().add(createWard(haiBaTrung, "Phường Bách Khoa", new BigDecimal("0.58"), new BigDecimal("155000000"), 15000));
        haiBaTrung.getWards().add(createWard(haiBaTrung, "Phường Đồng Nhân", new BigDecimal("0.65"), new BigDecimal("160000000"), 17000));
        haiBaTrung.getWards().add(createWard(haiBaTrung, "Phường Lê Đại Hành", new BigDecimal("0.61"), new BigDecimal("157000000"), 16500));
        hanoi.getDistricts().add(haiBaTrung);

        cities.add(hanoi);

        // ========== ĐÀ NẴNG ==========
        City daNang = createCity("Thành phố Đà Nẵng", "Thành phố trực thuộc trung ương, trung tâm kinh tế - xã hội lớn của miền Trung",
                new BigDecimal("1285.40"), new BigDecimal("80000000"), 1200000);

        // Quận Hải Châu
        District haiChau = createDistrict(daNang, "Quận Hải Châu",
                new BigDecimal("20.88"), new BigDecimal("100000000"), 190000);
        haiChau.getWards().add(createWard(haiChau, "Phường Thanh Bình", new BigDecimal("1.2"), new BigDecimal("110000000"), 12000));
        haiChau.getWards().add(createWard(haiChau, "Phường Thạch Thang", new BigDecimal("1.5"), new BigDecimal("108000000"), 15000));
        haiChau.getWards().add(createWard(haiChau, "Phường Hải Châu I", new BigDecimal("1.3"), new BigDecimal("105000000"), 13000));
        haiChau.getWards().add(createWard(haiChau, "Phường Hải Châu II", new BigDecimal("1.4"), new BigDecimal("107000000"), 14000));
        daNang.getDistricts().add(haiChau);

        // Quận Sơn Trà
        District sonTra = createDistrict(daNang, "Quận Sơn Trà",
                new BigDecimal("60.35"), new BigDecimal("95000000"), 138000);
        sonTra.getWards().add(createWard(sonTra, "Phường Thọ Quang", new BigDecimal("3.2"), new BigDecimal("120000000"), 18000));
        sonTra.getWards().add(createWard(sonTra, "Phường Mân Thái", new BigDecimal("2.8"), new BigDecimal("115000000"), 15000));
        sonTra.getWards().add(createWard(sonTra, "Phường An Hải Bắc", new BigDecimal("2.5"), new BigDecimal("118000000"), 16000));
        sonTra.getWards().add(createWard(sonTra, "Phường Phước Mỹ", new BigDecimal("3.5"), new BigDecimal("112000000"), 19000));
        daNang.getDistricts().add(sonTra);

        // Quận Ngũ Hành Sơn
        District nguHanhSon = createDistrict(daNang, "Quận Ngũ Hành Sơn",
                new BigDecimal("37.49"), new BigDecimal("88000000"), 75000);
        nguHanhSon.getWards().add(createWard(nguHanhSon, "Phường Mỹ An", new BigDecimal("4.2"), new BigDecimal("98000000"), 15000));
        nguHanhSon.getWards().add(createWard(nguHanhSon, "Phường Khuê Mỹ", new BigDecimal("3.8"), new BigDecimal("95000000"), 13000));
        nguHanhSon.getWards().add(createWard(nguHanhSon, "Phường Hòa Hải", new BigDecimal("5.1"), new BigDecimal("92000000"), 18000));
        daNang.getDistricts().add(nguHanhSon);

        cities.add(daNang);

        // ========== CẦN THƠ ==========
        City canTho = createCity("Thành phố Cần Thơ", "Thành phố trực thuộc trung ương, trung tâm vùng Đồng bằng sông Cửu Long",
                new BigDecimal("1409.30"), new BigDecimal("45000000"), 1400000);

        // Quận Ninh Kiều
        District ninhKieu = createDistrict(canTho, "Quận Ninh Kiều",
                new BigDecimal("30.12"), new BigDecimal("55000000"), 235000);
        ninhKieu.getWards().add(createWard(ninhKieu, "Phường Cái Khế", new BigDecimal("2.1"), new BigDecimal("62000000"), 18000));
        ninhKieu.getWards().add(createWard(ninhKieu, "Phường An Hòa", new BigDecimal("1.8"), new BigDecimal("60000000"), 15000));
        ninhKieu.getWards().add(createWard(ninhKieu, "Phường Tân An", new BigDecimal("2.5"), new BigDecimal("58000000"), 20000));
        ninhKieu.getWards().add(createWard(ninhKieu, "Phường An Cư", new BigDecimal("1.9"), new BigDecimal("59000000"), 16000));
        canTho.getDistricts().add(ninhKieu);

        // Quận Bình Thủy
        District binhThuy = createDistrict(canTho, "Quận Bình Thủy",
                new BigDecimal("68.00"), new BigDecimal("48000000"), 95000);
        binhThuy.getWards().add(createWard(binhThuy, "Phường Bình Thủy", new BigDecimal("5.2"), new BigDecimal("52000000"), 12000));
        binhThuy.getWards().add(createWard(binhThuy, "Phường Trà An", new BigDecimal("6.8"), new BigDecimal("50000000"), 15000));
        binhThuy.getWards().add(createWard(binhThuy, "Phường Trà Nóc", new BigDecimal("7.5"), new BigDecimal("49000000"), 18000));
        canTho.getDistricts().add(binhThuy);

        cities.add(canTho);

        // ========== HẢI PHÒNG ==========
        City haiPhong = createCity("Thành phố Hải Phòng", "Thành phố cảng lớn nhất miền Bắc Việt Nam",
                new BigDecimal("1527.40"), new BigDecimal("65000000"), 2100000);

        // Quận Hồng Bàng
        District hongBang = createDistrict(haiPhong, "Quận Hồng Bàng",
                new BigDecimal("12.37"), new BigDecimal("75000000"), 120000);
        hongBang.getWards().add(createWard(hongBang, "Phường Quán Toan", new BigDecimal("0.85"), new BigDecimal("82000000"), 10000));
        hongBang.getWards().add(createWard(hongBang, "Phường Hùng Vương", new BigDecimal("0.92"), new BigDecimal("80000000"), 11000));
        hongBang.getWards().add(createWard(hongBang, "Phường Sở Dầu", new BigDecimal("1.05"), new BigDecimal("78000000"), 12000));
        haiPhong.getDistricts().add(hongBang);

        // Quận Lê Chân
        District leChan = createDistrict(haiPhong, "Quận Lê Chân",
                new BigDecimal("12.30"), new BigDecimal("72000000"), 240000);
        leChan.getWards().add(createWard(leChan, "Phường Cát Dài", new BigDecimal("1.1"), new BigDecimal("78000000"), 15000));
        leChan.getWards().add(createWard(leChan, "Phường An Biên", new BigDecimal("0.95"), new BigDecimal("76000000"), 13000));
        leChan.getWards().add(createWard(leChan, "Phường Lam Sơn", new BigDecimal("1.02"), new BigDecimal("74000000"), 14000));
        haiPhong.getDistricts().add(leChan);

        cities.add(haiPhong);

        // Save all cities (cascade will save districts and wards)
        cityRepository.saveAll(cities);
        log.info("Saved {} cities with districts and wards to database", cities.size());
        log.info("Total: {} cities, {} districts, {} wards",
            cities.size(),
            cities.stream().mapToInt(c -> c.getDistricts().size()).sum(),
            cities.stream().flatMap(c -> c.getDistricts().stream())
                .mapToInt(d -> d.getWards().size()).sum());
    }

    private City createCity(String name, String description, BigDecimal totalArea,
                           BigDecimal avgLandPrice, Integer population) {
        LocalDateTime createdAt = timeGenerator.getRandomTime();
        LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

        City city = City.builder()
                .cityName(name)
                .description(description)
                .imgUrl(getRandomMediaUrl())
                .totalArea(totalArea)
                .avgLandPrice(avgLandPrice)
                .population(population)
                .isActive(true)
                .districts(new ArrayList<>())
                .build();

        city.setCreatedAt(createdAt);
        city.setUpdatedAt(updatedAt);
        return city;
    }

    private District createDistrict(City city, String name, BigDecimal totalArea,
                                   BigDecimal avgLandPrice, Integer population) {
        LocalDateTime createdAt = timeGenerator.getRandomTimeAfter(city.getCreatedAt().isBefore(LocalDateTime.now()) ? city.getCreatedAt() : LocalDateTime.now().minusDays(1), LocalDateTime.now());
        LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

        District district = District.builder()
                .city(city)
                .districtName(name)
                .description("District in " + city.getCityName())
                .imgUrl(getRandomMediaUrl())
                .totalArea(totalArea)
                .avgLandPrice(avgLandPrice)
                .population(population)
                .isActive(true)
                .wards(new ArrayList<>())
                .build();

        district.setCreatedAt(createdAt);
        district.setUpdatedAt(updatedAt);
        return district;
    }

    private Ward createWard(District district, String name, BigDecimal totalArea,
                           BigDecimal avgLandPrice, Integer population) {
        LocalDateTime createdAt = timeGenerator.getRandomTimeAfter(district.getCreatedAt().isBefore(LocalDateTime.now()) ? district.getCreatedAt() : LocalDateTime.now().minusDays(1), LocalDateTime.now());
        LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

        Ward ward = Ward.builder()
                .district(district)
                .wardName(name)
                .description("Ward in " + district.getDistrictName())
                .imgUrl(getRandomMediaUrl())
                .totalArea(totalArea)
                .avgLandPrice(avgLandPrice)
                .population(population)
                .isActive(true)
                .properties(new ArrayList<>())
                .build();

        ward.setCreatedAt(createdAt);
        ward.setUpdatedAt(updatedAt);
        return ward;
    }
}

