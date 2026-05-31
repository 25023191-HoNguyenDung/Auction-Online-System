Hệ thống đấu giá trực tuyến — Bài tập lớn Lập trình nâng cao

1. Mô tả hệ thống
Hệ thống đấu giá trực tuyến theo mô hình Client–Server với giao tiếp qua TCP Socket + JSON. Người dùng có thể đăng ký tài khoản, đăng sản phẩm (Seller), tham gia đấu giá theo thời gian thực (Bidder), hoặc quản lý toàn hệ thống (Admin). Tham khảo mô hình eBay Auctions.
Hệ thống gồm 3 module Maven:
•	common – Thư viện dùng chung: giao thức truyền thông (JSON messages), các exception tùy chỉnh.
•	server – Backend xử lý nghiệp vụ, quản lý kết nối TCP đa luồng, truy cập CSDL MySQL.
•	client – Giao diện đồ họa JavaFX, giao tiếp với server qua TCP Socket realtime.

2. Công nghệ sử dụng
Thành phần	Công nghệ / Thư viện
Ngôn ngữ	Java 21
Giao diện (Client)	JavaFX 21 + FXML
Build tool	Maven 3.8+
Giao tiếp mạng	TCP Socket, JSON (Jackson 2.17)
Cơ sở dữ liệu	MySQL 8.0
Connection pool	HikariCP 5.1
Logging	SLF4J + Logback
Unit Test	JUnit 5.10, Mockito 5.11
CI/CD	GitHub Actions

3. Yêu cầu cài đặt
Công cụ	Phiên bản tối thiểu	Lệnh kiểm tra
JDK	21	java -version
Maven	3.8	mvn -version
MySQL	8.0	mysql --version

Lưu ý: JavaFX 21 đã được nhúng sẵn qua Maven dependency, không cần cài JavaFX riêng.
4. Cấu trúc thư mục

