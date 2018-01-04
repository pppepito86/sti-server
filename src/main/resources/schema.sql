--DROP TABLE IF EXISTS `submissions`;
--DROP TABLE IF EXISTS `problems`;
--DROP TABLE IF EXISTS `workers`;

CREATE TABLE IF NOT EXISTS `problems` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `group` varchar(50) NOT NULL,
  `number` int(11) NOT NULL,
  `points` int(11) NOT NULL,
  `file` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `submissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `city` varchar(50) NOT NULL,
  `group` varchar(50) NOT NULL,
  `directory` varchar(1000) NOT NULL,
  `problem1` varchar(1000) DEFAULT NULL,
  `problem2` varchar(1000) DEFAULT NULL,
  `problem3` varchar(1000) DEFAULT NULL,
  `points1` int(11) DEFAULT NULL,
  `points2` int(11) DEFAULT NULL,
  `points3` int(11) DEFAULT NULL,
  `points` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `workers`(
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

UPDATE submissions set problem1='waiting' where problem1='judging';
UPDATE submissions set problem2='waiting' where problem2='judging';
UPDATE submissions set problem3='waiting' where problem3='judging';
