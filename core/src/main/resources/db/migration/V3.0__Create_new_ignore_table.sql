ALTER TABLE `ignorerecord` RENAME TO `ignorerecord_old`;
CREATE TABLE `ignorerecord` 
-- Table for ignored images
(`id` INTEGER PRIMARY KEY AUTOINCREMENT , `path` VARCHAR NOT NULL );

INSERT INTO `ignorerecord` (`id`, `path`) SELECT DISTINCT NULL, `path` FROM `imagerecord` JOIN `ignorerecord_old` ON `imagerecord`.`pHash` = `ignorerecord_old`.`pHash`;
DROP TABLE `ignorerecord_old`;
