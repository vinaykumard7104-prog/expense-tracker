CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  type VARCHAR(10) NOT NULL,
  user_id BIGINT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  amount DECIMAL(12,2) NOT NULL,
  type VARCHAR(10) NOT NULL,
  description VARCHAR(255),
  txn_date DATE NOT NULL,
  category_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  FOREIGN KEY (category_id) REFERENCES categories(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_txn_user_date (user_id, txn_date)
);
CREATE TABLE budgets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  monthly_limit DECIMAL(12,2) NOT NULL,
  category_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  UNIQUE (user_id, category_id),
  FOREIGN KEY (category_id) REFERENCES categories(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
