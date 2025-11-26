# Development Guide

## 📋 Table of Contents
1. [Development Environment Setup](#development-environment-setup)
2. [Project Structure](#project-structure)
3. [Development Workflow](#development-workflow)
4. [Coding Standards](#coding-standards)
5. [Testing](#testing)
6. [Debugging](#debugging)

## Development Environment Setup

### Prerequisites
- **JDK 17+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/)

### IDE Setup

#### IntelliJ IDEA (Recommended for Backend)
1. **Install IntelliJ IDEA** (Community or Ultimate)
2. **Import Project**:
   - File → Open → Select project root
   - Choose "Maven" as project type
3. **Install Plugins**:
   - Lombok Plugin
   - Spring Boot Assistant
   - Docker
4. **Configure JDK**:
   - File → Project Structure → Project SDK → Add JDK 17

#### VS Code (Recommended for Frontend)
1. **Install VS Code**
2. **Install Extensions**:
   - ESLint
   - Prettier
   - Tailwind CSS IntelliSense
   - ES7+ React/Redux/React-Native snippets
   - GitLens
3. **Configure Settings** (`.vscode/settings.json`):
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "tailwindCSS.experimental.classRegex": [
    ["clsx\\(([^)]*)\\)", "(?:'|\"|`)([^']*)(?:'|\"|`)"]
  ]
}
```

### Local Development Infrastructure

#### Start Required Services
```bash
# Start MySQL
docker run -d --name mysql-dev \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=userservice \
  mysql:8.0

# Create databases
docker exec -i mysql-dev mysql -uroot -p1234 <<EOF
CREATE DATABASE IF NOT EXISTS productmicroservice;
CREATE DATABASE IF NOT EXISTS orderservice;
CREATE DATABASE IF NOT EXISTS paymentservice;
EOF

# Start RabbitMQ
docker run -d --name rabbitmq-dev \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.13-management
```

## Project Structure

### Monorepo Layout
```
FastfoodDelivery-microservice-monorepo/
├── docs/                           # Documentation
│   ├── PRD.md                      # Product Requirements Document
│   ├── API.md                      # API Documentation
│   ├── DEPLOYMENT.md               # Deployment Guide
│   └── DEVELOPMENT.md              # This file
├── services/                       # Backend microservices
│   ├── registry-service/           # Eureka Server
│   ├── gateway-service/            # API Gateway
│   ├── user-microservice/          # User Service
│   ├── product-microservice/       # Product Service
│   ├── order-microservice/         # Order Service
│   └── payment-microservice/       # Payment Service
├── frontend/                       # React Frontend
├── mysql-init/                     # MySQL initialization scripts
├── docker-compose.yml              # Docker orchestration
└── README.md                       # Main README
```

### Microservice Structure (Clean Architecture)
```
service-name/
├── src/
│   └── main/
│       └── java/com/example/service/
│           ├── application/        # Application Layer
│           │   ├── dto/            # Data Transfer Objects
│           │   ├── usecase/        # Use Cases (Business Logic)
│           │   └── validation/     # Validation Logic
│           ├── domain/             # Domain Layer
│           │   ├── entity/         # Domain Entities
│           │   ├── repository/     # Repository Interfaces
│           │   └── service/        # Domain Services
│           ├── infrastructure/     # Infrastructure Layer
│           │   ├── config/         # Configuration Classes
│           │   ├── messaging/      # RabbitMQ Producers/Consumers
│           │   ├── persistence/    # JPA Repositories
│           │   ├── security/       # Security Configuration
│           │   └── service/        # External Service Adapters
│           └── presentation/       # Presentation Layer
│               ├── controller/     # REST Controllers
│               ├── exception/      # Exception Handlers
│               └── mapper/         # DTO Mappers
└── resources/
    ├── application.properties      # Configuration
    └── db/migration/               # Flyway migrations (optional)
```

### Frontend Structure
```
frontend/
├── public/                         # Static assets
├── src/
│   ├── components/                 # React Components
│   │   ├── admin/                  # Admin-specific components
│   │   ├── merchant/               # Merchant-specific components
│   │   └── common/                 # Shared components
│   ├── pages/                      # Page components
│   │   ├── admin/                  # Admin pages
│   │   ├── merchant/               # Merchant pages
│   │   └── auth/                   # Auth pages
│   ├── context/                    # React Context
│   ├── services/                   # API Service Layer
│   │   └── apiService.js           # Axios configuration
│   ├── App.jsx                     # Main App component
│   ├── main.jsx                    # Entry point
│   └── index.css                   # Global styles
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## Development Workflow

### Starting Development

#### 1. Clone and Setup
```bash
git clone <repository-url>
cd FastfoodDelivery-microservice-monorepo

# Start infrastructure
docker run -d --name mysql-dev -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 mysql:8.0
docker run -d --name rabbitmq-dev -p 5672:5672 -p 15672:15672 \
  rabbitmq:3.13-management
```

#### 2. Start Backend Services (in order)
```bash
# Terminal 1: Registry Service
cd services/registry-service
mvn clean install
mvn spring-boot:run

# Terminal 2: Gateway Service (wait for Eureka to start)
cd services/gateway-service
mvn clean install
mvn spring-boot:run

# Terminal 3-6: Microservices (can start in parallel)
cd services/user-microservice && mvn spring-boot:run &
cd services/product-microservice && mvn spring-boot:run &
cd services/order-microservice && mvn spring-boot:run &
cd services/payment-microservice && mvn spring-boot:run &
```

#### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### Feature Development Workflow

#### 1. Create Feature Branch
```bash
git checkout develop
git pull origin develop
git checkout -b feature/add-product-reviews
```

#### 2. Implement Feature (Example: Add Product Reviews)

**Backend (Product Service)**

a. **Create Domain Entity**
```java
// domain/entity/ProductReview.java
@Entity
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;
    private Long userId;
    private Integer rating;
    private String comment;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

b. **Create Repository**
```java
// domain/repository/ProductReviewRepository.java
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductId(Long productId);
    Double findAverageRatingByProductId(Long productId);
}
```

c. **Create Use Case**
```java
// application/usecase/CreateProductReviewUseCase.java
@Service
@RequiredArgsConstructor
public class CreateProductReviewUseCase {
    private final ProductReviewRepository reviewRepository;
    
    public ProductReview execute(CreateReviewRequest request, Long userId) {
        // Validation logic
        // Create review
        // Publish event if needed
        return reviewRepository.save(review);
    }
}
```

d. **Create Controller**
```java
// presentation/controller/ProductReviewController.java
@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ProductReviewController {
    
    @PostMapping
    public ResponseEntity<ProductReview> createReview(
        @PathVariable Long productId,
        @RequestBody CreateReviewRequest request,
        @AuthenticationPrincipal Long userId) {
        
        return ResponseEntity.ok(createReviewUseCase.execute(request, userId));
    }
}
```

**Frontend**

a. **Create API Service**
```javascript
// services/reviewService.js
export const createReview = async (productId, reviewData) => {
  const response = await apiClient.post(
    `/products/${productId}/reviews`,
    reviewData
  );
  return response.data;
};
```

b. **Create Component**
```jsx
// components/merchant/ProductReviews.jsx
export const ProductReviews = ({ productId }) => {
  const [reviews, setReviews] = useState([]);
  
  useEffect(() => {
    fetchReviews(productId);
  }, [productId]);
  
  return (
    <div className="reviews-container">
      {reviews.map(review => (
        <ReviewCard key={review.id} review={review} />
      ))}
    </div>
  );
};
```

#### 3. Testing

**Backend Unit Test**
```java
@SpringBootTest
class CreateProductReviewUseCaseTest {
    
    @Test
    void shouldCreateReviewSuccessfully() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Great product!");
        
        // Act
        ProductReview review = useCase.execute(request, 1L);
        
        // Assert
        assertNotNull(review.getId());
        assertEquals(5, review.getRating());
    }
}
```

**Frontend Test** (Optional)
```javascript
// __tests__/ProductReviews.test.jsx
import { render, screen } from '@testing-library/react';
import { ProductReviews } from '../components/merchant/ProductReviews';

test('renders product reviews', async () => {
  render(<ProductReviews productId={1} />);
  const reviewElement = await screen.findByText(/Great product!/i);
  expect(reviewElement).toBeInTheDocument();
});
```

#### 4. Commit and Push
```bash
git add .
git commit -m "feat: Add product review feature"
git push origin feature/add-product-reviews
```

#### 5. Create Pull Request
- Open PR from `feature/add-product-reviews` to `develop`
- Add description and screenshots
- Request code review

## Coding Standards

### Java (Backend)

#### Naming Conventions
- **Classes**: PascalCase (`UserService`, `OrderController`)
- **Methods**: camelCase (`createOrder`, `validateUser`)
- **Variables**: camelCase (`userId`, `orderTotal`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`)

#### Code Style
```java
// Use Lombok for boilerplate reduction
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
}

// Use meaningful variable names
public Order createOrder(CreateOrderRequest request, Long userId) {
    // Bad
    Order o = new Order();
    
    // Good
    Order order = Order.builder()
        .userId(userId)
        .status(OrderStatus.PENDING)
        .build();
    
    return orderRepository.save(order);
}

// Use Optional for nullable returns
public Optional<User> findUserById(Long id) {
    return userRepository.findById(id);
}

// Handle exceptions gracefully
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    ErrorResponse error = ErrorResponse.builder()
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

### JavaScript/React (Frontend)

#### Naming Conventions
- **Components**: PascalCase (`ProductCard`, `OrderList`)
- **Functions**: camelCase (`fetchOrders`, `handleSubmit`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`)

