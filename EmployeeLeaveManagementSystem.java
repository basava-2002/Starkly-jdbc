import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class EmployeeLeaveManagementSystem {

    static final String URL = "jdbc:mysql://localhost:3306/employee_leave_db";
    static final String USER = "root";
    static final String PASSWORD = "root123";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            int choice;
            do{

                System.out.println("\n===== EMPLOYEE LEAVE MANAGEMENT SYSTEM =====");
                System.out.println("1. Employee Registration");
                System.out.println("2. Apply Leave");
                System.out.println("3. Approve/Reject Leave");
                System.out.println("4. Leave Balance");
                System.out.println("5. View Employees");
                System.out.println("6. View Leave Requests");
                System.out.println("7. Update Employee");
                System.out.println("8. Delete Employee");
                System.out.println("9. Exit");

                System.out.print("Enter your choice: ");
                choice = sc.nextInt();

                switch (choice) {
                    case 1:
                        registerEmployee();
                        break;
                    case 2:
                        applyLeave();
                        break;
                    case 3:
                        approveRejectLeave();
                        break;
                    case 4:
                        viewLeaveBalance();
                        break;
                    case 5:
                        viewEmployees();
                        break;
                    case 6:
                        viewLeaveRequests();
                        break;
                    case 7:
                        updateEmployee();
                        break;
                    case 8:
                        deleteEmployee();
                        break;
                    case 9:
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid choice.");
                }
            }while(choice!=9);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 1. EMPLOYEE REGISTRATION

    static void registerEmployee() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            sc.nextLine();

            System.out.print("Enter employee name: ");
            String name = sc.nextLine();

            System.out.print("Enter email: ");
            String email = sc.next();

            System.out.print("Enter department: ");
            String department = sc.next();

            String sql = "INSERT INTO employees(name, email, department) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, department);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            int employeeId = 0;

            if (rs.next()) {
                employeeId = rs.getInt(1);
            }

            // Create leave balance for employee
            String balanceSql = "INSERT INTO leave_balance (employee_id, total_leave, used_leave, remaining_leave) VALUES (?, 20, 0, 20)";

            PreparedStatement balancePs = con.prepareStatement(balanceSql);

            balancePs.setInt(1, employeeId);

            balancePs.executeUpdate();

            System.out.println("Employee registered successfully.");
            System.out.println("Employee ID: " + employeeId);
            System.out.println("Initial Leave Balance: 20 days");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 2. APPLY LEAVE

    static void applyLeave() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Employee ID: ");
            int employeeId = sc.nextInt();

            // Check leave balance
            String balanceSql = "SELECT remaining_leave FROM leave_balance WHERE employee_id = ?";

            PreparedStatement balancePs = con.prepareStatement(balanceSql);

            balancePs.setInt(1, employeeId);

            ResultSet balanceRs = balancePs.executeQuery();

            if (!balanceRs.next()) {
                System.out.println("Employee not found.");
                return;
            }

            int remainingLeave = balanceRs.getInt("remaining_leave");

            System.out.println("Available Leave: "+ remainingLeave);
            sc.nextLine();
            System.out.print("Enter Leave Type: ");
            String leaveType = sc.nextLine();

            System.out.print("Enter Start Date (YYYY-MM-DD): ");

            LocalDate startDate = LocalDate.parse(sc.nextLine());

            System.out.print("Enter End Date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(sc.nextLine());
            System.out.print("Enter Reason: ");
            String reason = sc.nextLine();

            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            if (days <= 0) {
                System.out.println("Invalid dates.");
                return;
            }

            if (days > remainingLeave) {
                System.out.println("Insufficient leave balance.");
                return;
            }

            String sql = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, days, reason, status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setString(2, leaveType);
            ps.setDate(3, Date.valueOf(startDate));
            ps.setDate(4, Date.valueOf(endDate));
            ps.setInt(5, (int) days);
            ps.setString(6, reason);

            ps.executeUpdate();

            System.out.println("Leave request submitted successfully.");
            System.out.println("Number of days: " + days);
            System.out.println("Status: PENDING");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 3. APPROVE / REJECT LEAVE

    static void approveRejectLeave() {

        Connection con = null;
        try {
            con = DriverManager.getConnection(URL,USER,PASSWORD);

            // Start transaction

            con.setAutoCommit(false);

            System.out.print("Enter Request ID: ");
            int requestId = sc.nextInt();

            System.out.println("1. Approve");
            System.out.println("2. Reject");

            System.out.print("Enter choice: ");
            int choice = sc.nextInt();

            String status;

            if (choice == 1) {
                status = "APPROVED";
            }
            else if (choice == 2) {
                status = "REJECTED";
            }
            else {
                System.out.println("Invalid choice.");
                return;
            }

            // Get request information

            String requestSql = "SELECT employee_id, days, status FROM leave_requests WHERE request_id = ?";

            PreparedStatement requestPs = con.prepareStatement(requestSql);

            requestPs.setInt(1, requestId);

            ResultSet rs = requestPs.executeQuery();

            if (!rs.next()) {

                System.out.println("Leave request not found.");
                return;
            }

            int employeeId = rs.getInt("employee_id");

            int days = rs.getInt("days");

            String oldStatus = rs.getString("status");

            if (!oldStatus.equals("PENDING")) {
                System.out.println("Request has already been processed.");
                return;
            }

            // Update leave request

            String updateRequest = "UPDATE leave_requests SET status = ? WHERE request_id = ?";

            PreparedStatement updatePs = con.prepareStatement(updateRequest);

            updatePs.setString(1, status);
            updatePs.setInt(2, requestId);

            updatePs.executeUpdate();

            // If approved, update leave balance

            if (status.equals("APPROVED")) {

                String balanceSql = "UPDATE leave_balance SET used_leave = used_leave + ?, remaining_leave = remaining_leave - ? WHERE employee_id = ? AND remaining_leave >= ?";

                PreparedStatement balancePs = con.prepareStatement(balanceSql);

                balancePs.setInt(1, days);
                balancePs.setInt(2, days);
                balancePs.setInt(3, employeeId);
                balancePs.setInt(4, days);

                int rows = balancePs.executeUpdate();
                if (rows == 0) {
                    System.out.println("Insufficient leave balance.");
                    con.rollback();
                    return;
                }
            }

            // Commit transaction

            con.commit();
            System.out.println("Leave request " + status);

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                    System.out.println(
                            "Transaction rolled back.");
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

    // 4. VIEW LEAVE BALANCE

    static void viewLeaveBalance() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Employee ID: ");
            int employeeId = sc.nextInt();

            String sql = "SELECT e.name, lb.total_leave, lb.used_leave, lb.remaining_leave FROM employees e JOIN leave_balance lb ON e.employee_id = lb.employee_id WHERE e.employee_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("\nEmployee Name: "+ rs.getString("name"));
                System.out.println("Total Leave: "+ rs.getInt("total_leave"));
                System.out.println("Used Leave: "+ rs.getInt("used_leave"));
                System.out.println("Remaining Leave: "+ rs.getInt("remaining_leave"));
            }
            else {
                System.out.println("Employee not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 5. VIEW EMPLOYEES

    static void viewEmployees() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "SELECT * FROM employees";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== EMPLOYEES =====");

            while (rs.next()) {

                System.out.println( "ID: "
                        + rs.getInt("employee_id")
                        + " | Name: "
                        + rs.getString("name")
                        + " | Email: "
                        + rs.getString("email")
                        + " | Department: "
                        + rs.getString("department"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 6. VIEW LEAVE REQUESTS

    static void viewLeaveRequests() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "SELECT lr.request_id, e.name, lr.leave_type, lr.start_date, lr.end_date, lr.days, lr.reason, lr.status FROM leave_requests lr JOIN employees e ON lr.employee_id = e.employee_id";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            System.out.println("\n===== LEAVE REQUESTS =====");

            while (rs.next()) {
                System.out.println("Request ID: "+ rs.getInt("request_id"));
                System.out.println("Employee: "+ rs.getString("name"));
                System.out.println("Leave Type: "+ rs.getString("leave_type"));
                System.out.println("From: "+ rs.getDate("start_date"));
                System.out.println("To: "+ rs.getDate("end_date"));
                System.out.println("Days: "+ rs.getInt("days"));
                System.out.println( "Reason: "+ rs.getString("reason"));
                System.out.println("Status: "+ rs.getString("status"));
                System.out.println("-------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 7. UPDATE EMPLOYEE

    static void updateEmployee() {

        try (Connection con =DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Employee ID: ");
            int employeeId = sc.nextInt();

            sc.nextLine();

            System.out.print("Enter new name: ");
            String name = sc.nextLine();

            System.out.print("Enter new email: ");
            String email = sc.next();

            System.out.print("Enter new department: ");
            String department = sc.next();

            String sql = "UPDATE employees SET name = ?, email = ?, department = ? WHERE employee_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, department);
            ps.setInt(4, employeeId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Employee updated successfully.");
            }
            else {
                System.out.println("Employee not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 8. DELETE EMPLOYEE
 
    static void deleteEmployee() {

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.print("Enter Employee ID: ");
            int employeeId = sc.nextInt();

            String sql = "DELETE FROM employees WHERE employee_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, employeeId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Employee deleted successfully.");
            }
            else {
                System.out.println("Employee not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}