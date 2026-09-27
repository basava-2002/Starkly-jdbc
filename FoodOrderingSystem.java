import java.sql.*;
import java.util.*;

public class FoodOrderingSystem {

    static final String url = "jdbc:mysql://localhost:3306/restaurant_db";
    static final String user = "root";
    static final String password = "root123";
    static Connection con;

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            int ch;
            con = DriverManager.getConnection(url,user,password);
            do{

                System.out.println("\n===== RESTAURANT FOOD ORDERING SYSTEM =====");
                System.out.println("1. Customer Registration");
                System.out.println("2. View Menu");
                System.out.println("3. Place Order");
                System.out.println("4. Order History");
                System.out.println("5. Generate Bill");
                System.out.println("6. Exit");

                System.out.print("Enter your choice: ");
                ch = sc.nextInt();

                switch (ch) {

                    case 1:
                        registerCustomer();
                        break;
                    case 2:
                        viewMenu();
                        break;
                    case 3:
                        placeOrder();
                        break;
                    case 4:
                        orderHistory();
                        break;
                    case 5:
                        generateBill();
                        break;
                    case 6:
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid choice");
                }
            }while(ch!=6);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 1. Customer Registration
    static void registerCustomer() {

        try (Connection con = DriverManager.getConnection(url, user, password)) {

            System.out.print("Enter customer name: ");
            sc.nextLine();
            String name = sc.nextLine();

            System.out.print("Enter mobile: ");
            String mobile = sc.next();

            System.out.print("Enter email: ");
            String email = sc.next();

            String sql = "INSERT INTO customers(name, mobile, email) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, mobile);
            ps.setString(3, email);

            ps.executeUpdate();

            System.out.println("Customer registered successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. View Menu
    static void viewMenu() {

        try (Connection con = DriverManager.getConnection(url, user, password)) {

            String sql = "SELECT * FROM menu WHERE available = true";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== MENU =====");

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("item_id") + " | " + rs.getString("item_name") + " | ₹" + rs.getDouble("price")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. Place Order
    static void placeOrder() {

        Connection con = null;

        try {

            con = DriverManager.getConnection(url, user, password);

            // Start transaction
            con.setAutoCommit(false);

            System.out.print("Enter Customer ID: ");
            int customerId = sc.nextInt();

            // Create master order
            String orderSql = "INSERT INTO orders(customer_id, total_amount) VALUES (?, ?)";

            PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);

            orderPs.setInt(1, customerId);
            orderPs.setDouble(2, 0);

            orderPs.executeUpdate();

            ResultSet generatedKeys = orderPs.getGeneratedKeys();

            int orderId = 0;

            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            }

            double total = 0;

            while (true) {

                viewMenu();

                System.out.print("\nEnter Menu Item ID (Enter 0 to finish order): ");
                int itemId = sc.nextInt();

                if (itemId == 0) {
                    break;
                }

                System.out.print("Enter quantity: ");
                int quantity = sc.nextInt();

                // Get price from menu
                String priceSql = "SELECT price FROM menu WHERE item_id = ? AND available = true";

                PreparedStatement pricePs = con.prepareStatement(priceSql);

                pricePs.setInt(1, itemId);

                ResultSet rs = pricePs.executeQuery();

                if (rs.next()) {

                    double price = rs.getDouble("price");

                    double itemTotal = price * quantity;

                    total = total + itemTotal;

                    // Insert detail record
                    String itemSql = "INSERT INTO order_items(order_id, item_id, quantity, price) VALUES (?, ?, ?, ?)";

                    PreparedStatement itemPs = con.prepareStatement(itemSql);

                    itemPs.setInt(1, orderId);
                    itemPs.setInt(2, itemId);
                    itemPs.setInt(3, quantity);
                    itemPs.setDouble(4, price);

                    itemPs.executeUpdate();

                    System.out.println("Item added.");
                }
                else {
                    System.out.println("Invalid menu item.");
                }
            }

            // Update master order total
            String updateSql = "UPDATE orders SET total_amount = ? WHERE order_id = ?";

            PreparedStatement updatePs = con.prepareStatement(updateSql);

            updatePs.setDouble(1, total);
            updatePs.setInt(2, orderId);

            updatePs.executeUpdate();

            // Commit transaction
            con.commit();

            System.out.println("\nOrder placed successfully!");
            System.out.println("Order ID: " + orderId);
            System.out.println("Total Amount: ₹" + total);

        } catch (Exception e) {

            try {

                if (con != null) {
                    con.rollback();
                    System.out.println("Transaction rolled back.");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();

        } finally {

            try {

                if (con != null) {
                    con.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 4. Order History
    static void orderHistory() {

        try (Connection con = DriverManager.getConnection(url, user, password)) {

            System.out.print("Enter Customer ID: ");
            int customerId = sc.nextInt();

            String sql = "SELECT order_id, order_date, total_amount FROM orders WHERE customer_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== ORDER HISTORY =====");

            while (rs.next()) {

                System.out.println("Order ID: " + rs.getInt("order_id") + " | Date: " + rs.getTimestamp("order_date") + " | Total: ₹" + rs.getDouble("total_amount"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 5. Generate Bill
    static void generateBill() {

        try (Connection con = DriverManager.getConnection(url, user, password)) {

            System.out.print("Enter Order ID: ");
            int orderId = sc.nextInt();

            String sql = "SELECT m.item_name, oi.quantity, oi.price, (oi.quantity * oi.price) AS item_total FROM order_items oi JOIN menu m ON oi.item_id = m.item_id WHERE oi.order_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            System.out.println("\n========== BILL ==========");

            double total = 0;

            while (rs.next()) {

                String itemName = rs.getString("item_name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double itemTotal = rs.getDouble("item_total");

                System.out.println(itemName + " | Qty: " + quantity + " | Price: ₹" + price + " | Total: ₹" + itemTotal);
                total = total + itemTotal;
            }
            System.out.println("--------------------------");
            System.out.println("Grand Total: ₹" + total);
            System.out.println("==========================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}