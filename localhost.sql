-- phpMyAdmin SQL Dump
-- version 4.1.7
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Ott 07, 2017 alle 15:03
-- Versione del server: 5.6.33-log
-- PHP Version: 5.3.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Struttura della tabella `Detections`
--

CREATE TABLE IF NOT EXISTS `Detections` (
  `room` varchar(10) NOT NULL,
  `temp` float NOT NULL,
  `lum` float NOT NULL,
  `date` datetime NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Struttura della tabella `Log`
--

CREATE TABLE IF NOT EXISTS `Log` (
  `user` varchar(50) NOT NULL,
  `room` varchar(10) NOT NULL,
  `access` varchar(3) NOT NULL,
  `date` datetime NOT NULL,
  `marked` varchar(6) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Struttura della tabella `Opinions`
--

CREATE TABLE IF NOT EXISTS `Opinions` (
  `user` varchar(50) NOT NULL,
  `room` varchar(10) NOT NULL,
  `temp_vote` int(11) NOT NULL,
  `temp` float NOT NULL,
  `lum_vote` int(11) NOT NULL,
  `lum` float NOT NULL,
  `date` datetime NOT NULL,
  `dirty` varchar(1) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Struttura della tabella `Rooms`
--

CREATE TABLE IF NOT EXISTS `Rooms` (
  `id` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Struttura della tabella `Users`
--

CREATE TABLE IF NOT EXISTS `Users` (
  `uname` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `passwd` varchar(50) NOT NULL,
  UNIQUE KEY `uname` (`uname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
