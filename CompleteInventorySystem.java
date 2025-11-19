import java.io.*;
import java.util.*;
import java.text.*;
import java.nio.file.*;

interface InventoryOperations {
    void addProduct(Product p);
    boolean removeProductById(int id);
    Product findById(int id);
    List<Product> listAll();
    void sortByName();
    void sortByPrice();
    void saveToFile(String path) throws IOException;
    void loadFromFile(String path) throws IOException;
}

class Product implements Comparable<Product>, Serializable {
    private int id;
    private String name;
    private double price;
    private int quantity;

    public Product(int id, String name, double price, int quantity) {
        this.id = id;
        this.name = (name == null) ? "" : name;
        this.price = price;
        this.quantity = quantity;
    }

    public Product(Product other) {
        this(other.id, other.name, other.price, other.quantity);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = (name == null) ? "" : name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }

    public double totalValue() { return price * quantity; }

    public void displayShort() {
        System.out.printf("%4d | %-30s | %6d | %10.2f | %10.2f%n",
                id, name, quantity, price, totalValue());
    }

    public String toCsvRow() {
        String safeName = name.contains(",") ? ("\"" + name.replace("\"","\"\"") + "\"") : name;
        return id + "," + safeName + "," + price + "," + quantity;
    }

    @Override
    public int compareTo(Product other) {
        return Integer.compare(this.id, other.id);
    }
}

class ElectronicProduct extends Product {
    private String warranty;

    public ElectronicProduct(int id, String name, double price, int qty, String warranty) {
        super(id, name, price, qty);
        this.warranty = (warranty == null) ? "No warranty" : warranty;
    }

    public String getWarranty() { return warranty; }
    public void setWarranty(String w) { this.warranty = w; }

    @Override
    public void displayShort() {
        System.out.printf("%4d | %-30s | %6d | %10.2f | %10.2f | %-12s%n",
                getId(), getName(), getQuantity(), getPrice(), totalValue(), warranty);
    }

    public String toCsvRowExtended() {
        return super.toCsvRow() + "," + warranty;
    }
}

class Inventory implements InventoryOperations {
    private ArrayList<Product> products = new ArrayList<>();
    private int nextId = 1;

    public Inventory() {}

    public static class InventorySummary {
        public int count;
        public int totalQuantity;
        public double totalValue;

        public InventorySummary(int count, int totalQuantity, double totalValue) {
            this.count = count;
            this.totalQuantity = totalQuantity;
            this.totalValue = totalValue;
        }

        public String toString() {
            return "Count: " + count + ", TotalQty: " + totalQuantity + ", TotalValue: " + String.format("%.2f", totalValue);
        }
    }

    public class PartialIterator implements Iterator<Product> {
        private int index;

        public PartialIterator(int start) {
            this.index = Math.max(0, start);
        }

        @Override
        public boolean hasNext() {
            return index < products.size();
        }

        @Override
        public Product next() {
            if (!hasNext()) throw new NoSuchElementException();
            return products.get(index++);
        }
    }

    public void demoBackgroundAction(String label) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Background action running: " + label + " (demo)");
            }
        };
        r.run();
    }

    @Override
    public synchronized void addProduct(Product p) {
        if (p.getId() <= 0 || findById(p.getId()) != null) {
            try {
                p.setId(Integer.valueOf(nextId));
            } catch (Exception ex) {
                p.setId(nextId);
            }
            nextId++;
        } else {
            nextId = Math.max(nextId, p.getId() + 1);
        }
        products.add(p);
    }

    @Override
    public synchronized boolean removeProductById(int id) {
        Iterator<Product> it = products.iterator();
        while (it.hasNext()) {
            if (it.next().getId() == id) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Product findById(int id) {
        for (Product p : products) if (p.getId() == id) return p;
        return null;
    }

    @Override
    public synchronized List<Product> listAll() {
        return new ArrayList<>(products);
    }

    @Override
    public synchronized void sortByName() {
        products.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    @Override
    public synchronized void sortByPrice() {
        Comparator<Product> cmp = new Comparator<Product>() {
            @Override
            public int compare(Product a, Product b) {
                return Double.compare(a.getPrice(), b.getPrice());
            }
        };
        Collections.sort(products, cmp);
    }

    @Override
    public synchronized void saveToFile(String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("id,name,price,quantity");
            for (Product p : products) {
                pw.println(p.toCsvRow());
            }
        }
    }

    @Override
    public synchronized void loadFromFile(String path) throws IOException {
        products.clear();
        List<String> lines = Files.readAllLines(Paths.get(path));
        boolean first = true;
        for (String line : lines) {
            if (first) { first = false; continue; }
            if (line.trim().isEmpty()) continue;
            String[] parts = parseCsvLine(line);
            if (parts.length >= 4) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());
                    int qty = Integer.parseInt(parts[3].trim());
                    Product p = new Product(id, name, price, qty);
                    products.add(p);
                    nextId = Math.max(nextId, id + 1);
                } catch (NumberFormatException nfe) {
                    // skip malformed line
                }
            }
        }
    }

    private String[] parseCsvLine(String line) {
        ArrayList<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }

    public synchronized InventorySummary summary() {
        int count = products.size();
        int totalQty = 0;
        double totalVal = 0.0;
        for (Product p : products) {
            totalQty += p.getQuantity();
            totalVal += p.totalValue();
        }
        return new InventorySummary(count, totalQty, totalVal);
    }

    public synchronized void clearAll() {
        products.clear();
        nextId = 1;
    }

    public synchronized List<Product> filterByPriceRange(double min, double max) {
        class PriceFilter {
            public boolean matches(Product p) {
                return p.getPrice() >= min && p.getPrice() <= max;
            }
        }
        PriceFilter pf = new PriceFilter();
        List<Product> out = new ArrayList<>();
        for (Product p : products) if (pf.matches(p)) out.add(p);
        return out;
    }

    public Iterator<Product> iteratorFrom(int start) {
        return new PartialIterator(start);
    }
}

