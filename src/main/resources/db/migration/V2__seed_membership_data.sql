-- Demo password for all users: password123
INSERT INTO user_account (external_user_id, email, password_hash, name) VALUES
    ('user-001', 'user001@firstclub.co.in', '$2a$10$FeM.tC1ea5gJVLlFoQ4l4ee3uHTsSHL921IbPrp.eQGnzxfTM4afe', 'Demo User One'),
    ('user-002', 'user002@firstclub.co.in', '$2a$10$FeM.tC1ea5gJVLlFoQ4l4ee3uHTsSHL921IbPrp.eQGnzxfTM4afe', 'Demo User Two'),
    ('user-003', 'user003@firstclub.co.in', '$2a$10$FeM.tC1ea5gJVLlFoQ4l4ee3uHTsSHL921IbPrp.eQGnzxfTM4afe', 'Premium Cohort User');

INSERT INTO user_order_aggregate (user_id, total_orders, monthly_order_value, last_order_at) VALUES
    ((SELECT id FROM user_account WHERE external_user_id = 'user-001'), 5, 1200.00, NOW()),
    ((SELECT id FROM user_account WHERE external_user_id = 'user-002'), 15, 3500.00, NOW()),
    ((SELECT id FROM user_account WHERE external_user_id = 'user-003'), 20, 6000.00, NOW());

INSERT INTO user_cohort (user_id, cohort_code) VALUES
    ((SELECT id FROM user_account WHERE external_user_id = 'user-003'), 'PREMIUM_COHORT');
