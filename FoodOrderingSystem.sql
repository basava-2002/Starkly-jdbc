CREATE DATABASE restaurant_db;
USE restaurant_db;

CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    mobile VARCHAR(15),
    email VARCHAR(100)
);

CREATE TABLE menu (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(100),
    price DOUBLE,
    available BOOLEAN
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DOUBLE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE TABLE order_items (
    order_item_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT,
    item_id INT,
    quantity INT,
    price DOUBLE,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (item_id) REFERENCES menu(item_id)
);

INSERT INTO menu(item_name, price, available)
VALUES
('Pizza', 250, true),
('Burger', 150, true),
('French Fries', 100, true),
('Coke', 60, true),
('Biryani', 220, true);