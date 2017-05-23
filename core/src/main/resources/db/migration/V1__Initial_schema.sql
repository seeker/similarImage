CREATE TABLE `imagerecord` (`path` VARCHAR NOT NULL , `pHash` BIGINT NOT NULL ,
PRIMARY KEY (`path`) );
CREATE TABLE `filterrecord` (`pHash` BIGINT NOT NULL , `reason` VARCHAR NOT NULL
 , PRIMARY KEY (`pHash`) );
CREATE TABLE `badfilerecord` (`path` VARCHAR NOT NULL , PRIMARY KEY (`path`) );
CREATE TABLE `ignorerecord` (`pHash` BIGINT NOT NULL , PRIMARY KEY (`pHash`) );
