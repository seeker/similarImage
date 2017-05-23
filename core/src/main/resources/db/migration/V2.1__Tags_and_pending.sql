CREATE TABLE `tag`
-- User created tags
(`userTagId` INTEGER PRIMARY KEY AUTOINCREMENT , `name` VARCHAR NOT NULL , `contextMenu` BOOLEAN NOT NULL ,  UNIQUE (`name`));
CREATE INDEX `tag_name_idx` ON `tag` ( `name` );

CREATE TABLE `thumbnail`
-- Thumbnails used in filter records
(`id` INTEGER PRIMARY KEY AUTOINCREMENT , `uniqueHash` BLOB NOT NULL , `imageData` BLOB NOT NULL ,  UNIQUE (`uniqueHash`));
CREATE INDEX `thumbnail_uniqueHash_idx` ON `thumbnail` ( `uniqueHash` );

CREATE TABLE `pendinghashimage`
-- Images who are waiting to be hashed
(`id` INTEGER PRIMARY KEY AUTOINCREMENT , `path` VARCHAR , `most` BIGINT , `least` BIGINT ,  UNIQUE (`path`), UNIQUE (`most`,`least`) );
CREATE INDEX `pendinghashimage_most_idx` ON `pendinghashimage` ( `most` );
CREATE INDEX `pendinghashimage_least_idx` ON `pendinghashimage` ( `least` );
CREATE INDEX `pendinghashimage_path_idx` ON `pendinghashimage` ( `path` );

-- Rename the old table so we can recreate it and copy the data
ALTER TABLE `filterrecord` RENAME TO `filterrecord_old`;

CREATE TABLE `filterrecord`
-- Hashes tagged by the user
(`id` INTEGER PRIMARY KEY AUTOINCREMENT , `pHash` BIGINT NOT NULL , `tag_id` BIGINT NOT NULL, `thumbnail_id` INTEGER , UNIQUE (`pHash`,`tag_id`) );
CREATE INDEX `filterrecord_pHash_idx` ON `filterrecord` ( `pHash` );
CREATE INDEX `filterrecord_tag_idx` ON `filterrecord` ( `tag_id` );

-- Insert tags from old table, contextMenu = 0 -> false
INSERT INTO `tag` (`name`, `contextMenu`) SELECT DISTINCT `reason`, 0 FROM `filterrecord_old`;

-- Copy old filter rows into new table
INSERT INTO `filterrecord` (`pHash`, `tag_id`, `thumbnail_id`) SELECT DISTINCT `filterrecord_old`.pHash, tag.userTagId, NULL FROM `filterrecord_old` JOIN `tag` on `filterrecord_old`.reason = tag.name;

-- Remove the old filter table
DROP TABLE `filterrecord_old`;
