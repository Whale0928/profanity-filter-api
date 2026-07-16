SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `login_refresh_tokens`;
DELETE FROM `login_refresh_sessions`;
DELETE FROM `login_exchange_codes`;
DELETE FROM `oauth_accounts`;
DELETE FROM `users`;
DELETE FROM `client_reports`;
DELETE FROM `records`;
DELETE FROM `api_keys`;
DELETE FROM `word_management`;
DELETE FROM `profanity_word`;
DELETE FROM `manage_account`;

ALTER TABLE `client_reports` AUTO_INCREMENT = 1;
ALTER TABLE `records` AUTO_INCREMENT = 1;
ALTER TABLE `word_management` AUTO_INCREMENT = 1;
ALTER TABLE `profanity_word` AUTO_INCREMENT = 1;
ALTER TABLE `manage_account` AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
