package INVENTORY;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Scanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class INVENTORYMANAGEMENT {
	public static void main(String[] args) throws SQLException {
		Scanner sc = new Scanner(System.in);
		
		HikariConfig config=new HikariConfig();
		
		String password = System.getenv("DB_PASSWORD");

	    if (password == null || password.isBlank()) {
	        throw new IllegalStateException("DB_PASSWORD is missing");
	    }

	    config.setJdbcUrl("jdbc:mysql://localhost:3306/inventory_management");
	    config.setUsername("root");
	    config.setPassword(password);
	    config.setMaximumPoolSize(3);

	    HikariDataSource hd = new HikariDataSource(config);
	    Connection con = hd.getConnection(); int choice;

        do {
            System.out.println("\n===== INVENTORY MANAGEMENT =====");
            System.out.println("1. Add Supplier");
            System.out.println("2. Add Product");
            System.out.println("3. View Products");
            System.out.println("4. Update Stock");
            System.out.println("5. Record Sale");
            System.out.println("6. View Sales");
            System.out.println("7. Generate Report");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            choice = Integer.parseInt(sc.nextLine());

            switch (choice) {
                case 1:
                    addSupplier(con, sc);
                    break;

                case 2:
                    addProduct(con, sc);
                    break;

                case 3:
                    viewProducts(con);
                    break;

                case 4:
                    updateStock(con, sc);
                    break;

                case 5:
                    recordSale(con, sc);
                    break;

                case 6:
                    viewSales(con);
                    break;

                case 7:
                    generateReport(con);
                    break;

                case 0:
                    System.out.println("Thank you!");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

        } while (choice != 0);

        con.close();
        sc.close();
    }

	public static void addSupplier(Connection con, Scanner sc)
	        throws SQLException {

	    System.out.print("Enter supplier name: ");
	    String name = sc.nextLine();

	    System.out.print("Enter phone number: ");
	    String phone = sc.nextLine();

	    String sql = "INSERT INTO suppliers (supplier_name, phone) VALUES (?, ?)";

	    PreparedStatement st = con.prepareStatement(sql);

	    st.setString(1, name);
	    st.setString(2, phone);

	    int rows = st.executeUpdate();

	    if (rows > 0) {
	        System.out.println("Supplier added successfully!");
	    }

	    st.close();
	}


public static void addProduct(Connection con, Scanner sc)
        throws SQLException {

    System.out.print("Enter product name: ");
    String name = sc.nextLine();

    System.out.print("Enter price: ");
    double price = Double.parseDouble(sc.nextLine());

    System.out.print("Enter quantity: ");
    int quantity = Integer.parseInt(sc.nextLine());

    System.out.print("Enter supplier ID: ");
    int supplierId = Integer.parseInt(sc.nextLine());

    String sql = "INSERT INTO products "
            + "(product_name, price, quantity, supplier_id) "
            + "VALUES (?, ?, ?, ?)";

    PreparedStatement st = con.prepareStatement(sql);

    st.setString(1, name);
    st.setDouble(2, price);
    st.setInt(3, quantity);
    st.setInt(4, supplierId);

    int rows = st.executeUpdate();

    if (rows > 0) {
        System.out.println("Product added successfully!");
    }

    st.close();
}
public static void viewProducts(Connection con) throws SQLException {

    String sql = "SELECT p.product_id, p.product_name, p.price, "
            + "p.quantity, s.supplier_name "
            + "FROM products p "
            + "LEFT JOIN suppliers s ON p.supplier_id = s.supplier_id";

    PreparedStatement st = con.prepareStatement(sql);
    ResultSet rs = st.executeQuery();

    while (rs.next()) {
        System.out.println(
            rs.getInt("product_id") + " | "
            + rs.getString("product_name") + " | Price: "
            + rs.getDouble("price") + " | Quantity: "
            + rs.getInt("quantity") + " | Supplier: "
            + rs.getString("supplier_name")
        );
    }

    rs.close();
    st.close();
}
public static void updateStock(Connection con, Scanner sc)
        throws SQLException {

    System.out.print("Product ID: ");
    int id = Integer.parseInt(sc.nextLine());

    System.out.print("Quantity to add: ");
    int quantity = Integer.parseInt(sc.nextLine());

    if (quantity <= 0) {
        System.out.println("Quantity must be greater than zero!");
        return;
    }

    String sql = "UPDATE products SET quantity = quantity + ? "
            + "WHERE product_id = ?";

    PreparedStatement st = con.prepareStatement(sql);
    st.setInt(1, quantity);
    st.setInt(2, id);

    int rows = st.executeUpdate();

    if (rows > 0) {
        System.out.println("Stock updated successfully!");
    } else {
        System.out.println("Product not found!");
    }

    st.close();
}
public static void recordSale(Connection con, Scanner sc)
        throws SQLException {

    System.out.print("Product ID: ");
    int id = Integer.parseInt(sc.nextLine());

    System.out.print("Quantity sold: ");
    int quantity = Integer.parseInt(sc.nextLine());

    if (quantity <= 0) {
        System.out.println("Quantity must be greater than zero!");
        return;
    }

    boolean oldAutoCommit = con.getAutoCommit();

    try {
        con.setAutoCommit(false);

        String check = "SELECT price, quantity FROM products "
                + "WHERE product_id = ? FOR UPDATE";

        PreparedStatement st = con.prepareStatement(check);
        st.setInt(1, id);

        ResultSet rs = st.executeQuery();

        if (!rs.next()) {
            System.out.println("Product not found!");
            rs.close();
            st.close();
            con.rollback();
            return;
        }

        double price = rs.getDouble("price");
        int stock = rs.getInt("quantity");

        rs.close();
        st.close();

        if (quantity > stock) {
            System.out.println("Insufficient stock!");
            con.rollback();
            return;
        }

        double total = price * quantity;

        String saleSql = "INSERT INTO sales "
                + "(product_id, quantity_sold, total_amount, sale_date) "
                + "VALUES (?, ?, ?, ?)";

        PreparedStatement sale = con.prepareStatement(saleSql);
        sale.setInt(1, id);
        sale.setInt(2, quantity);
        sale.setDouble(3, total);
        sale.setDate(4, Date.valueOf(LocalDate.now()));

        sale.executeUpdate();
        sale.close();

        String stockSql = "UPDATE products "
                + "SET quantity = quantity - ? WHERE product_id = ?";

        PreparedStatement update = con.prepareStatement(stockSql);
        update.setInt(1, quantity);
        update.setInt(2, id);

        update.executeUpdate();
        update.close();

        con.commit();

        System.out.println("Sale recorded successfully!");
        System.out.println("Total amount: " + total);

    } catch (SQLException e) {
        con.rollback();
        throw e;

    } finally {
        con.setAutoCommit(oldAutoCommit);
    }
}
public static void viewSales(Connection con) throws SQLException {

    String sql = "SELECT s.sale_id, p.product_name, "
            + "s.quantity_sold, s.total_amount, s.sale_date "
            + "FROM sales s "
            + "JOIN products p ON s.product_id = p.product_id";

    PreparedStatement st = con.prepareStatement(sql);
    ResultSet rs = st.executeQuery();

    while (rs.next()) {
        System.out.println(
            "Sale ID: " + rs.getInt("sale_id")
            + " | Product: " + rs.getString("product_name")
            + " | Quantity: " + rs.getInt("quantity_sold")
            + " | Total: " + rs.getDouble("total_amount")
            + " | Date: " + rs.getDate("sale_date")
        );
    }

    rs.close();
    st.close();
}

// 7. Generate Report
public static void generateReport(Connection con) throws SQLException {

    String sql = "SELECT COUNT(*) AS total_sales, "
            + "COALESCE(SUM(total_amount), 0) AS revenue "
            + "FROM sales";

    PreparedStatement st = con.prepareStatement(sql);
    ResultSet rs = st.executeQuery();

    if (rs.next()) {
        System.out.println("Total sales transactions: "
                + rs.getInt("total_sales"));
        System.out.println("Total revenue: "
                + rs.getDouble("revenue"));
    }

    rs.close();
    st.close();

    String stockSql = "SELECT SUM(quantity) AS total_stock FROM products";

    PreparedStatement stockSt = con.prepareStatement(stockSql);
    ResultSet stockRs = stockSt.executeQuery();

    if (stockRs.next()) {
        System.out.println("Total items in stock: "
                + stockRs.getInt("total_stock"));
    }

    stockRs.close();
    stockSt.close();
}

}
