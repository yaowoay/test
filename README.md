# Spring Boot Demo Project

这是一个标准的Spring Boot项目示例，展示了如何构建一个完整的RESTful API应用。

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── demo/
│   │               ├── SpringbootDemoApplication.java    # 主应用类
│   │               ├── controller/                       # 控制器层
│   │               │   └── UserController.java
│   │               ├── service/                         # 服务层
│   │               │   ├── UserService.java            # 服务接口
│   │               │   └── impl/
│   │               │       └── UserServiceImpl.java    # 服务实现
│   │               ├── repository/                      # 数据访问层
│   │               │   └── UserRepository.java
│   │               ├── entity/                         # 实体类
│   │               │   └── User.java
│   │               ├── dto/                            # 数据传输对象
│   │               │   ├── UserDTO.java
│   │               │   └── CreateUserRequest.java
│   │               ├── exception/                      # 异常处理
│   │               │   ├── ResourceNotFoundException.java
│   │               │   ├── DuplicateResourceException.java
│   │               │   └── GlobalExceptionHandler.java
│   │               └── config/                         # 配置类
│   └── resources/
│       ├── application.yml                             # 应用配置
│       └── static/                                     # 静态资源
└── test/
    └── java/
        └── com/
            └── example/
                └── demo/
                    ├── SpringbootDemoApplicationTests.java
                    └── controller/
                        └── UserControllerTest.java
```

## 技术栈

- **Spring Boot 3.2.0** - 主框架
- **Spring Data JPA** - 数据访问
- **Spring Web** - Web层
- **H2 Database** - 内存数据库（开发环境）
- **MySQL** - 生产数据库
- **Lombok** - 减少样板代码
- **JUnit 5** - 单元测试
- **Maven** - 构建工具

## 功能特性

### 用户管理API
- ✅ 创建用户
- ✅ 根据ID查询用户
- ✅ 根据用户名查询用户
- ✅ 分页查询所有用户
- ✅ 查询活跃用户
- ✅ 更新用户信息
- ✅ 删除用户
- ✅ 激活/停用用户
- ✅ 用户名搜索

### 技术特性
- ✅ 参数验证
- ✅ 全局异常处理
- ✅ 分页和排序
- ✅ 事务管理
- ✅ 单元测试
- ✅ 日志记录

## 快速开始

### 1. 克隆项目
```bash
git clone <repository-url>
cd springboot-demo
```

### 2. 运行项目
```bash
mvn spring-boot:run
```

### 3. 访问应用
- 应用地址: http://localhost:8080
- H2控制台: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - 用户名: `sa`
  - 密码: `password`

## API文档

### 用户管理接口

#### 创建用户
```http
POST /api/users
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "fullName": "Test User",
  "phone": "1234567890"
}
```

#### 获取用户
```http
GET /api/users/{id}
GET /api/users/username/{username}
```

#### 获取所有用户（分页）
```http
GET /api/users?page=0&size=10&sortBy=id&sortDir=asc
```

#### 获取活跃用户
```http
GET /api/users/active
```

#### 更新用户
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "updateduser",
  "email": "updated@example.com",
  "fullName": "Updated User",
  "phone": "0987654321",
  "active": true
}
```

#### 删除用户
```http
DELETE /api/users/{id}
```

#### 激活/停用用户
```http
PATCH /api/users/{id}/status?active=true
```

#### 搜索用户
```http
GET /api/users/search?username=test
```

## 测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试
```bash
mvn test -Dtest=UserControllerTest
```

## 配置

### 开发环境
项目默认使用H2内存数据库，无需额外配置。

### 生产环境
1. 修改 `application.yml` 中的数据库配置
2. 取消MySQL配置的注释
3. 注释掉H2相关配置

## 部署

### 构建JAR包
```bash
mvn clean package
```

### 运行JAR包
```bash
java -jar target/springboot-demo-1.0.0.jar
```

## 贡献

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
