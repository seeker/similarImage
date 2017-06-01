ALTER TABLE `ignorerecord` RENAME TO `ignorerecord_old`;
CREATE TABLE `ignorerecord` 
-- Table for ignored images
(`imagePath` VARCHAR NOT NULL , PRIMARY KEY (`imagePath`) );

INSERT INTO `ignorerecord` SELECT DISTINCT `path` FROM `imagerecord` JOIN `ignorerecord_old` ON `imagerecord`.`pHash` = `ignorerecord_old`.`pHash`;
DROP TABLE `ignorerecord_old`;
