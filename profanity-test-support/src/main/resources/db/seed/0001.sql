SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `client_reports`;
DELETE FROM `records`;
DELETE FROM `word_management`;
DELETE FROM `profanity_word`;
DELETE FROM `manage_account`;
DELETE FROM `clients`;

ALTER TABLE `client_reports` AUTO_INCREMENT = 1;
ALTER TABLE `records` AUTO_INCREMENT = 1;
ALTER TABLE `word_management` AUTO_INCREMENT = 1;
ALTER TABLE `profanity_word` AUTO_INCREMENT = 1;
ALTER TABLE `manage_account` AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
