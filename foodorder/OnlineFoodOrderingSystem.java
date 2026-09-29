package com.foodorder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OnlineFoodOrderingSystem {

    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    private static final String DATABASE = "food_ordering";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        try {
            initDatabase();
        } catch (SQLException e) {
            System.out.println("Unable to initialise database: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\n========== ONLINE FOOD ORDERING SYSTEM ==========");
            System.out.println("1. Customer Registration");
            System.out.println("2. View Menu");
            System.out.println("3. Place Order");
            System.out.println("4. Order History");
            System.out.println("5. Generate Bill");
            System.out.println("6. Exit");
            System.out.print("Enter Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    registerCustomer();
                    break;
                case "2":
                    viewMenu();
                    break;
                case "3":
                    placeOrder();
                    break;
                case "4":
                    orderHistory();
                    break;
                case "5":
                    generateBill();
                    break;
                case "6":
                    System.out.println("Thank you for using the system.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(SERVER_URL + DATABASE, USER, PASSWORD);
    }

    private static void initDatabase() throws SQLException {

        try (Connection con = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
        }

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate("CREATE TABLE IF NOT EXISTS customers ("
                    + "customer_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "phone VARCHAR(15) UNIQUE NOT NULL, "
                    + "email VARCHAR(100) NOT NULL, "
                    + "address VARCHAR(255) NOT NULL) ENGINE=InnoDB");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS menu ("
                    + "menu_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "item_name VARCHAR(100) NOT NULL, "
                    + "category VARCHAR(50) NOT NULL, "
                    + "price DECIMAL(10,2) NOT NULL) ENGINE=InnoDB");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                    + "order_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "order_number VARCHAR(30) UNIQUE NOT NULL, "
                    + "customer_id INT NOT NULL, "
                    + "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "total_amount DECIMAL(12,2) NOT NULL DEFAULT 0, "
                    + "FOREIGN KEY (customer_id) REFERENCES customers(customer_id)) ENGINE=InnoDB");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS order_items ("
                    + "order_item_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "order_id INT NOT NULL, "
                    + "menu_id INT NOT NULL, "
                    + "quantity INT NOT NULL, "
                    + "unit_price DECIMAL(10,2) NOT NULL, "
                    + "item_total DECIMAL(12,2) NOT NULL, "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (menu_id) REFERENCES menu(menu_id)) ENGINE=InnoDB");

            seedMenu(con);
        }
    }

    private static void seedMenu(Connection con) throws SQLException {

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM menu")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }

        String[][] items = {
                { "Margherita Pizza", "Pizza", "250.00" },
                { "Veg Burger", "Burger", "120.00" },
                { "Chicken Biryani", "Rice", "220.00" },
                { "Paneer Butter Masala", "Curry", "180.00" },
                { "French Fries", "Snacks", "90.00" },
                { "Cold Coffee", "Beverage", "80.00" }
        };

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO menu(item_name, category, price) VALUES (?, ?, ?)")) {

            for (String[] item : items) {
                ps.setString(1, item[0]);
                ps.setString(2, item[1]);
                ps.setDouble(3, Double.parseDouble(item[2]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void registerCustomer() {

        System.out.println("\n========== CUSTOMER REGISTRATION ==========");

        String name = readText("Name    : ");
        String phone = readText("Phone   : ");
        String email = readText("Email   : ");
        String address = readText("Address : ");

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            System.out.println("Phone must contain exactly 10 digits.");
            return;
        }

        String sql = "INSERT INTO customers(name, phone, email, address) VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, address);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Customer registered successfully. Customer ID: " + keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("Phone number already registered.");
            } else {
                System.out.println("Registration failed: " + e.getMessage());
            }
        }
    }

    private static void viewMenu() {

        System.out.println("\n========== MENU ==========");
        System.out.printf("%-5s %-25s %-15s %10s%n", "ID", "Item", "Category", "Price");
        System.out.println("-".repeat(58));

        String sql = "SELECT menu_id, item_name, category, price FROM menu ORDER BY menu_id";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                System.out.printf("%-5d %-25s %-15s %10.2f%n",
                        rs.getInt("menu_id"),
                        rs.getString("item_name"),
                        rs.getString("category"),
                        rs.getDouble("price"));
            }

        } catch (SQLException e) {
            System.out.println("Unable to load menu: " + e.getMessage());
        }
    }

    private static void placeOrder() {

        System.out.println("\n========== PLACE ORDER ==========");

        int customerId = readInt("Customer ID: ");

        if (!customerExists(customerId)) {
            System.out.println("Customer not found.");
            return;
        }

        viewMenu();

        List<int[]> cart = new ArrayList<>();       
        List<Double> prices = new ArrayList<>();    

        do {
            int menuId = readInt("Food ID  : ");
            int quantity = readInt("Quantity : ");

            if (quantity <= 0) {
                System.out.println("Quantity must be greater than 0.");
                continue;
            }

            double price = getPrice(menuId);

            if (price < 0) {
                System.out.println("Food item not found.");
                continue;
            }

            cart.add(new int[] { menuId, quantity });
            prices.add(price);

        } while (readYesNo("Add another item? (Y/N): "));

        if (cart.isEmpty()) {
            System.out.println("No items selected.");
            return;
        }

        saveOrder(customerId, cart, prices);
    }

    private static void saveOrder(int customerId, List<int[]> cart, List<Double> prices) {

        String orderNumber = "ORD" + System.currentTimeMillis();

        String orderSql = "INSERT INTO orders(order_number, customer_id, total_amount) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO order_items(order_id, menu_id, quantity, unit_price, item_total) "
                + "VALUES (?, ?, ?, ?, ?)";

        double total = 0;
        for (int i = 0; i < cart.size(); i++) {
            total += cart.get(i)[1] * prices.get(i);
        }

        try (Connection con = getConnection()) {

            con.setAutoCommit(false);

            try {
                int orderId;

                // Master record
                try (PreparedStatement ps = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, orderNumber);
                    ps.setInt(2, customerId);
                    ps.setDouble(3, total);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                // Detail records
                try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                    for (int i = 0; i < cart.size(); i++) {
                        int quantity = cart.get(i)[1];
                        double price = prices.get(i);

                        ps.setInt(1, orderId);
                        ps.setInt(2, cart.get(i)[0]);
                        ps.setInt(3, quantity);
                        ps.setDouble(4, price);
                        ps.setDouble(5, quantity * price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();

                System.out.println("\nOrder placed successfully.");
                System.out.println("Order Number : " + orderNumber);
                System.out.printf("Total Amount : %.2f%n", total);

            } catch (SQLException e) {
                con.rollback();
                System.out.println("Order failed. Transaction rolled back.");
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void orderHistory() {

        System.out.println("\n========== ORDER HISTORY ==========");

        int customerId = readInt("Customer ID: ");

        String sql = "SELECT o.order_number, o.order_date, COUNT(oi.order_item_id) AS items, o.total_amount "
                + "FROM orders o LEFT JOIN order_items oi ON o.order_id = oi.order_id "
                + "WHERE o.customer_id = ? "
                + "GROUP BY o.order_id, o.order_number, o.order_date, o.total_amount "
                + "ORDER BY o.order_id DESC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                System.out.printf("%-20s %-22s %6s %12s%n", "Order No", "Date", "Items", "Total");
                System.out.println("-".repeat(64));

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.printf("%-20s %-22s %6d %12.2f%n",
                            rs.getString("order_number"),
                            rs.getTimestamp("order_date"),
                            rs.getInt("items"),
                            rs.getDouble("total_amount"));
                }

                if (!found) {
                    System.out.println("No orders found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Unable to load order history: " + e.getMessage());
        }
    }

    private static void generateBill() {

        System.out.println("\n========== GENERATE BILL ==========");

        String orderNumber = readText("Order Number: ");

        String sql = "SELECT o.order_date, o.total_amount, c.name, c.phone, c.address, "
                + "m.item_name, oi.quantity, oi.unit_price, oi.item_total "
                + "FROM orders o "
                + "JOIN customers c ON o.customer_id = c.customer_id "
                + "JOIN order_items oi ON o.order_id = oi.order_id "
                + "JOIN menu m ON oi.menu_id = m.menu_id "
                + "WHERE o.order_number = ? ORDER BY oi.order_item_id";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, orderNumber);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Order not found.");
                    return;
                }

                double total = rs.getDouble("total_amount");

                System.out.println();
                System.out.println("========================================");
                System.out.println("          ONLINE FOOD ORDERING");
                System.out.println("========================================");
                System.out.println("Order No  : " + orderNumber);
                System.out.println("Date      : " + rs.getTimestamp("order_date"));
                System.out.println("Customer  : " + rs.getString("name"));
                System.out.println("Phone     : " + rs.getString("phone"));
                System.out.println("Address   : " + rs.getString("address"));
                System.out.println("----------------------------------------");
                System.out.printf("%-18s %5s %8s %10s%n", "Item", "Qty", "Price", "Total");
                System.out.println("----------------------------------------");

                do {
                    System.out.printf("%-18s %5d %8.2f %10.2f%n",
                            rs.getString("item_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("item_total"));
                } while (rs.next());

                System.out.println("----------------------------------------");
                System.out.printf("%-24s %14.2f%n", "GRAND TOTAL", total);
                System.out.println("========================================");
                System.out.println("          Thank You! Visit Again");
                System.out.println("========================================");
            }

        } catch (SQLException e) {
            System.out.println("Unable to generate bill: " + e.getMessage());
        }
    }

    private static boolean customerExists(int customerId) {

        String sql = "SELECT 1 FROM customers WHERE customer_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    private static double getPrice(int menuId) {

        String sql = "SELECT price FROM menu WHERE menu_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, menuId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return -1;
    }

    private static String readText(String message) {

        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private static int readInt(String message) {

        while (true) {
            try {
                return Integer.parseInt(readText(message));
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private static boolean readYesNo(String message) {

        while (true) {
            String choice = readText(message);

            if (choice.equalsIgnoreCase("Y")) {
                return true;
            }
            if (choice.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Enter Y or N.");
        }
    }
}