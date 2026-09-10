SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `inquiry_replies`;
DELETE FROM `word_management`;
DELETE FROM `inquiries`;
DELETE FROM `news_posts`;
DELETE FROM `admin_audit_logs`;
DELETE FROM `login_refresh_tokens`;
DELETE FROM `login_refresh_sessions`;
DELETE FROM `login_exchange_codes`;
DELETE FROM `oauth_accounts`;
DELETE FROM `users`;
DELETE FROM `client_reports`;
DELETE FROM `records`;
DELETE FROM `api_keys`;
DELETE FROM `profanity_word`;
DELETE FROM `manage_account`;

ALTER TABLE `inquiry_replies` AUTO_INCREMENT = 1;
ALTER TABLE `inquiries` AUTO_INCREMENT = 1;
ALTER TABLE `news_posts` AUTO_INCREMENT = 1;
ALTER TABLE `admin_audit_logs` AUTO_INCREMENT = 1;
ALTER TABLE `client_reports` AUTO_INCREMENT = 1;
ALTER TABLE `records` AUTO_INCREMENT = 1;
ALTER TABLE `word_management` AUTO_INCREMENT = 1;
ALTER TABLE `profanity_word` AUTO_INCREMENT = 1;
ALTER TABLE `manage_account` AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