Auction-Online-System/
├── pom.xml                              # Parent POM (multi-module)
├── docs/                                # Tài liệu kiến trúc, schema, protocol
│   ├── architecture.md
│   ├── database-schema.md
│   └── protocol.md
├── .github/workflows/
│   ├── ci.yml                           # CI build + test tự động
│   └── qodana_code_quality.yml
│
├── common/                              # Module dùng chung (client & server)
│   ├── pom.xml
│   └── src/main/java/…/common/
│       ├── exception/                   # Custom exceptions
│       │   ├── AuctionConnectException.java
│       │   ├── AuctionMisMatchException.java
│       │   ├── AuctionTimeException.java
│       │   ├── InvalidBidException.java
│       │   └── ValidRegisterException.java
│       └── protocol/                    # Giao thức JSON Socket
│           ├── MessageEnvelope.java
│           ├── MessageType.java
│           ├── ProtocolMapper.java
│           ├── LoginReqPayload.java / LoginResPayload.java
│           ├── PlaceBidReqPayload.java / PlaceBidResPayload.java
│           ├── RegisterAutoBidReqPayload.java / …ResPayload.java
│           ├── BidUpdatedEventPayload.java
│           ├── AuctionClosedEventPayload.java
│           └── … (các Payload khác)
│
├── server/                              # Module server (backend)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/…/server/
│       │   │   ├── config/
│       │   │   │   └── DatabaseConfig.java         # Singleton + HikariCP
│       │   │   ├── concurrency/
│       │   │   │   ├── AuctionLockManager.java     # ReentrantReadWriteLock
│       │   │   │   ├── ConnectionHolder.java
│       │   │   │   └── TransactionManager.java
│       │   │   ├── model/
│       │   │   │   ├── User.java  /  Bidder.java  /  Seller.java  /  Admin.java
│       │   │   │   ├── Item.java  /  Electronics.java  /  Art.java  /  Vehicle.java
│       │   │   │   ├── Auction.java  /  AuctionStatus.java
│       │   │   │   ├── BidTransaction.java
│       │   │   │   └── AutoBidProfile.java
│       │   │   ├── dao/
│       │   │   │   ├── UserDao.java  /  ItemDao.java  /  AuctionDao.java
│       │   │   │   ├── BidDao.java  /  AutoBidProfileDao.java  (interfaces)
│       │   │   │   └── jdbc/
│       │   │   │       ├── JdbcUserDao.java
│       │   │   │       ├── JdbcItemDao.java
│       │   │   │       ├── JdbcAuctionDao.java
│       │   │   │       ├── JdbcBidDao.java
│       │   │   │       └── JdbcAutoBidProfileDao.java
│       │   │   ├── service/
│       │   │   │   ├── AuctionService.java  /  AuctionServiceImpl.java
│       │   │   │   ├── AuctionLogicManager.java    # Xử lý bid + lock
│       │   │   │   ├── AuctionClosingService.java  # Scheduler đóng phiên
│       │   │   │   ├── AutoBidService.java         # Auto-bidding
│       │   │   │   ├── AuthService.java  /  ItemService.java  /  UserService.java
│       │   │   │   └── ValidRegister.java
│       │   │   ├── controller/
│       │   │   │   ├── AuthController.java
│       │   │   │   └── ItemController.java
│       │   │   ├── network/
│       │   │   │   ├── ServerApplication.java      # Entry point
│       │   │   │   ├── AuctionServer.java          # TCP listener, ThreadPool
│       │   │   │   ├── ClientConnectionHandler.java
│       │   │   │   ├── RequestDispatcher.java      # Router theo MessageType
│       │   │   │   └── SubscriptionRegistry.java
│       │   │   ├── observer/
│       │   │   │   ├── AuctionObserver.java  (interface)
│       │   │   │   ├── AuctionEvent.java
│       │   │   │   ├── AuctionEventPublisher.java  # Singleton broadcaster
│       │   │   │   └── ClientAuctionObserver.java
│       │   │   └── pattern/
│       │   │       ├── AuctionManager.java         # Singleton
│       │   │       ├── ItemFactory.java            # Factory Method
│       │   │       └── BidStrategy.java            # Strategy
│       │   └── resources/
│       │       ├── application.properties          # Cấu hình DB & port
│       │       └── db/
│       │           ├── schema.sql
│       │           └── seed.sql
│       └── test/java/…/server/
│           ├── concurrency/   ConcurrentBiddingTest.java
│           ├── dao/           AuctionDaoTest.java
│           ├── integration/   PlaceBidIntegrationTest.java
│           ├── observer/      AuctionEventPublisherTest.java
│           └── service/       AuctionServiceTest, AuthServiceTest,
│                              AutoBidServiceTest, AuctionClosingServiceTest,
│                              ItemServiceTest
│
└── client/                              # Module client (JavaFX GUI)
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/…/client/
        │   │   ├── AppLauncher.java  /  MainApp.java  (entry point)
        │   │   ├── controller/
        │   │   │   ├── LoginController.java  /  RegisterController.java
        │   │   │   ├── AuctionListController.java  /  AuctionDetailController.java
        │   │   │   ├── BidController.java  /  BidHistoryController.java
        │   │   │   ├── SellerDashboardController.java
        │   │   │   └── AdminDashboardController.java
        │   │   ├── viewmodel/
        │   │   │   ├── LoginViewModel.java
        │   │   │   ├── AuctionListViewModel.java
        │   │   │   └── AuctionDetailViewModel.java
        │   │   ├── model/
        │   │   │   ├── AuctionItem.java  /  Bid.java  /  User.java
        │   │   ├── network/
        │   │   │   ├── ServerConnection.java       # Singleton TCP connection
        │   │   │   ├── ClientMessageSender.java    # Gửi request lên server
        │   │   │   └── ServerEventListener.java    # Nhận push event từ server
        │   │   ├── sessions/
        │   │   │   └── UserSession.java
        │   │   ├── data/
        │   │   │   └── AuctionStore.java
        │   │   └── util/
        │   │       ├── AlertUtils.java
        │   │       ├── NavigationUtils.java
        │   │       └── TimeFormatUtils.java
        │   └── resources/…/client/
        │       ├── view/                           # FXML files
        │       │   ├── Login.fxml  /  Register.fxml
        │       │   ├── AuctionList.fxml  /  AuctionDetail.fxml
        │       │   ├── BidScreen.fxml  /  BidHistory.fxml
        │       │   ├── SellerDashboard.fxml
        │       │   └── AdminDashboard.fxml
        │       ├── css/
        │       │   └── style.css
        │       └── image/
        │           └── (bg.png, logo.png)
