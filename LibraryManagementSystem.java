import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class LibraryManagementSystem {

    static final String URL = "jdbc:mysql://localhost:3306/library_db";
    static final String USER = "root";
    static final String PASSWORD = "root123";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            int choice;
            do{
                System.out.println("\n===== LIBRARY MANAGEMENT SYSTEM =====");

                System.out.println("1. Add Book");
                System.out.println("2. Search Book");
                System.out.println("3. Add Member");
                System.out.println("4. Issue Book");
                System.out.println("5. Return Book");
                System.out.println("6. Calculate Fine");
                System.out.println("7. View Books");
                System.out.println("8. View Issue Details");
                System.out.println("9. Update Book");
                System.out.println("10. Delete Book");
                System.out.println("11. Exit");

                System.out.print("Enter your choice: ");
                choice = sc.nextInt();
                switch (choice) {
                    case 1:
                        addBook();
                        break;
                    case 2:
                        searchBook();
                        break;
                    case 3:
                        addMember();
                        break;
                    case 4:
                        issueBook();
                        break;
                    case 5:
                        returnBook();
                        break;
                    case 6:
                        calculateFine();
                        break;
                    case 7:
                        viewBooks();
                        break;
                    case 8:
                        viewIssueDetails();
                        break;
                    case 9:
                        updateBook();
                        break;
                    case 10:
                        deleteBook();
                        break;
                    case 11:
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid choice.");
                }
            }while(choice!=11);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 1. ADD BOOK

    static void addBook() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            sc.nextLine();
            System.out.print("Enter book title: ");
            String title = sc.nextLine();

            System.out.print("Enter author: ");
            String author = sc.nextLine();

            System.out.print("Enter quantity: ");
            int quantity = sc.nextInt();

            String sql = "INSERT INTO books (title, author, quantity, available_quantity) VALUES (?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, quantity);
            ps.setInt(4, quantity);

            ps.executeUpdate();
            System.out.println("Book added successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. SEARCH BOOK

    static void searchBook() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            sc.nextLine();

            System.out.print("Enter book title to search: ");
            String title = sc.nextLine();

            String sql = "SELECT * FROM books WHERE title LIKE ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, "%" + title + "%");

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println( "Book ID: " + rs.getInt("book_id"));

                System.out.println( "Title: " + rs.getString("title"));

                System.out.println( "Author: " + rs.getString("author"));

                System.out.println( "Quantity: " + rs.getInt("quantity"));

                System.out.println( "Available: " + rs.getInt("available_quantity"));

                System.out.println("-------------------");
            }

            if (!found) {
                System.out.println("Book not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. ADD MEMBER

    static void addMember() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            sc.nextLine();

            System.out.print("Enter member name: ");
            String name = sc.nextLine();

            System.out.print("Enter email: ");
            String email = sc.next();

            System.out.print("Enter mobile: ");
            String mobile = sc.next();

            String sql = "INSERT INTO members (name, email, mobile) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, mobile);

            ps.executeUpdate();

            System.out.println("Member added successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. ISSUE BOOK

    static void issueBook() {

        Connection con = null;

        try {
        	con = DriverManager.getConnection(URL,USER,PASSWORD);

            // Start transaction
            con.setAutoCommit(false);

            System.out.print("Enter Book ID: ");
            int bookId = sc.nextInt();

            System.out.print("Enter Member ID: ");
            int memberId = sc.nextInt();

            // Check book availability

            String checkSql ="SELECT available_quantity FROM books WHERE book_id = ?";

            PreparedStatement checkPs = con.prepareStatement(checkSql);

            checkPs.setInt(1, bookId);

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {

                System.out.println("Book not found.");

                con.rollback();
                return;
            }

            int available = rs.getInt("available_quantity");

            if (available <= 0) {

                System.out.println("Book is not available.");
                con.rollback();
                return;
            }

            // Insert issue record

            String issueSql = "INSERT INTO book_issues (book_id, member_id, issue_date, status) VALUES (?, ?, ?, 'ISSUED')";

            PreparedStatement issuePs = con.prepareStatement(issueSql);

            issuePs.setInt(1, bookId);
            issuePs.setInt(2, memberId);
            issuePs.setDate(3,Date.valueOf(LocalDate.now()));

            issuePs.executeUpdate();

            // Decrease available quantity

            String updateSql = "UPDATE books SET available_quantity = available_quantity - 1 WHERE book_id = ?";

            PreparedStatement updatePs = con.prepareStatement(updateSql);

            updatePs.setInt(1, bookId);

            updatePs.executeUpdate();

            // Commit transaction

            con.commit();

            System.out.println("Book issued successfully.");

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

    // 5. RETURN BOOK

    static void returnBook() {

        Connection con = null;

        try {
            con = DriverManager.getConnection(URL,USER,PASSWORD);

            // Start transaction

            con.setAutoCommit(false);

            System.out.print("Enter Issue ID: ");
            int issueId = sc.nextInt();

            // Get issue details

            String sql = "SELECT book_id, issue_date, status FROM book_issues WHERE issue_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, issueId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {

                System.out.println("Issue record not found.");
                con.rollback();
                return;
            }

            int bookId = rs.getInt("book_id");

            Date issueDate = rs.getDate("issue_date");

            String status = rs.getString("status");

            if (!status.equals("ISSUED")) {

                System.out.println("Book is already returned.");
                con.rollback();
                return;
            }

            LocalDate issueDateLocal = issueDate.toLocalDate();

            LocalDate returnDate = LocalDate.now();

            long days = ChronoUnit.DAYS.between(issueDateLocal,returnDate);

            // Free period = 14 days

            long lateDays = days - 14;

            double fine = 0;

            if (lateDays > 0) {

                fine = lateDays * 5;
            }

            // Update issue record

            String updateIssue = "UPDATE book_issues SET return_date = ?, fine = ?, status = 'RETURNED' WHERE issue_id = ?";

            PreparedStatement updatePs = con.prepareStatement(updateIssue);

            updatePs.setDate(1,Date.valueOf(returnDate));

            updatePs.setDouble(2, fine);
            updatePs.setInt(3, issueId);

            updatePs.executeUpdate();

            // Increase available quantity

            String updateBook = "UPDATE books SET available_quantity = available_quantity + 1 WHERE book_id = ?";

            PreparedStatement bookPs = con.prepareStatement(updateBook);

            bookPs.setInt(1, bookId);

            bookPs.executeUpdate();

            // Commit

            con.commit();

            System.out.println("Book returned successfully.");

            System.out.println("Fine: ₹" + fine);

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

    // 6. CALCULATE FINE

    static void calculateFine() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Issue ID: ");
            int issueId = sc.nextInt();

            String sql = "SELECT issue_date, return_date FROM book_issues WHERE issue_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, issueId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {

                System.out.println("Issue record not found.");

                return;
            }

            LocalDate issueDate = rs.getDate("issue_date").toLocalDate();

            Date returnDateSql = rs.getDate("return_date");

            LocalDate endDate;

            if (returnDateSql == null) {

                endDate = LocalDate.now();

            } else {

                endDate = returnDateSql.toLocalDate();
            }

            long totalDays = ChronoUnit.DAYS.between( issueDate, endDate);

            long lateDays = totalDays - 14;

            double fine = 0;

            if (lateDays > 0) {

                fine = lateDays * 5;
            }

            System.out.println("Total Days: " + totalDays);

            System.out.println("Late Days: " + Math.max(lateDays, 0));

            System.out.println("Fine: ₹" + fine);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 7. VIEW BOOKS

    static void viewBooks() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "SELECT * FROM books";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== BOOKS =====");

            while (rs.next()) {

                System.out.println( "ID: " + rs.getInt("book_id") + " | Title: " + rs.getString("title")+ " | Author: "+ rs.getString("author") + " | Quantity: " + rs.getInt("quantity") + " | Available: " + rs.getInt("available_quantity"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 8. VIEW ISSUE DETAILS - JOIN

    static void viewIssueDetails() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "SELECT bi.issue_id, b.title, m.name, bi.issue_date, bi.return_date, bi.fine, bi.status FROM book_issues bi JOIN books b ON bi.book_id = b.book_id JOIN members m  ON bi.member_id = m.member_id";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== ISSUE DETAILS =====");

            while (rs.next()) {

                System.out.println("Issue ID: "  + rs.getInt("issue_id"));

                System.out.println("Book: " + rs.getString("title"));

                System.out.println("Member: " + rs.getString("name"));

                System.out.println("Issue Date: " + rs.getDate("issue_date"));

                System.out.println("Return Date: " + rs.getDate("return_date"));

                System.out.println("Fine: ₹" + rs.getDouble("fine"));

                System.out.println("Status: " + rs.getString("status"));

                System.out.println("-----------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 9. UPDATE BOOK

    static void updateBook() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Book ID: ");
            int bookId = sc.nextInt();

            sc.nextLine();

            System.out.print("Enter new title: ");
            String title = sc.nextLine();

            System.out.print("Enter new author: ");
            String author = sc.nextLine();

            System.out.print("Enter new quantity: ");
            int quantity = sc.nextInt();

            String sql = "UPDATE books SET title = ?, author = ?, quantity = ?, available_quantity = ? WHERE book_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, quantity);
            ps.setInt(4, quantity);
            ps.setInt(5, bookId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                System.out.println("Book updated successfully.");

            } else {

                System.out.println("Book not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 10. DELETE BOOK

    static void deleteBook() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Book ID: ");
            int bookId = sc.nextInt();

            String sql = "DELETE FROM books WHERE book_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, bookId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                System.out.println("Book deleted successfully.");

            } else {

                System.out.println("Book not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}