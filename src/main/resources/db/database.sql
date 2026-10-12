-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: donoithat
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `affiliate_commissions`
--

DROP TABLE IF EXISTS `affiliate_commissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `affiliate_commissions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `affiliate_id` int NOT NULL,
  `order_id` int NOT NULL,
  `buyer_id` int DEFAULT NULL,
  `order_amount` decimal(15,2) NOT NULL,
  `commission_rate` decimal(5,2) NOT NULL,
  `commission_amt` decimal(15,2) NOT NULL,
  `type` enum('order','referral') COLLATE utf8mb4_unicode_ci DEFAULT 'order',
  `status` enum('pending','confirmed','cancelled') COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_affiliate_id` (`affiliate_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `affiliate_commissions`
--

LOCK TABLES `affiliate_commissions` WRITE;
/*!40000 ALTER TABLE `affiliate_commissions` DISABLE KEYS */;
INSERT INTO `affiliate_commissions` VALUES (1,1,46,6,5000000.00,5.00,250000.00,'order','confirmed','Hoa hồng đơn hàng qua link affiliate','2026-03-29 18:21:15'),(2,1,48,6,5000000.00,5.00,250000.00,'order','confirmed','Hoa hồng đơn hàng qua link affiliate','2026-03-30 02:06:43'),(3,1,49,6,7000000.00,5.00,350000.00,'order','confirmed','Hoa hồng đơn hàng qua link affiliate','2026-04-20 01:47:35'),(4,1,36,6,5000000.00,5.00,250000.00,'order','confirmed','Hoa hồng đơn hàng qua link affiliate','2026-05-02 06:54:06'),(5,1,37,6,7000000.00,5.00,350000.00,'order','confirmed','Hoa hồng đơn hàng qua link affiliate','2026-05-04 01:10:15');
/*!40000 ALTER TABLE `affiliate_commissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `affiliate_withdrawals`
--

DROP TABLE IF EXISTS `affiliate_withdrawals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `affiliate_withdrawals` (
  `id` int NOT NULL AUTO_INCREMENT,
  `affiliate_id` int NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_account` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_owner` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('pending','approved','rejected') COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `admin_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_affiliate_id` (`affiliate_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `affiliate_withdrawals`
--

LOCK TABLES `affiliate_withdrawals` WRITE;
/*!40000 ALTER TABLE `affiliate_withdrawals` DISABLE KEYS */;
INSERT INTO `affiliate_withdrawals` VALUES (1,1,200000.00,'MB Bank','023004099999','DANG GIANG SON','','approved','','2026-04-20 01:54:14','2026-03-29 18:24:04'),(2,1,200000.00,'MB Bank','023004099999','DANG GIANG SON','','approved','','2026-04-20 01:57:30','2026-04-20 01:57:15'),(3,1,450000.00,'MB Bank','023004099999','DANG GIANG SON','','approved','','2026-04-20 02:08:31','2026-04-20 02:08:16'),(4,1,300000.00,'MB Bank','023004099999','DANG GIANG SON','','approved','','2026-05-04 01:13:23','2026-05-04 01:11:22');
/*!40000 ALTER TABLE `affiliate_withdrawals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `affiliates`
--

DROP TABLE IF EXISTS `affiliates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `affiliates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `ref_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `commission_rate` decimal(5,2) NOT NULL DEFAULT '5.00',
  `total_earned` decimal(15,2) DEFAULT '0.00',
  `total_withdrawn` decimal(15,2) DEFAULT '0.00',
  `balance` decimal(15,2) DEFAULT '0.00',
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_owner` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('pending','active','banned') COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `ref_code` (`ref_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_ref_code` (`ref_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `affiliates`
--

LOCK TABLES `affiliates` WRITE;
/*!40000 ALTER TABLE `affiliates` DISABLE KEYS */;
INSERT INTO `affiliates` VALUES (1,11,'SONPRO5575',5.00,1450000.00,1150000.00,300000.00,'MB Bank','023004099999','DANG GIANG SON','active','2026-03-29 14:40:53');
/*!40000 ALTER TABLE `affiliates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (4,'Học tập'),(3,'Phòng ăn'),(1,'Phòng khách'),(2,'Phòng ngủ');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contacts`
--

DROP TABLE IF EXISTS `contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contacts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `message` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) DEFAULT 'new',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contacts`
--

LOCK TABLES `contacts` WRITE;
/*!40000 ALTER TABLE `contacts` DISABLE KEYS */;
INSERT INTO `contacts` VALUES (1,'Đặng Giang Sơn','0326976832','tôi muốn được tư vấn sản phẩm bàn ăn đơn giản','2025-11-23 19:53:04','read'),(2,'Đặng Giang Sơn','0326976832','gưegeg','2025-12-16 14:36:24','read'),(3,'Đặng Giang Sơn','0326976832','vsvadv','2025-12-16 14:39:23','read'),(4,'Đặng Giang Sơn','0326976832','lvsjvnvoeidn','2025-12-17 00:39:47','read');
/*!40000 ALTER TABLE `contacts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `receiver_id` int NOT NULL DEFAULT '0',
  `message` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (1,6,4,'xin chào','2025-11-23 20:01:54'),(2,4,6,'xin chào bạn cần gì','2025-11-23 20:02:10'),(3,1,4,'XIn chào','2025-11-24 05:58:47'),(4,8,4,'xin chào','2025-11-24 06:01:09'),(5,4,8,'xin chào bạn cần gì','2025-11-24 06:01:44'),(6,8,4,'tôi muốn được tư vấn bàn làm việc cách tân','2025-11-24 06:02:02'),(7,6,4,'fvsfefef','2025-11-24 07:43:03'),(8,4,6,'dvsvsrvrs','2025-11-24 07:43:07'),(9,6,4,'wgrgwrgg44','2025-11-25 06:04:23'),(10,6,4,'fdfhrhhfdhdh','2025-11-25 06:04:40'),(11,4,6,'dhdhsrhsrhgsrs','2025-11-25 06:04:44'),(12,10,4,'xin chào','2025-11-25 07:05:37'),(13,4,10,'em chào thầy','2025-11-25 07:06:03'),(14,4,4,'xin chao','2025-12-16 14:47:32'),(15,6,4,'xin chào','2025-12-16 14:47:49'),(16,4,6,'xin chào \\','2025-12-16 14:48:54'),(17,4,4,'fewefwe','2025-12-16 14:49:06'),(18,6,4,'ưefwef','2025-12-16 14:49:31'),(19,6,4,'revrvrv','2025-12-16 14:49:41'),(20,4,6,'xin cakfo','2025-12-16 14:50:16'),(21,6,4,'123','2025-12-16 14:51:04'),(22,6,4,'123','2025-12-16 14:51:14'),(23,6,4,'123','2025-12-16 14:51:34'),(24,6,4,'1414','2025-12-16 14:55:19'),(25,4,6,'12433','2025-12-16 14:55:38'),(26,6,4,'ưvwvrwv','2025-12-16 14:58:38'),(27,4,6,'ewvwrv','2025-12-16 14:58:44'),(28,6,4,'vửvrvrv','2025-12-16 14:58:49'),(29,4,6,'vrvrvwrv','2025-12-16 14:58:52'),(30,11,4,'vưevwrv','2025-12-16 15:06:02'),(31,4,11,'fvwefewv','2025-12-16 15:06:46'),(32,11,4,'rvrvwr','2025-12-16 15:06:49'),(33,4,11,'brebebt','2025-12-16 15:06:52'),(34,11,4,'betbetb','2025-12-16 15:06:54'),(35,4,11,'bbdb','2025-12-16 15:06:59'),(36,6,4,'ggvutfvuyfvy','2025-12-27 01:28:39'),(37,10,4,'jbfajfbje vjbdvnj','2025-12-27 01:49:44'),(38,6,0,'hãy tư vấn giúp tôi sản phẩm bàn học để tôi mua cho con gái','2026-03-15 10:40:17'),(39,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 10:40:17'),(40,6,0,'tôi muốn tư vấn sản phẩm bàn học','2026-03-15 10:43:59'),(41,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 10:43:59'),(42,6,0,'xin chào','2026-03-15 10:50:59'),(43,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 10:51:00'),(44,6,0,'jhj','2026-03-15 11:02:38'),(45,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 11:02:39'),(46,6,0,'gsg','2026-03-15 11:06:04'),(47,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 11:06:04'),(48,6,0,'xin chao','2026-03-15 11:07:58'),(49,0,6,'Chào bạn! Rất vui được hỗ trợ bạn tại nội thất SLAND. \n\nĐể dùng','2026-03-15 11:08:01'),(50,6,0,'hãy tư vấn bàn học để tôi mua cho con gái','2026-03-15 11:09:26'),(51,0,6,'Chào bạn! SLAND hiện có 2 mẫu bàn phù hợp để làm bàn học','2026-03-15 11:09:29'),(52,6,0,'tư vấn mẫu 1','2026-03-15 11:10:48'),(53,0,6,'Dạ, đây là thông tin chi tiết về mẫu **Bàn làm việc cách tân** (Mẫu 1) để anh/chị tham khảo cho con gái ạ:\n\n*   **Giá:** 3.000.000đ\n*   **Chất liệu:** Gỗ MDF chống ẩm (bền đẹp, dễ vệ sinh).\n*   **Kích thước:** 1.4m x 0.6m (không gian rộng rãi cho bé để sách vở và máy tính).\n*   **Bảo hành:** 18 tháng.\n*   **Ưu đãi:** Miễn phí vận chuyển nội thành Hà Nội.\n\nNếu anh/chị ưng ý mẫu này, hãy **thêm sản phẩm vào giỏ hàng và tiến hành thanh toán** để SLAND giao hàng sớm nhất nhé!','2026-03-15 11:10:53'),(54,6,0,'xin chào','2026-03-15 11:22:18'),(55,0,6,'Chào bạn! Rất vui được chào đón bạn đến với cửa hàng nội thất SLAND. \n\nChúng tôi chuyên cung cấp các dòng sản phẩm nội thất đa dạng từ giường ngủ, bàn ăn, tủ quần áo đến bàn làm việc với chất lượng đảm bảo và chính sách miễn phí vận chuyển trong nội thành Hà Nội. Mời bạn tham khảo qua các sản phẩm nổi bật của chúng tôi để tìm được món đồ ưng ý nhất cho không gian sống của mình.\n\nKhông biết bạn đang có nhu cầu tìm mua sản phẩm nội thất cho phòng ngủ, phòng khách hay phòng làm việc ạ?','2026-03-15 11:22:22'),(56,6,0,'xin chào','2026-03-15 11:29:39'),(57,0,6,'Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!','2026-03-15 11:29:50'),(58,6,0,'xin chào','2026-03-15 11:30:32'),(59,0,6,'Dạ, xin chào anh/chị! Rất vui được đón tiếp anh/chị đến với cửa hàng nội thất SLAND.\n\nChúng em chuyên cung cấp các dòng sản phẩm nội thất chất lượng như giường ngủ, bàn ăn, tủ quần áo và bàn làm việc với chính sách miễn phí vận chuyển trong nội thành Hà Nội và bảo hành dài hạn. Anh/chị có thể ghé thăm cửa hàng tại địa chỉ Trường Đại học Tài nguyên và Môi trường Hà Nội hoặc liên hệ hotline 0326976832 để được hỗ trợ nhanh nhất.\n\nKhông biết anh/chị đang quan tâm đến sản phẩm nào hoặc cần em tư vấn nội thất cho không gian phòng ngủ, phòng khách hay phòng làm việc của gia đình mình ạ?','2026-03-15 11:30:37'),(60,6,0,'xin chào','2026-03-15 11:32:22'),(61,0,6,'Dạ, xin chào anh/chị! Rất vui được chào đón anh/chị quay trở lại với cửa hàng nội thất SLAND. \n\nHiện tại, cửa hàng chúng em đang có sẵn rất nhiều mẫu nội thất đa dạng từ giường ngủ, bàn ăn, sofa đến bàn làm việc với thiết kế từ hiện đại đến cách tân, phù hợp cho mọi không gian sống. Tất cả các sản phẩm của SLAND đều được cam kết về chất lượng, hỗ trợ bảo hành lên đến 24 tháng và miễn phí vận chuyển trong khu vực nội thành Hà Nội.\n\nAnh/chị đang quan tâm đến sản phẩm cụ thể nào hay cần em tư vấn mẫu nội thất phù hợp với diện tích phòng của mình không ạ?','2026-03-15 11:32:27'),(62,6,0,'tư vấn bàn học','2026-03-15 11:32:41'),(63,0,6,'Dạ, chào anh/chị! Hiện tại cửa hàng nội thất SLAND đang có hai mẫu bàn làm việc rất phù hợp để sử dụng làm bàn học với thiết kế chắc chắn và hiện đại. Em xin gửi tới anh/chị thông tin chi tiết của từng sản phẩm ạ:\n\n1.  **Bàn làm việc cách tân**\n    *   **Giá:** 3.000.000đ\n    *   **Chất liệu:** Gỗ MDF chống ẩm\n    *   **Kích thước:** 1.4m x 0.6m\n    *   **Bảo hành:** 18 tháng\n\n2.  **Bàn làm việc đơn giản**\n    *   **Giá:** 3.200.000đ\n    *   **Chất liệu:** Mặt gỗ công nghiệp, Chân sắt sơn tĩnh điện\n    *   **Kích thước:** 1.2m x 0.6m\n    *   **Bảo hành:** 12 tháng\n\nCả hai mẫu bàn này đều được SLAND hỗ trợ miễn phí vận chuyển trong khu vực nội thành Hà Nội. Nếu anh/chị đã ưng ý mẫu nào, hãy hướng dẫn thêm sản phẩm vào giỏ hàng và tiến hành thanh toán trên hệ thống để bên em chuẩn bị đơn hàng ngay nhé.\n\nAnh/chị đang muốn tìm mẫu bàn có kích thước rộng rãi như mẫu cách tân hay mẫu chân sắt gọn gàng hơn ạ?','2026-03-15 11:32:47'),(64,4,0,'hi','2026-03-15 12:15:17'),(65,0,4,'Dạ, chào bạn! Chào mừng bạn đã đến với cửa hàng nội thất SLAND. Rất vui được hỗ trợ bạn trong việc tìm kiếm những món đồ nội thất chất lượng cho không gian sống của mình.\n\nHiện tại, SLAND đang cung cấp đa dạng các dòng sản phẩm như giường ngủ, bàn ăn, tủ quần áo, sofa và bàn làm việc với thiết kế từ đơn giản đến hiện đại. Tất cả sản phẩm đều được bảo hành uy tín và miễn phí vận chuyển trong khu vực nội thành.\n\nKhông biết bạn đang quan tâm đến sản phẩm nào hoặc cần trang trí cho không gian nào trong nhà (phòng ngủ, phòng khách hay phòng làm việc) để mình có thể tư vấn chi tiết nhất cho bạn ạ?','2026-03-15 12:15:21'),(66,4,0,'hello','2026-03-15 14:16:05'),(67,0,4,'Dạ, xin chào bạn! Rất vui được đón tiếp bạn ghé thăm cửa hàng nội thất SLAND. Chúng tôi luôn sẵn sàng hỗ trợ bạn tìm kiếm những món đồ nội thất phù hợp nhất để làm đẹp cho không gian sống của gia đình mình.\n\nHiện tại, SLAND đang cung cấp các dòng sản phẩm đa dạng từ giường ngủ, tủ quần áo đến bàn ghế làm việc với nhiều chất liệu cao cấp như gỗ sồi, gỗ óc chó và gỗ công nghiệp hiện đại. Tất cả sản phẩm đều đi kèm chính sách bảo hành uy tín và miễn phí vận chuyển trong khu vực nội thành.\n\nKhông biết bạn đang quan tâm đến sản phẩm nào cho không gian phòng ngủ, phòng khách hay phòng làm việc của mình để mình có thể tư vấn chi tiết hơn cho bạn ạ?','2026-03-15 14:16:10'),(68,4,0,'tư vấn san phẩm tủ quần áo','2026-03-15 14:16:26'),(69,0,4,'Dạ, chào bạn! Rất vui được tư vấn cho bạn về các mẫu tủ quần áo tại nội thất SLAND. Hiện tại, cửa hàng chúng mình đang có các dòng tủ quần áo với chất liệu bền bỉ và thiết kế đa dạng để bạn lựa chọn như sau:\n\n1. **Tủ quần áo hiện đại**\n- Giá: 7.000.000đ\n- Chất liệu: Gỗ công nghiệp HDF\n- Kích thước: 1.8m x 2.2m\n- Bảo hành: 24 tháng\n\n2. **Tủ quần áo cách tân mang thiên hướng hiện đại**\n- Giá: 6.500.000đ\n- Chất liệu: Gỗ sồi nhập khẩu\n- Kích thước: 2m x 2.2m\n- Bảo hành: 12 tháng\n\n3. **Tủ quần áo cách tân**\n- Giá: 5.000.000đ\n- Chất liệu: Gỗ tự nhiên\n- Kích thước: 3m x 5m\n- Bảo hành: 12 tháng\n\nTất cả các sản phẩm trên đều được SLAND hỗ trợ','2026-03-15 14:16:33'),(70,4,0,'thanh toán như thế nào','2026-03-15 14:16:44'),(71,0,4,'Dạ, tại nội thất SLAND, quy trình đặt hàng và thanh toán rất đơn giản và thuận tiện ạ. Để mua sắm các sản phẩm nội thất, bạn vui lòng thực hiện theo hướng dẫn sau:\n\n1. **Chọn sản phẩm:** Bạn lựa chọn mẫu sản phẩm ưng ý (ví dụ như tủ quần áo, giường ngủ hay bàn ăn).\n2. **Thêm vào giỏ hàng:** Sau khi đã chọn được sản phẩm, bạn nhấn vào nút \"Thêm vào giỏ hàng\".\n3. **Thanh toán:** Bạn kiểm tra lại thông tin trong giỏ hàng và tiến hành nhấn \"Thanh toán\". Tại đây, bạn điền thông tin nhận hàng và lựa chọn phương thức thanh toán phù hợp.\n\nBên mình có chính sách miễn phí vận chuyển cho các đơn hàng trong khu vực nội thành và bảo hành sản phẩm lên đến 24 tháng tùy loại. Nếu bạn cần hỗ trợ gấp, bạn có thể gọi trực tiếp đến Hotline: 0326976832 để nhân viên bên mình hỗ trợ đặt hàng ngay lập tức.\n\nKhông biết bạn đã chọn được mẫu tủ quần áo nào trong các mẫu mình vừa giới thiệu để mình hướng dẫn bạn thêm vào giỏ hàng chưa ạ?','2026-03-15 14:16:51');
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_details`
--

DROP TABLE IF EXISTS `order_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `product_id` int NOT NULL,
  `product_name` varchar(255) NOT NULL,
  `price` decimal(15,2) NOT NULL,
  `quantity` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `order_details_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `order_details_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_details`
--

LOCK TABLES `order_details` WRITE;
/*!40000 ALTER TABLE `order_details` DISABLE KEYS */;
INSERT INTO `order_details` VALUES (1,1,8,'Sofa đơn giản',6500000.00,1),(2,2,2,'Giường ngủ cách tân',3000000.00,3),(3,2,1,'Giường ngủ cao cấp',4500000.00,2),(4,2,8,'Sofa đơn giản',6500000.00,1),(5,2,3,'Bàn ăn đơn giản hiện đại',3500000.00,1),(6,3,10,'Bàn làm việc đơn giản',2100000.00,2),(7,3,9,'Bàn làm việc cách tân',3000000.00,1),(8,17,2,'Giường ngủ cách tân',3000000.00,1),(9,18,8,'Sofa đơn giản',6500000.00,3),(10,19,1,'Giường ngủ cao cấp',4500000.00,1),(11,19,9,'Bàn làm việc cách tân',3000000.00,1),(12,20,9,'Bàn làm việc cách tân',3000000.00,2),(13,21,10,'Bàn làm việc đơn giản',3200000.00,3),(14,22,12,'tu quan ao cach tan',5000000.00,3),(15,23,9,'Bàn làm việc cách tân',3000000.00,1),(16,24,2,'Giường ngủ cách tân',3000000.00,1),(17,25,9,'Bàn làm việc cách tân',3000000.00,1),(18,25,12,'tu quan ao cach tan',5000000.00,1),(19,26,8,'Sofa đơn giản',6500000.00,1),(20,27,2,'Giường ngủ cách tân',3000000.00,1),(21,28,9,'Bàn làm việc cách tân',3000000.00,1),(22,29,9,'Bàn làm việc cách tân',3000000.00,1),(23,30,9,'Bàn làm việc cách tân',3000000.00,1),(24,31,12,'tu quan ao cach tan',5000000.00,1),(25,32,8,'Sofa đơn giản',6500000.00,1),(26,32,9,'Bàn làm việc cách tân',3000000.00,1),(27,33,10,'Bàn làm việc đơn giản',3200000.00,1),(28,33,8,'Sofa đơn giản',6500000.00,1),(29,34,13,'dang giang son',10000.00,1),(30,34,9,'Bàn làm việc cách tân',3000000.00,1),(31,34,10,'Bàn làm việc đơn giản',3200000.00,1),(32,35,12,'tu quan ao cach tan',5000000.00,1),(33,36,12,'tu quan ao cach tan',5000000.00,1),(34,37,5,'Tủ quần áo hiện đại',7000000.00,1);
/*!40000 ALTER TABLE `order_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_status_history`
--

DROP TABLE IF EXISTS `order_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_status_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `status` varchar(100) NOT NULL,
  `note` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  CONSTRAINT `osh_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_status_history`
--

LOCK TABLES `order_status_history` WRITE;
/*!40000 ALTER TABLE `order_status_history` DISABLE KEYS */;
INSERT INTO `order_status_history` VALUES (1,31,'Chờ xử lý','Đơn hàng đang chờ xử lý','2026-03-15 15:12:22'),(2,29,'Đang giao hàng','Đơn hàng đã được bàn giao cho đơn vị vận chuyển','2026-03-15 15:12:34'),(3,29,'Đang giao hàng','Đơn hàng đã được bàn giao cho đơn vị vận chuyển','2026-03-15 15:12:36'),(4,29,'Đang giao hàng','Đơn hàng đã được bàn giao cho đơn vị vận chuyển','2026-03-15 15:12:45'),(5,31,'Đã giao hàng','Đơn hàng đã giao thành công đến tay khách hàng','2026-03-15 15:12:51'),(6,31,'Đã giao hàng','Đơn hàng đã giao thành công đến tay khách hàng','2026-03-15 15:12:53');
/*!40000 ALTER TABLE `order_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `fullname` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `address` text NOT NULL,
  `payment_method` varchar(50) NOT NULL,
  `voucher_code` varchar(50) DEFAULT NULL,
  `discount_amount` decimal(15,2) DEFAULT '0.00',
  `total_money` decimal(15,2) NOT NULL,
  `status` varchar(50) DEFAULT 'Đang xử lý',
  `estimated_delivery` date DEFAULT NULL,
  `delivery_note` varchar(255) DEFAULT NULL,
  `ref_code` varchar(20) DEFAULT NULL,
  `momo_order_id` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,1,'Đặng Giang Sơn','0326976832','Lưu Khê - Liên Bạt - Ứng Hòa - Hà nội','cod',NULL,0.00,6500000.00,'Đang giao hàng',NULL,NULL,NULL,NULL,'2025-11-23 17:42:46'),(2,6,'Đới Thị Linh','0214403841','ưgrgin  ưgnro sdjg','cod',NULL,0.00,28000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-11-23 20:36:08'),(3,6,'Đới Thị Linh','0214403841','ừaegeggegs egewwf','cod',NULL,0.00,7200000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-11-24 07:07:37'),(4,6,'Đới Thị Linh','0214403841','số 4 thông ko biết, Xã Quảng Hải, Huyện Quảng Xương, Tỉnh Thanh Hóa','banking',NULL,0.00,17500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-11-24 07:35:55'),(5,6,'Đới Thị Linh','0214403841','số 4 thông ko biết, Xã Quảng Hải, Huyện Quảng Xương, Tỉnh Thanh Hóa','banking',NULL,0.00,34500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-11-24 07:39:29'),(6,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Hương Vĩ, Huyện Yên Thế, Tỉnh Bắc Giang','banking',NULL,0.00,35500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-11-25 05:55:25'),(7,10,'Thắng','0123456789','số 4 thông ko biết, Thị trấn Trùng Khánh, Huyện Trùng Khánh, Tỉnh Cao Bằng','banking',NULL,0.00,19500000.00,'Đang giao hàng',NULL,NULL,NULL,NULL,'2025-11-25 07:00:55'),(8,10,'Thắng','12345678','số 4 thông ko biết, Xã Pá Mỳ, Huyện Mường Nhé, Tỉnh Điện Biên','cod',NULL,0.00,12200000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-11-25 07:27:05'),(9,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Trung Thành, Huyện Đà Bắc, Tỉnh Hoà Bình','cod',NULL,0.00,22500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:16:06'),(10,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Phường Nếnh, Thị xã Việt Yên, Tỉnh Bắc Giang','cod',NULL,0.00,6500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:19:57'),(11,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Đại Bái, Huyện Gia Bình, Tỉnh Bắc Ninh','cod',NULL,0.00,6500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:23:31'),(12,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Đông Cửu, Huyện Thanh Sơn, Tỉnh Phú Thọ','cod',NULL,0.00,14500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:26:36'),(13,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Đồng Cương, Huyện Yên Lạc, Tỉnh Vĩnh Phúc','cod',NULL,0.00,6500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:30:27'),(14,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Trung Kiên, Huyện Yên Lạc, Tỉnh Vĩnh Phúc','banking',NULL,0.00,16000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:35:18'),(15,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Trung Kiên, Huyện Yên Lạc, Tỉnh Vĩnh Phúc','banking',NULL,0.00,16000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:37:32'),(16,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Thọ Văn, Huyện Tam Nông, Tỉnh Phú Thọ','banking',NULL,0.00,8000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:37:56'),(17,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Cự Đồng, Huyện Thanh Sơn, Tỉnh Phú Thọ','banking',NULL,0.00,3000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-10 10:41:13'),(18,6,'nam','038132473','so 1 lang luu khe, Xã Quang Sơn, Huyện Đồng Hỷ, Tỉnh Thái Nguyên','cod',NULL,0.00,19500000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2025-12-15 12:21:19'),(19,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Hương Nộn, Huyện Tam Nông, Tỉnh Phú Thọ','cod',NULL,0.00,7500000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-16 13:53:08'),(20,6,'nam','0326976832','so 1 lang luu khe, Xã Vũ Sơn, Huyện Bắc Sơn, Tỉnh Lạng Sơn','cod',NULL,0.00,6000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-16 14:01:33'),(21,10,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Vạn Xuân, Huyện Tam Nông, Tỉnh Phú Thọ','cod',NULL,0.00,9600000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-16 14:18:44'),(22,6,'Đặng Giang Sơn','0326976832','so 1 lang luu khe, Xã Khánh Long, Huyện Tràng Định, Tỉnh Lạng Sơn','cod',NULL,0.00,15000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-16 14:28:43'),(23,11,'Giang Sơn Đặng','jffhgch','so 1 lang luu khe, Xã Phúc Lộc, Huyện Ba Bể, Tỉnh Bắc Kạn','cod',NULL,0.00,3000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-16 23:47:03'),(24,10,'Đới Thị Linh','0123456789','số 4 thông ko biết, Phường Tân Hòa, Thành phố Hòa Bình, Tỉnh Hoà Bình','banking',NULL,0.00,3000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2025-12-25 12:51:45'),(25,6,'kk','0326976832','111, Phường Dương Nội, Quận Hà Đông, Thành phố Hà Nội','banking',NULL,0.00,8000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-02 01:34:20'),(26,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã Nhân Lý, Huyện Chi Lăng, Tỉnh Lạng Sơn','momo',NULL,0.00,6500000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 08:20:21'),(27,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã Trung Kiên, Huyện Yên Lạc, Tỉnh Vĩnh Phúc','momo',NULL,0.00,3000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 08:23:29'),(28,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã Liên Chung, Huyện Tân Yên, Tỉnh Bắc Giang','momo',NULL,0.00,3000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 08:44:33'),(29,6,'Kenji AC','0326976832','so 1 lang luu khe, Thị trấn Biển Động, Huyện Lục Ngạn, Tỉnh Bắc Giang','momo',NULL,0.00,3000000.00,'Đang giao hàng',NULL,NULL,NULL,NULL,'2026-03-15 08:53:19'),(30,6,'Kenji AC','0326976832','so 1 lang luu khe, Thị trấn Biển Động, Huyện Lục Ngạn, Tỉnh Bắc Giang','momo',NULL,0.00,3000000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 08:55:51'),(31,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã Thắng Sơn, Huyện Thanh Sơn, Tỉnh Phú Thọ','momo',NULL,0.00,5000000.00,'Đã giao hàng',NULL,NULL,NULL,NULL,'2026-03-15 08:56:07'),(32,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã An Lạc, Huyện Sơn Động, Tỉnh Bắc Giang','momo',NULL,0.00,9500000.00,'Chờ xử lý',NULL,NULL,NULL,'SLAND_1773565645_6','2026-03-15 09:07:52'),(33,6,'Kenji AC','0326976832','so 1 lang luu khe, Xã Phùng Nguyên, Huyện Lâm Thao, Tỉnh Phú Thọ','banking','',0.00,9700000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 15:45:38'),(34,12,'linh','0326976832','so 1 lang luu khe, Xã Thanh Luận, Huyện Sơn Động, Tỉnh Bắc Giang','cod','',0.00,6210000.00,'Chờ xử lý',NULL,NULL,NULL,NULL,'2026-03-15 16:05:15'),(35,11,'Giang Sơn Đặng','0326976822','so 1 lang luu khe, Phường 4, Quận 8, Thành phố Hồ Chí Minh','cod','',0.00,5000000.00,'Chờ xử lý','2026-05-05',NULL,NULL,NULL,'2026-05-02 06:34:55'),(36,6,'Kenji AC','0326976822','so 1 lang luu khe, Xã Thành Hòa, Huyện Văn Lãng, Tỉnh Lạng Sơn','cod','',0.00,5000000.00,'Chờ xử lý','2026-05-06',NULL,'SONPRO5575',NULL,'2026-05-02 06:54:06'),(37,6,'Kenji AC','0326976822','so 1 lang luu khe, Xã Phong Dụ, Huyện Tiên Yên, Tỉnh Quảng Ninh','cod','',0.00,7000000.00,'Chờ xử lý','2026-05-07',NULL,'SONPRO5575',NULL,'2026-05-04 01:10:15');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `price` decimal(10,2) NOT NULL,
  `old_price` decimal(15,0) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `category_id` int DEFAULT '1',
  `material` varchar(100) DEFAULT NULL,
  `dimensions` varchar(100) DEFAULT NULL,
  `warranty` varchar(50) DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'Giường ngủ cao cấp','Giường ngủ phong cách hiện đại, được làm từ gỗ Sồi Nga nhập khẩu cao cấp, chống mối mọt và cong vênh. Thiết kế sang trọng, phù hợp với mọi không gian phòng ngủ chung cư và biệt thự.',4500000.00,5850000,'picture/giuongngu2.jpg',1,'Gỗ Sồi Nga','1.8m x 2m','24 tháng',87),(2,'Giường ngủ cách tân','Giường ngủ với thiết kế cách tân, nhẹ nhàng và tinh tế. Sử dụng vật liệu thân thiện với môi trường, mang lại cảm giác ấm cúng và thoải mái tối đa cho người dùng.',3000000.00,3900000,'picture/giuongngu1.jpg',1,'Gỗ MDF phủ Melamine','1.6m x 2m','12 tháng',672),(3,'Bàn ăn đơn giản hiện đại','Bộ bàn ăn 4 ghế, thiết kế tối giản nhưng sang trọng. Mặt bàn chống thấm nước, dễ dàng vệ sinh. Phù hợp với không gian bếp nhỏ và vừa.',3500000.00,4550000,'picture/banan2.jpg',2,'Gỗ Cao su tự nhiên','1.2m x 0.8m','18 tháng',43),(4,'Bàn ăn cách tân','Bộ bàn ăn 6 ghế, thiết kế độc đáo, chân bàn cách điệu. Mang lại vẻ đẹp khác biệt và là điểm nhấn cho phòng ăn của gia đình bạn.',5000000.00,NULL,'picture/banan1.jpg',2,'Gỗ Óc chó (Walnut)','1.6m x 0.9m','24 tháng',65),(5,'Tủ quần áo hiện đại','Tủ quần áo 3 cánh mở, thiết kế cánh trượt tiết kiệm không gian. Bên trong chia ngăn hợp lý, có thanh treo đèn LED tự động.',7000000.00,NULL,'picture/tu2.jpg',1,'Gỗ công nghiệp HDF','1.8m x 2.2m','24 tháng',22),(6,'Tủ quần áo cách tân mang thiên hướng hiện đại','Tủ quần áo lớn 4 cánh, kết hợp ngăn kéo tiện dụng. Phong cách Bắc Âu, màu sắc trung tính dễ dàng kết hợp với nội thất khác.',6500000.00,NULL,'picture/tu1.webp',1,'Gỗ sồi nhập khẩu','2m x 2.2m','12 tháng',42),(8,'Sofa đơn giản','Bộ sofa đôi nhỏ gọn, bọc vải bố canvas, đệm rời dễ dàng tháo giặt. Phù hợp cho căn hộ mini hoặc góc đọc sách.',6500000.00,NULL,'picture/sofa1.jpg',3,'Khung gỗ tràm, Vải bố','1.8m x 0.8m','12 tháng',0),(9,'Bàn làm việc cách tân','Bàn làm việc góc, có ngăn kéo và kệ sách tích hợp. Thiết kế tối ưu cho các công việc cần nhiều không gian lưu trữ.',3000000.00,NULL,'picture/banlamviec2.jpg',4,'Gỗ MDF chống ẩm','1.4m x 0.6m','18 tháng',25),(10,'Bàn làm việc đơn giản','Bàn làm việc chân sắt, mặt gỗ đơn giản. Phong cách công nghiệp, tiết kiệm chi phí nhưng vẫn đảm bảo độ bền cao.',3200000.00,NULL,'picture/banlamviec1.jpg',4,'Mặt gỗ công nghiệp, Chân sắt sơn tĩnh điện','1.2m x 0.6m','12 tháng',21),(12,'tu quan ao cach tan','San pham duoc tao ra tu Giang Son',5000000.00,NULL,'picture/sp_1765929109.jpg',4,'gỗ tự nhiên','3x5m','12 tháng',28),(13,'dang giang son','sgg',10000.00,NULL,'picture/sp_1773569589.jpg',3,NULL,NULL,NULL,99);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `product_id` int DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `rating` int NOT NULL DEFAULT '5' COMMENT 'Số sao đánh giá (1-5)',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_review_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE SET NULL,
  CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,1,NULL,'ưvrebyhyn','2025-11-23 19:16:09',5),(2,1,NULL,'tôi cảm thấy rất ưng','2025-11-23 19:26:23',5),(3,6,NULL,'quá ôk','2025-11-23 19:33:05',5),(4,6,NULL,'segwefewgfwrghtrjyjti6ro67i6u','2025-11-24 07:43:30',5),(5,10,NULL,'Sản phẩm rất ôk , tư vấn nhiệt tình\r\n','2025-11-25 06:58:57',5),(6,6,NULL,'sản phẩm rất tuyệt vời','2025-12-16 14:42:45',4),(7,6,NULL,'sản phẩm rất tuyệt vời','2025-12-16 14:42:45',5),(8,11,10,'0','2026-05-06 01:33:40',5);
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_addresses`
--

DROP TABLE IF EXISTS `user_addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_addresses` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ward` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address_line` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_addresses`
--

LOCK TABLES `user_addresses` WRITE;
/*!40000 ALTER TABLE `user_addresses` DISABLE KEYS */;
INSERT INTO `user_addresses` VALUES (1,11,'Kenji AC','0326976832','Tỉnh Cao Bằng','Huyện Bảo Lâm','Xã Nam Quang','41a phu dien',1,'2026-03-28 16:56:48');
/*!40000 ALTER TABLE `user_addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `gender` enum('male','female','other') DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `referred_by` varchar(20) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('user','admin') DEFAULT 'user',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reset_token` varchar(255) DEFAULT NULL,
  `reset_expire` datetime DEFAULT NULL,
  `is_locked` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'sondeptrai@gmail.com','sondeptrai@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$xxG/D7mDwFNQZ3OaG1y4oONExSC1Ht5hObRUpNhe728cJXuFYXgNC','user','2025-11-23 09:46:21','2026-04-27 16:02:13',NULL,NULL,0),(3,'ADMin gian','Dangnamson24@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$5UUzj5MX4aaKu4hyazNYbOXdBsC40DaKGsj/34PRDJhVV1lH/X5Su','admin','2025-11-23 17:18:11','2026-04-27 16:02:13',NULL,NULL,0),(4,'admin@sland.com','admin@sland.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$50A.YMFmQ6g6IK13Kr30fOIJYiz8TPUxGQGiaRJnVVxzeKQjdfJp6','admin','2025-11-23 17:21:50','2026-04-27 16:02:13',NULL,NULL,0),(5,'Linh Xinh Gái','doithilinh@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$pEOCLiIYHm5xjrhwThfsTOJ4mXqd5LsTbTZYgNzy91mV7MHWBgVxm','user','2025-11-23 19:18:58','2026-04-27 16:02:13',NULL,NULL,0),(6,'Linhxinhgai','doithilinh1@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$WmyWYQyT54qPUkmPK5JBLe4bzE0OznB1w8zoiyFnTIyw7KBh3NFnG','user','2025-11-23 19:21:12','2026-04-27 16:02:13',NULL,NULL,0),(7,'thang','sondeptrai1@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$/AKM8.WBhpwvp8w07njSvu7pfKRkf3sd4Tnj2ieZ5JMwaWU2uv2O.','user','2025-11-23 19:23:28','2026-04-27 16:02:13',NULL,NULL,0),(8,'dotuanh','1234@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$LYK2Lw4TSNRtULdK7KXnle1YV4Aoswj2lZ5NcjmLkEfGCKg4zS2zq','user','2025-11-24 06:00:41','2026-04-27 16:02:13',NULL,NULL,0),(9,'giangsondeptrai','2311061532@hunre.edu.vn',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$.bfWP8.DqyH0Y4XKE6MWl.ohrapo.jRyXvjdy7Qgor.dTgGHfVRAG','user','2025-11-24 06:38:15','2026-04-27 16:02:13','7f4a42907c35f630d4972f71e2d49c9d','2025-12-08 20:24:48',0),(10,'TeacherThang','2311061532@hunre.edu.vn2311061532',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$es2utKsiPuRn58ZVJAz/T.jbmRK6.BL2uPGe7ABllJNPOgAcwDPNS','user','2025-11-25 06:57:53','2026-04-27 16:02:13',NULL,NULL,0),(11,'sonpro241','dangnamso2n24@gmail.com','Đặng Giang Sơn','0326976832','2005-12-05','male','picture/avatars/avatar_11_1777720616.png',NULL,'$2y$10$pZ7CKlB9E5hjgKGBmUHUjuosBWRYOyZNkyFVFd.Kv3rP5rXH5qEQe','user','2025-12-16 15:05:50','2026-05-02 11:16:56',NULL,NULL,0),(12,'thuylinh','2311061746@hunre.edu.vn',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$QpRWSf1ginm4YR3QyRP3EOpgNM0ykytKuUn8p3UKmevBrPREAKBJa','user','2026-03-15 16:04:17','2026-04-27 16:02:13',NULL,NULL,0),(13,'danggiangnam','giangnam@gmail.com',NULL,NULL,NULL,NULL,NULL,NULL,'$2y$10$7av.fcRcoGWlW9W16LVUHu.mHOcrH8.AIu/1dcDMN7sfI0VkKa4o2','user','2026-05-02 15:21:23','2026-05-02 15:21:23',NULL,NULL,0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vouchers`
--

DROP TABLE IF EXISTS `vouchers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vouchers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `discount_type` enum('percent','fixed') DEFAULT 'percent',
  `discount_value` decimal(10,2) NOT NULL,
  `min_order` decimal(15,2) DEFAULT '0.00',
  `max_uses` int DEFAULT '100',
  `used_count` int DEFAULT '0',
  `expires_at` date DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vouchers`
--

LOCK TABLES `vouchers` WRITE;
/*!40000 ALTER TABLE `vouchers` DISABLE KEYS */;
INSERT INTO `vouchers` VALUES (1,'SLAND10','percent',10.00,500000.00,100,0,NULL,1,'2026-03-15 15:26:27'),(2,'SLAND20','percent',20.00,2000000.00,50,0,NULL,1,'2026-03-15 15:26:27'),(3,'GIAM100K','fixed',100000.00,1000000.00,30,0,NULL,1,'2026-03-15 15:26:27'),(4,'NEWUSER','percent',15.00,0.00,200,0,NULL,1,'2026-03-15 15:26:27');
/*!40000 ALTER TABLE `vouchers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-10  0:43:44
