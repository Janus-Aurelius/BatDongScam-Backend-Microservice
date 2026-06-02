SET search_path TO property_catalog, transaction_workflow, public;

-- ====================================================================
-- 1. MODULE ĐỊA LÝ (LOCATION MODULE)
-- ====================================================================
INSERT INTO cities (city_id, city_name)
VALUES ('10000000-0000-0000-0000-000000000001', 'TP. Hồ Chí Minh')
    ON CONFLICT (city_id) DO NOTHING;

INSERT INTO districts (district_id, city_id, district_name)
VALUES ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Quận Bình Thạnh')
    ON CONFLICT (district_id) DO NOTHING;

INSERT INTO wards (ward_id, district_id, ward_name)
VALUES ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'Phường 22')
    ON CONFLICT (ward_id) DO NOTHING;


-- ====================================================================
-- 2. MODULE DANH MỤC TÀI SẢN (PROPERTY TYPE)
-- ====================================================================
INSERT INTO property_types (property_type_id, type_name, description, is_active, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Căn hộ Bcons', 'Căn hộ, chung cư cao cấp', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000002', 'Nhà phố', 'Nhà riêng nguyên căn mặt tiền', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (property_type_id) DO NOTHING;


-- ====================================================================
-- 3. MODULE BẤT ĐỘNG SẢN (PROPERTY MODULE)
-- ====================================================================
-- Tài sản 1: Dùng để Thuê (RENTAL)
INSERT INTO properties (
    property_id, owner_id, assigned_agent_id, ward_id, property_type_id,
    title, description, transaction_type, full_address, area, rooms, bathrooms, floors, bedrooms,
    house_orientation, balcony_orientation, price_amount, commission_rate, service_fee_amount, service_fee_collected_amount, status, view_count, created_at, updated_at
) VALUES (
             '50000000-0000-0000-0000-000000000001', -- property_id
             '11111111-1111-1111-1111-111111111111', -- owner_id (Chủ nhà ảo)
             '22222222-2222-2222-2222-222222222222', -- assigned_agent_id (Môi giới ảo)
             '30000000-0000-0000-0000-000000000003', -- ward_id (Liên kết mục 1)
             '40000000-0000-0000-0000-000000000001', -- property_type_id (Liên kết mục 2)
             'Căn hộ Vinhomes Central Park Landmark 81',
             'Căn hộ tầng cao view sông thoáng mát đầy đủ tiện nghi',
             'RENTAL', 'Khu đô thị Vinhomes, Bình Thạnh, TP.HCM', 75.5, 2, 2, 1, 2,
             'SOUTH', 'SOUTH_EAST', 15000000.00, 0.0500, 500000.00, 0.00, 'AVAILABLE', 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         ) ON CONFLICT (property_id) DO NOTHING;

-- Tài sản 2: Dùng để Bán (SALE)
INSERT INTO properties (
    property_id, owner_id, assigned_agent_id, ward_id, property_type_id,
    title, description, transaction_type, full_address, area, rooms, bathrooms, floors, bedrooms,
    house_orientation, balcony_orientation, price_amount, commission_rate, service_fee_amount, service_fee_collected_amount, status, view_count, created_at, updated_at
) VALUES (
             '50000000-0000-0000-0000-000000000002',
             '11111111-1111-1111-1111-111111111111',
             '22222222-2222-2222-2222-222222222222',
             '30000000-0000-0000-0000-000000000003',
             '40000000-0000-0000-0000-000000000002',
             'Nhà phố mặt tiền đường Nguyễn Hữu Cảnh',
             'Diện tích lớn kinh doanh thuận lợi, sổ hồng chính chủ',
             'SALE', '123 Nguyễn Hữu Cảnh, Bình Thạnh, TP.HCM', 120.0, 5, 4, 3, 4,
             'EAST', 'NORTH_EAST', 8500000000.00, 0.0300, 2000000.00, 2000000.00, 'AVAILABLE', 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         ) ON CONFLICT (property_id) DO NOTHING;


-- ====================================================================
-- 4. MODULE HỢP ĐỒNG (CONTRACT MODULES - JOINED INHERITANCE)
-- ====================================================================
-- Tạo Hợp đồng gốc (Bảng cha: contract)
INSERT INTO contract (contract_id, property_id, customer_id, agent_id, contract_type, status, contract_number, start_date, end_date, created_at, updated_at)
VALUES
    ('60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'RENTAL', 'ACTIVE', 'HD-RENT-2026-001', '2026-06-01', '2027-06-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('60000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'DEPOSIT', 'COMPLETED', 'HD-DEP-2026-002', '2026-05-27', '2026-06-27', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (contract_id) DO NOTHING;

-- Chi tiết Hợp đồng thuê (Bảng con: rental_contract)
INSERT INTO rental_contract (contract_id, month_count, monthly_rent_amount, commission_amount, security_deposit_amount, security_deposit_status, late_payment_penalty_rate, accumulated_unpaid_penalty, unpaid_months_count)
VALUES ('60000000-0000-0000-0000-000000000001', 12, 15000000.00, 750000.00, 30000000.00, 'HELD', 0.0500, 0.00, 0)
    ON CONFLICT (contract_id) DO NOTHING;

-- Chi tiết Hợp đồng đặt cọc (Bảng con: deposit_contract)
INSERT INTO deposit_contract (contract_id, main_contract_type, deposit_amount, agreed_price)
VALUES ('60000000-0000-0000-0000-000000000002', 'RENTAL', 5000000.00, 15000000.00)
    ON CONFLICT (contract_id) DO NOTHING;


-- ====================================================================
-- 5. MODULE THANH TOÁN & KÝ QUỸ (PAYMENTS & ESCROW)
-- ====================================================================
-- Thêm lịch sử Thanh toán (payments)
INSERT INTO payments (payment_id, contract_id, property_id, payer_user_id, payment_type, amount, due_date, paid_time, installment_number, payment_method, status, created_at, updated_at)
VALUES ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'SECURITY_DEPOSIT', 30000000.00, '2026-05-27', CURRENT_TIMESTAMP, 1, 'BANK_TRANSFER', 'SUCCESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (payment_id) DO NOTHING;

-- Thêm quỹ đóng băng (escrow_hold)
INSERT INTO escrow_hold (escrow_id, contract_id, contract_type, payment_id, property_id, customer_id, hold_amount, released_amount, deducted_amount, status, description, version, created_at, updated_at)
VALUES ('80000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'RENTAL', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 30000000.00, 0.00, 0.00, 'HELD', 'Tiền đặt cọc giữ an toàn đảm bảo hợp đồng thuê', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (escrow_id) DO NOTHING;


-- ====================================================================
-- 6. MODULE ĐÁNH GIÁ (REVIEW MODULE)
-- ====================================================================
INSERT INTO agent_review (review_id, agent_id, customer_id, contract_id, contract_type, rating, comment, created_at, updated_at)
VALUES ('90000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '60000000-0000-0000-0000-000000000001', 'RENTAL', 5, 'Môi giới làm việc rất có tâm, hỗ trợ giấy tờ nhiệt tình!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (review_id) DO NOTHING;


-- ====================================================================
-- 7. MODULE GIẤY TỜ XÁC MINH (VERIFICATION DOCUMENTS MODULE)
-- ====================================================================
INSERT INTO document_types (document_type_id, name, description, is_compulsory, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Certificate of Land Use Rights', 'Sổ hồng / Sổ đỏ chính chủ xác minh quyền sở hữu đất và tài sản gắn liền với đất', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000002', 'General Property Documents', 'Các giấy tờ xác minh tài sản chung khác', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (document_type_id) DO NOTHING;