5. Cài đặt Database
Bước 1 – Đăng nhập MySQL
mysql -u root -p

Bước 2 – Chạy schema và seed
Linux / macOS:
mysql -u root -p < server/src/main/resources/db/schema.sql
mysql -u root -p < server/src/main/resources/db/seed.sql

Windows (Command Prompt / PowerShell):
mysql -u root -p < server\src\main\resources\db\schema.sql
mysql -u root -p < server\src\main\resources\db\seed.sql

Bước 3 – Cấu hình kết nối
Chỉnh sửa server/src/main/resources/application.properties:
db.url=jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC
db.username=root
db.password=YOUR_PASSWORD_HERE

6. Cách chạy chương trình
* Lưu ý: Phải khởi động Server trước, sau đó mới chạy Client.
Bước 1 – Build toàn bộ project
Chạy tại thư mục gốc — hoạt động trên Linux, macOS và Windows:
mvn clean package -DskipTests

Bước 2 – Khởi động Server
Cách 1: Maven (Linux / macOS / Windows):
cd server
mvn exec:java -Dexec.mainClass="com.auction.server.network.ServerApplication"

Cách 2: JAR đã build (Linux / macOS / Windows):
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar

Server lắng nghe tại cổng 1337 theo mặc định.

Bước 3 – Khởi động Client
Có thể mở nhiều cửa sổ client song song để test nhiều người dùng.

Cách 1: Maven JavaFX plugin (Linux / macOS / Windows):
cd client
mvn javafx:run

Cách 2: IDE (IntelliJ IDEA / Eclipse):
Mở project, tìm class com.auction.client.AppLauncher và nhấn Run.

Lưu ý trên macOS — nếu gặp lỗi JavaFX rendering:
cd client
mvn javafx:run -Djavafx.platform=mac

Bước 4 – Chạy Tests
mvn test                  # Tất cả tests
cd server && mvn test     # Chỉ server
cd common && mvn test     # Chỉ common

Tài khoản mẫu (từ seed.sql)

Vai trò	Username	Password
Admin	admin	admin2308
Seller	seller1	seller12308
Bidder	bidder1	bidder12308
Bidder	bach123	pach2308

7. Chức năng đã hoàn thành
7.1 Chức năng bắt buộc
Quản lý người dùng
• Đăng ký tài khoản (kiểm tra trùng username, email)
• Đăng nhập / đăng xuất
• Phân quyền 3 vai trò: Bidder, Seller, Admin
• Quản lý số dư tài khoản (nạp tiền, rút tiền)
Quản lý sản phẩm đấu giá
• Seller: thêm, sửa, xóa sản phẩm
• 3 loại sản phẩm: Electronics, Art, Vehicle (kế thừa từ Item)
• Thông tin: tên, mô tả, giá khởi điểm, giá hiện tại, thời gian bắt đầu/kết thúc

Tham gia đấu giá
• Bidder đặt giá cao hơn giá hiện tại
• Kiểm tra tính hợp lệ của giá đấu (so sánh giá, kiểm tra số dư)
• Cập nhật người dẫn đầu realtime sau mỗi bid
Kết thúc phiên đấu giá
• Tự động đóng phiên khi hết thời gian (scheduler mỗi 10 giây)
• Xác định người thắng cuộc
• Quản lý trạng thái: OPEN → RUNNING → FINISHED → PAID / CANCELLED