#### Code Style
```javascript
// Use functional components with hooks
const ProductCard = ({ product }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  
  const handleToggle = () => {
    setIsExpanded(!isExpanded);
  };
  
  return (
    <div className="product-card">
      <h3>{product.name}</h3>
      <p className="price">${product.price}</p>
      {isExpanded && <p>{product.description}</p>}
      <button onClick={handleToggle}>
        {isExpanded ? 'Show Less' : 'Show More'}
      </button>
    </div>
  );
};

// Use destructuring
const { username, email, role } = user;

// Use async/await for API calls
const fetchProducts = async () => {
  try {
    const response = await apiService.getProducts();
    setProducts(response.data);
  } catch (error) {
    console.error('Failed to fetch products:', error);
    setError(error.message);
  }
};

// Use Tailwind CSS utilities
<button className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
  Submit
</button>
```

## Testing

### Backend Testing

#### Unit Tests
```java
@SpringBootTest
class OrderServiceTest {
    
    @MockBean
    private OrderRepository orderRepository;
    
    @Autowired
    private CreateOrderUseCase createOrderUseCase;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        when(orderRepository.save(any())).thenReturn(mockOrder);
        
        // Act
        Order result = createOrderUseCase.execute(request, 1L);
        
        // Assert
        assertNotNull(result);
        verify(orderRepository).save(any());
    }
}
```