class InventoryUtils {
    public static String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }

    public static double parseDoubleOrZero(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static int parseIntOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}

public class CompleteInventorySystem {
    private static final Scanner sc = new Scanner(System.in);
    private static final Inventory inventory = new Inventory();

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printHeader();
            printMenu();
            int choice = readInt("Enter choice: ");
            switch (choice) {
                case 1: uiAddProduct(); break;
                case 2: uiRemoveProduct(); break;
                case 3: uiShowAll(); break;
                case 4: uiSortByName(); break;
                case 5: uiSortByPrice(); break;
                case 6: uiUpdateQuantity(); break;
                case 7: uiExportCsv(); break;
                case 8: uiImportCsv(); break;
                case 9: uiSummaryAndReport(); break;
                case 10: uiFilterByPrice(); break;
                case 11: uiIteratorDemo(); break;
                case 12: uiClearAll(); break;
                case 0:
                    running = false;
                    System.out.println("Exiting application. Goodbye.");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
            System.out.println();
        }
        sc.close();
    }

    private static void printHeader() {
        System.out.println("========================================");
        System.out.println("   COMPLETE PRODUCT INVENTORY SYSTEM");
        System.out.println("   (OOP concepts demo: classes, collections, IO, comparators)");
        System.out.println("========================================");
    }

    private static void printMenu() {
        System.out.println("1.  Add product");
        System.out.println("2.  Remove product by ID");
        System.out.println("3.  Show all products");
        System.out.println("4.  Sort by name");
        System.out.println("5.  Sort by price");
        System.out.println("6.  Update product quantity");
        System.out.println("7.  Export inventory to CSV");
        System.out.println("8.  Import inventory from CSV");
        System.out.println("9.  Summary & report");
        System.out.println("10. Filter by price range");
        System.out.println("11. Iterator demo (partial)");
        System.out.println("12. Clear all products");
        System.out.println("0.  Exit");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException nfe) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException nfe) {
                System.out.println("Please enter a valid number (e.g. 12.50).");
            }
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static void uiAddProduct() {
        System.out.println("-- Add Product --");
        String name = readLine("Name: ");
        double price = readDouble("Price: ");
        int qty = readInt("Quantity: ");
        String special = readLine("Is this an electronic item? (y/n): ");
        if (special.equalsIgnoreCase("y")) {
            String warranty = readLine("Warranty (e.g. 1 year): ");
            ElectronicProduct ep = new ElectronicProduct(0, name, price, qty, warranty);
            inventory.addProduct(ep);
        } else {
            Product p = new Product(0, name, price, qty);
            inventory.addProduct(p);
        }
        System.out.println("Product added.");
    }

    private static void uiRemoveProduct() {
        System.out.println("-- Remove Product --");
        int id = readInt("Enter product ID to remove: ");
        boolean ok = inventory.removeProductById(id);
        System.out.println(ok ? "Product removed." : "Product not found.");
    }

    private static void uiShowAll() {
        List<Product> list = inventory.listAll();
        if (list.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        boolean anyElectronic = false;
        for (Product p : list) if (p instanceof ElectronicProduct) { anyElectronic = true; break; }

        if (anyElectronic) {
            System.out.printf("%4s | %-30s | %6s | %10s | %10s | %-12s%n", "ID", "Name", "Qty", "Price", "Value", "Warranty");
            System.out.println("-----------------------------------------------------------------------------------------------");
            for (Product p : list) {
                if (p instanceof ElectronicProduct) ((ElectronicProduct)p).displayShort();
                else p.displayShort();
            }
        } else {
            System.out.printf("%4s | %-30s | %6s | %10s | %10s%n", "ID", "Name", "Qty", "Price", "Value");
            System.out.println("--------------------------------------------------------------------------");
            for (Product p : list) p.displayShort();
        }
    }

    private static void uiSortByName() {
        inventory.sortByName();
        System.out.println("Sorted by name.");
    }

    private static void uiSortByPrice() {
        inventory.sortByPrice();
        System.out.println("Sorted by price.");
    }

    private static void uiUpdateQuantity() {
        System.out.println("-- Update Quantity --");
        int id = readInt("Enter product ID: ");
        Product p = inventory.findById(id);
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }
        int newQty = readInt("Enter new quantity: ");
        p.setQuantity(newQty);
        System.out.println("Quantity updated.");
    }

    private static void uiExportCsv() {
        System.out.println("-- Export Inventory to CSV --");
        String path = readLine("Enter file path (e.g. inventory.csv): ");
        try {
            inventory.saveToFile(path);
            System.out.println("Exported to " + path);
        } catch (IOException ioe) {
            System.out.println("Error while saving file: " + ioe.getMessage());
        }
    }

    private static void uiImportCsv() {
        System.out.println("-- Import Inventory from CSV --");
        String path = readLine("Enter file path (e.g. inventory.csv): ");
        try {
            inventory.loadFromFile(path);
            System.out.println("Imported from " + path);
        } catch (IOException ioe) {
            System.out.println("Error while loading file: " + ioe.getMessage());
        }
    }

    private static void uiSummaryAndReport() {
        Inventory.InventorySummary s = inventory.summary();
        System.out.println("-- Inventory Summary --");
        System.out.println(s.toString());
        String rpt = generateReport();
        System.out.println();
        System.out.println(rpt);
        String save = readLine("Save report to file? (y/n): ");
        if (save.equalsIgnoreCase("y")) {
            String path = readLine("Report file path (e.g. report.txt): ");
            try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
                pw.println(rpt);
                System.out.println("Report saved to " + path);
            } catch (IOException e) {
                System.out.println("Failed to save report: " + e.getMessage());
            }
        }
    }

    private static String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("INVENTORY REPORT\n");
        sb.append("================\n");
        List<Product> list = inventory.listAll();
        sb.append(String.format("Products: %d%n", list.size()));
        int totalQty = 0;
        double totalVal = 0.0;
        for (Product p : list) {
            totalQty += p.getQuantity();
            totalVal += p.totalValue();
        }
        sb.append(String.format("Total quantity: %d%n", totalQty));
        sb.append(String.format("Total inventory value: %s%n", InventoryUtils.formatCurrency(totalVal)));
        sb.append("\nProducts detail:\n");
        for (Product p : list) {
            sb.append(String.format("ID:%d Name:%s Qty:%d Price:%.2f Value:%.2f%n",
                    p.getId(), p.getName(), p.getQuantity(), p.getPrice(), p.totalValue()));
        }
        return sb.toString();
    }

    private static void uiFilterByPrice() {
        System.out.println("-- Filter by Price Range --");
        double min = readDouble("Min price: ");
        double max = readDouble("Max price: ");
        List<Product> filtered = inventory.filterByPriceRange(min, max);
        if (filtered.isEmpty()) {
            System.out.println("No products in given price range.");
            return;
        }
        System.out.println("Products in range:");
        for (Product p : filtered) p.displayShort();
    }

    private static void uiIteratorDemo() {
        System.out.println("-- Iterator Demo (Partial from index) --");
        int start = readInt("Start index (0-based): ");
        Iterator<Product> it = inventory.iteratorFrom(start);
        int count = 0;
        while (it.hasNext()) {
            Product p = it.next();
            System.out.print("[" + (start + count) + "] ");
            p.displayShort();
            count++;
            if (count >= 10) {
                System.out.println("... showing first 10 items from start");
                break;
            }
        }
    }

    private static void uiClearAll() {
        String yes = readLine("Are you sure you want to clear all products? (type YES to confirm): ");
        if ("YES".equals(yes)) {
            inventory.clearAll();
            System.out.println("All products cleared.");
        } else {
            System.out.println("Clear cancelled.");
        }
    }
}