Xử lý lỗi & ngoại lệ
• Từ chối bid thấp hơn giá hiện tại (InvalidBidException)
• Từ chối bid khi phiên đã đóng (AuctionTimeException)
• Xử lý lỗi kết nối mạng (AuctionConnectException)
• Trả về ERROR_RES có mã lỗi (ErrorCode) cho client

Giao diện người dùng (JavaFX + FXML)
• Màn hình đăng nhập / đăng ký
• Danh sách phiên đấu giá (lọc, tìm kiếm)
• Chi tiết sản phẩm và phiên đấu giá
• Màn hình đặt giá trực tiếp (realtime bidding)
• Dashboard Seller: quản lý sản phẩm, theo dõi phiên
• Dashboard Admin: quản lý người dùng, duyệt phiên

7.2 Kỹ thuật & Kiến trúc
• Kiến trúc Client–Server qua TCP Socket (cổng 1337), giao tiếp JSON
• Xử lý đồng thời an toàn: ReentrantReadWriteLock trong AuctionLogicManager, tránh race condition và lost update
• Realtime update qua Observer Pattern (AuctionEventPublisher → ClientAuctionObserver) + Socket event, không dùng polling
• MVC phía client: JavaFX Controller + ViewModel + FXML
• MVC phía server: Controller → Service → DAO → MySQL
• Design Patterns: Singleton, Factory Method, Observer, Strategy, DAO
• OOP đầy đủ: Encapsulation, Inheritance, Polymorphism, Abstraction
• Unit Tests: JUnit 5 + Mockito cho AuctionService, AuthService, AutoBidService, AuctionClosingService, ItemService, ConcurrentBidding, Observer
• CI/CD: GitHub Actions tự động build + test khi push / pull request

7.3 Chức năng nâng cao
• Auto-Bidding: Bidder đặt maxBid + increment, hệ thống tự động trả giá khi có bid mới, không vượt maxBid, ưu tiên theo thời điểm đăng ký
• Anti-sniping (Gia hạn phiên): Tự động gia hạn phiên nếu có bid trong X giây cuối
• Bid History Visualization: Biểu đồ đường giá realtime (line chart) 
8. Design Pattern áp dụng
Pattern	Vị trí áp dụng
Singleton	DatabaseConfig, AuctionLockManager, AuctionEventPublisher, SubscriptionRegistry, ServerConnection
Factory Method	ItemFactory — tạo Electronics, Art, Vehicle từ loại sản phẩm
Observer	AuctionEventPublisher + ClientAuctionObserver — realtime update giá đấu
Strategy	BidStrategy / AuctionLogicManager — xử lý bid logic linh hoạt
DAO	Tách biệt tầng data access khỏi business logic (Interface + JDBC impl)
9. Kiến trúc tổng quan
CLIENT (JavaFX)
  │  JavaFX UI ──► Controller ──► ClientMessageSender
  │                ServerEventListener ◄── (push events)
  │                ServerConnection (Singleton)
  │
  │  TCP Socket — port 1337 — JSON (MessageEnvelope)
  │
SERVER
  │  AuctionServer
  │    └── ClientConnectionHandler (1 thread/client, ThreadPool 50)
  │          └── RequestDispatcher (switch MessageType)
  │                ├── AuthController
  │                ├── ItemController
  │                └── AuctionService
  │                      ├── AuctionLogicManager (ReentrantReadWriteLock)
  │                      ├── AutoBidService (PriorityQueue)
  │                      └── AuctionClosingService (ScheduledExecutorService)
  │                            └── DAO Layer (JDBC + HikariCP)
  │                                  └── MySQL 8.0

9. Video Demo: https://drive.google.com/file/d/13Bo2rXCVYSap5Fpi-SIPP0ZHOYPUHA0V/view?usp=drive_link
10. File Báo Cáo: https://drive.google.com/file/d/1z-zTj73CZbEJNYNvK8vkk9HL5OMYJDv-/view?usp=drive_link