#### Integration Tests
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldCreateOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

#### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Testing

```javascript
// Using React Testing Library
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductCard } from './ProductCard';

test('renders product name', () => {
  const product = { id: 1, name: 'Burger', price: 9.99 };
  render(<ProductCard product={product} />);
  expect(screen.getByText('Burger')).toBeInTheDocument();
});

test('toggles description on click', () => {
  const product = { 
    id: 1, 
    name: 'Burger', 
    price: 9.99,
    description: 'Delicious burger'
  };
  render(<ProductCard product={product} />);
  
  const button = screen.getByText('Show More');
  fireEvent.click(button);
  
  expect(screen.getByText('Delicious burger')).toBeInTheDocument();
});
```

## Debugging

### Backend Debugging

#### IntelliJ IDEA
1. Set breakpoints by clicking left of line number
2. Run → Debug 'ServiceNameApplication'
3. Use debug console to inspect variables

#### Remote Debugging (Docker)
```yaml
# docker-compose.yml
services:
  order-service:
    environment:
      JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "5005:5005"
```

Connect IntelliJ:
- Run → Edit Configurations → Remote JVM Debug
- Host: localhost, Port: 5005

#### Logging
```java
@Slf4j
public class OrderService {
    public Order createOrder(CreateOrderRequest request) {
        log.debug("Creating order for user: {}", request.getUserId());
        log.info("Order created successfully: {}", order.getId());
        log.error("Failed to create order", exception);
    }
}
```

### Frontend Debugging

#### Browser DevTools
- **Chrome DevTools**: F12 or Ctrl+Shift+I
- **React DevTools**: Install extension
- **Network Tab**: Monitor API calls
- **Console**: Check logs and errors

#### VS Code Debugging
```json
// .vscode/launch.json
{
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Chrome",
      "url": "http://localhost:5173",
      "webRoot": "${workspaceFolder}/frontend/src"
    }
  ]
}
```

#### Console Logging
```javascript
console.log('User data:', user);
console.error('API Error:', error);
console.table(products);
```

## Useful Commands

### Maven Commands
```bash
mvn clean                    # Clean build directory
mvn compile                  # Compile source code
mvn test                     # Run tests
mvn package                  # Build JAR
mvn spring-boot:run         # Run application
mvn dependency:tree         # View dependencies
```

### NPM Commands
```bash
npm install                  # Install dependencies
npm run dev                  # Start dev server
npm run build               # Build for production
npm run lint                # Run ESLint
npm run preview             # Preview production build
```

### Docker Commands
```bash
docker-compose up -d        # Start all services
docker-compose logs -f      # View logs
docker-compose restart      # Restart services
docker-compose down         # Stop and remove containers
docker system prune -a      # Clean up Docker
```

---

**Happy Coding! 🚀**
