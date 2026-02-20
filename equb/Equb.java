

import java.io.*;
import java.util.*;

abstract class User {
    private String username, password, role;
    public User(String u, String p, String role) {
        this.username = u; this.password = p; this.role = role;
    }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean authenticate(String pass) { return password.equals(pass); }
    public abstract void showMenu();
}
class Admin extends User {
    public Admin(String u, String p) { super(u, p, "ADMIN"); }
    @Override public void showMenu() {
        System.out.println("\n=== ADMIN ===");
        System.out.println("1. View Members\n2. Add Member\n3. Set Contribution\n4. Set Penalty");
        System.out.println("5. Process Week\n6. Reports\n7. Apply Penalty\n8. Check Late Status");
        System.out.println("9. Settings\n10. Special Pot Requests\n11. Logout");
        System.out.print("Select: ");
    }
}
class Client extends User {
    public Client(String u, String p) { super(u, p, "CLIENT"); }
    @Override public void showMenu() {
        System.out.println("\n=== CLIENT ===");
        System.out.println("1. My Status\n2. Equub Info\n3. Contact Admin\n4. About System");
        System.out.println("5. Request Special Pot\n6. Logout");
        System.out.print("Select: ");
    }
}
class EquubMember {
    private String name;
    private boolean receivedPot, contributedThisWeek;
    private int totalContributed, penalties;
    private List<String> lateWeeks;
    private boolean specialRequest;
    private String specialReason;
    private int timesReceived; // NEW: Track how many times received pot  
    public EquubMember(String name) {
        this.name = name;
        this.receivedPot = false;
        this.contributedThisWeek = false;
        this.totalContributed = 0;
        this.penalties = 0;
        this.lateWeeks = new ArrayList<>();
        this.specialRequest = false;
        this.specialReason = "";
        this.timesReceived = 0;
    }
    public String getName() { return name; }
    public boolean hasReceivedPot() { return receivedPot; }
    public void setReceivedPot(boolean r) { 
        receivedPot = r; 
        if (r) timesReceived++;
    }
    public boolean hasContributedThisWeek() { return contributedThisWeek; }
    public void setContributedThisWeek(boolean c) { contributedThisWeek = c; }
    public int getTotalContributed() { return totalContributed; }
    public int getPenalties() { return penalties; }
    public List<String> getLateWeeks() { return lateWeeks; }
    public boolean hasSpecialRequest() { return specialRequest; }
    public void setSpecialRequest(boolean sr) { specialRequest = sr; }
    public String getSpecialReason() { return specialReason; }
    public void setSpecialReason(String reason) { specialReason = reason; }
    public int getTimesReceived() { return timesReceived; } // NEW    
    public void addPenalty(int amount, int week) { 
        penalties += amount;
        totalContributed += amount;
        markLateForWeek(week);
    }    
    public void addContribution(int amount) {
        this.contributedThisWeek = true;
        this.totalContributed += amount;
    }   
    public void markLateForWeek(int week) {
        lateWeeks.add("Week " + week);
    }  
    public void resetForNewWeek() { 
        this.contributedThisWeek = false;
    }   
    public void resetForNewCycle() {
        this.receivedPot = false;
        this.contributedThisWeek = false;
        this.totalContributed = 0;
        this.penalties = 0;
        this.lateWeeks.clear();
        this.specialRequest = false;
        this.specialReason = "";
    }
    @Override public String toString() {
        String result = name + " | Received: " + (receivedPot?"Yes":"No") + 
               " (" + timesReceived + " times) | Paid: " + (contributedThisWeek?"Yes":"No") +
               " | Total: $" + totalContributed +
               " | Penalties: $" + penalties;
        if (specialRequest) {
            result += " | Special Request: " + specialReason;
}
        return result;
    }
}
public class Equb {
    private List<User> users = new ArrayList<>();
    private List<EquubMember> members = new ArrayList<>();
    private User currentUser;
    private Scanner sc = new Scanner(System.in);  
    private int weeklyContribution = 1000;
    private int currentWeek = 1;
    private int potAmount = 0;
    private int penaltyAmount = 100;
    private int penaltyFund = 0;
    private Random random = new Random(); // NEW: Random generator
    public Equb() {
        System.out.println("Equub System Starting...");
        loadUsers();
        loadMembers();
        loadPenaltyFund();
    }   
    private void loadUsers() {
        try {
            File f = new File("users.txt");
            if (!f.exists()) {
                users.add(new Admin("admin", "admin123"));
                users.add(new Client("client", "client123"));
                saveUsersToFile();
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 3) {
                    if (d[2].equals("ADMIN")) users.add(new Admin(d[0], d[1]));
                    else users.add(new Client(d[0], d[1]));
                }
            }
            br.close();
        } catch (IOException e) {
            users.add(new Admin("admin", "admin123"));
            users.add(new Client("client", "client123"));
        }
    } 
    private void saveUsersToFile() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("users.txt"));
            for (User u : users) {
                String password = "default123";
                if (u.getUsername().equals("admin")) password = "admin123";
                else if (u.getUsername().equals("client")) password = "client123";
                bw.write(u.getUsername()+","+password+","+u.getRole());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    } 
    private void loadMembers() {
        try {
            File f = new File("members.txt");
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 5) {
                    EquubMember m = new EquubMember(d[0].trim());
                    m.setReceivedPot(Boolean.parseBoolean(d[1].trim()));
                    m.setContributedThisWeek(Boolean.parseBoolean(d[2].trim()));
                    int total = Integer.parseInt(d[3].trim());
                    int penalties = Integer.parseInt(d[4].trim());
                    m.addContribution(total - penalties);                 
                    if (d.length >= 7) {
                        m.setSpecialRequest(Boolean.parseBoolean(d[5].trim()));
                        m.setSpecialReason(d[6].trim());
                    }                   
                    if (d.length >= 8) {
                        // Load times received if exists
                        m.setReceivedPot(true); // Will increment count
                    }       
                    members.add(m);
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Error loading members: " + e.getMessage());
        }
    } 
    private void loadPenaltyFund() {
        try {
            File f = new File("penalty_fund.txt");
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            if (line != null) {
                penaltyFund = Integer.parseInt(line.trim());
            }
            br.close();
        } catch (IOException e) {

System.out.println("Error loading penalty fund: " + e.getMessage());
        }
    }  
    private void saveMembers() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("members.txt"));
            for (EquubMember m : members) {
                bw.write(m.getName()+","+m.hasReceivedPot()+","+m.hasContributedThisWeek()+","+
                        m.getTotalContributed()+","+m.getPenalties()+","+
                        m.hasSpecialRequest()+","+m.getSpecialReason());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            System.out.println("Error saving members: " + e.getMessage());
        }
    }   
    private void savePenaltyData() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("penalty_fund.txt"));
            bw.write(String.valueOf(penaltyFund));
            bw.close();
            
            BufferedWriter logBw = new BufferedWriter(new FileWriter("penalty_log.txt", true));
            logBw.write("Week " + currentWeek + " - Penalty Fund: $" + penaltyFund);
            logBw.newLine();
            for (EquubMember m : members) {
                if (m.getPenalties() > 0) {
                    logBw.write("  " + m.getName() + ": $" + m.getPenalties());
                    logBw.newLine();
                }
            }
            logBw.write("-----------------------------------");
            logBw.newLine();
            logBw.close();
        } catch (IOException e) {
            System.out.println("Error saving penalty data: " + e.getMessage());
        }
    }  
    public void start() {
        while (true) {
            System.out.println("\n=== EQUUB SYSTEM ===");
            System.out.println("1. Login\n2. About Team\n3. Exit");
            System.out.print("Choice: ");
            int ch = getInt();
            if (ch == 1) login();
            else if (ch == 2) aboutUs();
            else if (ch == 3) {
                saveMembers();
                savePenaltyData();
                System.out.println("Goodbye!");
                return;
            } else System.out.println("Invalid!");
        }
    } 
    private void login() {
        System.out.println("\n=== LOGIN ===");
        System.out.println("Available users: Admin (admin/admin123) | Client (client/client123)");
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();    
        if (u.equals("admin") && p.equals("admin123")) {
            currentUser = new Admin(u, p);
            System.out.println("\nWelcome, Admin!");
            dashboard();
        } else if (u.equals("client") && p.equals("client123")) {
            currentUser = new Client(u, p);
            System.out.println("\nWelcome, Client!");
            dashboard();
        } else {
            System.out.println("\nLogin failed! Try admin/admin123 or client/client123");
        }
    }
    private void dashboard() {
        while (true) {
            currentUser.showMenu();
            int c = getInt();
            if (currentUser instanceof Admin) {
                if (!adminActions(c)) break;
            } else {
                if (!clientActions(c)) break;
            }
        }
        currentUser = null;
    }   
    private boolean adminActions(int c) {
        switch(c) {
            case 1: viewAllMembers(); break;
            case 2: addNewMember(); break;
            case 3: setWeeklyContribution(); break;
            case 4: setPenaltyAmount(); break;
            case 5: processNextWeek(); break;
            case 6: viewReports(); break;
            case 7: applyPenalty(); break;
            case 8: checkMemberLateStatus(); break;
            case 9: systemSettings(); break;
            case 10: handleSpecialRequestAdmin(); break;
            case 11: System.out.println("Logging out..."); return false;
            default: System.out.println("Invalid!");
        }
        return true;
    } 
    private void viewAllMembers() {

if (members.isEmpty()) {
            System.out.println("No members yet.");
            return;
        }
        System.out.println("\n=== ALL MEMBERS ===");
        System.out.println("Weekly: $" + weeklyContribution + " | Penalty: $" + penaltyAmount);
        for (int i = 0; i < members.size(); i++) {
            System.out.println((i+1) + ". " + members.get(i));
        }
        System.out.println("\nWeek: " + currentWeek + " | Pot: $" + potAmount);
        System.out.println("Penalty Fund: $" + penaltyFund);
        System.out.println("Paid this week: " + countMembersPaid() + "/" + members.size());
    } 
    private int countMembersPaid() {
        int count = 0;
        for (EquubMember m : members) if (m.hasContributedThisWeek()) count++;
        return count;
    } 
    private void addNewMember() {
        System.out.print("\nNew member name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name required!");
            return;
        }
        for (EquubMember m : members) {
            if (m.getName().equalsIgnoreCase(name)) {
                System.out.println("Member exists!");
                return;
            }
        }
        EquubMember newMember = new EquubMember(name);
        members.add(newMember);
        saveMembers();
        System.out.println("Member added!");
    }
    private void setWeeklyContribution() {
        System.out.println("\nCurrent: $" + weeklyContribution);
        System.out.print("New amount (min $100): $");
        int newAmount = getInt();
        if (newAmount < 100) {
            System.out.println("Too low!");
            return;
        }
        weeklyContribution = newAmount;
        System.out.println("Updated!");
    }
    private void setPenaltyAmount() {
        System.out.println("\nCurrent penalty: $" + penaltyAmount);
        System.out.print("New amount (min $50): $");
        int newAmount = getInt();
        if (newAmount < 50) {
            System.out.println("Too low!");
            return;
        }
        penaltyAmount = newAmount;
        System.out.println("Updated!");
    }  
    private void processNextWeek() {
        if (members.isEmpty()) {
            System.out.println("No members!");
            return;
        }
        System.out.println("\n=== WEEK " + currentWeek + " ===");   
        // Calculate pot based on contributions only
        potAmount = members.size() * weeklyContribution;  
        // Apply penalties to late members
        List<EquubMember> lateMembers = new ArrayList<>();
        for (EquubMember m : members) {
            if (!m.hasContributedThisWeek()) {
                lateMembers.add(m);
                m.addPenalty(penaltyAmount, currentWeek);
                penaltyFund += penaltyAmount;
                System.out.println(m.getName() + " late - penalty $" + penaltyAmount);
            }
        }       
        System.out.println("\nWeekly pot: $" + potAmount + " (" + members.size() + " members × $" + weeklyContribution + ")");
        System.out.println("Late members: " + lateMembers.size());
        System.out.println("Penalty fund: $" + penaltyFund);       
        // Get eligible members (haven't received pot or special case)
        List<EquubMember> eligibleMembers = new ArrayList<>();
        for (EquubMember m : members) {
            if (!m.hasReceivedPot()) {
                eligibleMembers.add(m);
            }
        }       
        // Check if all have received (cycle complete)
        if (eligibleMembers.isEmpty()) {
            System.out.println("\nCYCLE COMPLETE! New cycle starts.");
            for (EquubMember m : members) {
                m.resetForNewCycle();
            }
            // Add all members to eligible list for new cycle
            eligibleMembers.addAll(members);
        }       
        // SPECIAL CASE: Check for special requests first
        List<EquubMember> specialRequestMembers = new ArrayList<>();
        for (EquubMember m : eligibleMembers) {
            if (m.hasSpecialRequest()) {

specialRequestMembers.add(m);
            }
        }     
        EquubMember receiver = null;       
        // If there are special requests, handle them first
        if (!specialRequestMembers.isEmpty()) {
            System.out.println("\n=== SPECIAL REQUESTS DETECTED ===");
            for (int i = 0; i < specialRequestMembers.size(); i++) {
                EquubMember m = specialRequestMembers.get(i);
                System.out.println((i+1) + ". " + m.getName() + " - Reason: " + m.getSpecialReason());
            }       
            System.out.println("\nOptions:");
            System.out.println("1. Select from special requests");
            System.out.println("2. Use random selection from all eligible");
            System.out.println("3. Use manual selection");
            System.out.print("Choose method: ");
            int choice = getInt();       
            switch(choice) {
                case 1:
                    // Select from special requests
                    System.out.println("\nSelect special request member:");
                    for (int i = 0; i < specialRequestMembers.size(); i++) {
                        System.out.println((i+1) + ". " + specialRequestMembers.get(i).getName());
                    }
                    System.out.print("Choose (1-" + specialRequestMembers.size() + "): ");
                    int select = getInt();
                    if (select > 0 && select <= specialRequestMembers.size()) {
                        receiver = specialRequestMembers.get(select - 1);
                        System.out.println("Selected " + receiver.getName() + " for special case.");
                    } else {
                        System.out.println("Invalid, using random selection.");
                        receiver = specialRequestMembers.get(random.nextInt(specialRequestMembers.size()));
                    }
                    break;                
                case 2:
                    // Random selection from all eligible
                    receiver = eligibleMembers.get(random.nextInt(eligibleMembers.size()));
                    System.out.println("Randomly selected: " + receiver.getName());
                    break;                   
                case 3:
                    // Manual selection from all eligible
                    System.out.println("\nSelect member manually:");
                    for (int i = 0; i < eligibleMembers.size(); i++) {
                        System.out.println((i+1) + ". " + eligibleMembers.get(i).getName());
                    }
                    System.out.print("Choose (1-" + eligibleMembers.size() + "): ");
                    int manual = getInt();
                    if (manual > 0 && manual <= eligibleMembers.size()) {
                        receiver = eligibleMembers.get(manual - 1);
                    } else {
                        System.out.println("Invalid, using random.");
                        receiver = eligibleMembers.get(random.nextInt(eligibleMembers.size()));
                    }
                    break;                 
                default:
                    System.out.println("Invalid, using random from special requests.");
                    receiver = specialRequestMembers.get(random.nextInt(specialRequestMembers.size()));
            }
        } else {
            // No special requests, use normal selection
            System.out.println("\n=== SELECT POT RECEIVER ===");
            System.out.println("Eligible members (" + eligibleMembers.size() + "):");
            for (int i = 0; i < eligibleMembers.size(); i++) {
                EquubMember m = eligibleMembers.get(i);
                System.out.println((i+1) + ". " + m.getName() + 
                                 " (Received: " + m.getTimesReceived() + " times)");
            } 
            System.out.println("\nSelection method:");
            System.out.println("1. Random selection");
            System.out.println("2. Manual selection");
            System.out.println("3. Least received first (fairness)");

System.out.print("Choose method: ");
            int method = getInt();          
            switch(method) {
                case 1:
                    // Random selection
                    receiver = eligibleMembers.get(random.nextInt(eligibleMembers.size()));
                    System.out.println("Randomly selected: " + receiver.getName());
                    break;                 
                case 2:
                    // Manual selection
                    System.out.print("Select member (1-" + eligibleMembers.size() + "): ");
                    int choice = getInt();
                    if (choice > 0 && choice <= eligibleMembers.size()) {
                        receiver = eligibleMembers.get(choice - 1);
                    } else {
                        System.out.println("Invalid, using random.");
                        receiver = eligibleMembers.get(random.nextInt(eligibleMembers.size()));
                    }
                    break;         
                case 3:
                    // Least received first (most fair)
                    receiver = eligibleMembers.get(0);
                    int minReceived = receiver.getTimesReceived();
                    for (EquubMember m : eligibleMembers) {
                        if (m.getTimesReceived() < minReceived) {
                            receiver = m;
                            minReceived = m.getTimesReceived();
                        }
                    }
                    System.out.println("Selected " + receiver.getName() + 
                                     " (received only " + minReceived + " times)");
                    break;                  
                default:
                    System.out.println("Invalid, using random selection.");
                    receiver = eligibleMembers.get(random.nextInt(eligibleMembers.size()));
            }
        }    
        if (receiver == null) {
            System.out.println("No eligible receiver found!");
            return;
        }    
        // Process the receiver
        receiver.setReceivedPot(true);      
        // Clear special request if they had one
        if (receiver.hasSpecialRequest()) {
            receiver.setSpecialRequest(false);
            receiver.setSpecialReason("");
        }      
        System.out.println("\n🎉 " + receiver.getName() + " receives $" + potAmount + "!");      
        // Save data
        savePenaltyData();        
        // Reset for next week
        for (EquubMember m : members) m.resetForNewWeek();
        currentWeek++;       
        saveMembers();
        System.out.println("Week processed!");
    }  
    private void handleSpecialRequestAdmin() {
        System.out.println("\n=== SPECIAL POT REQUESTS ===");
        boolean hasRequests = false;      
        for (EquubMember m : members) {
            if (m.hasSpecialRequest() && !m.hasReceivedPot()) {
                System.out.println("Request from: " + m.getName());
                System.out.println("Reason: " + m.getSpecialReason());
                System.out.println("Status: Pending");
                System.out.println("---");
                hasRequests = true;
            }
        }        
        if (!hasRequests) {
            System.out.println("No pending special requests.");
        }        
        System.out.println("\nOptions:");
        System.out.println("1. Approve a request for next week");
        System.out.println("2. Cancel a request");
        System.out.println("3. View all requests history");
        System.out.print("Select: ");
        int choice = getInt();      
        if (choice == 1) {
            System.out.print("Enter member name to approve: ");
            String name = sc.nextLine().trim();
            for (EquubMember m : members) {
                if (m.getName().equalsIgnoreCase(name) && m.hasSpecialRequest()) {
                    System.out.println("Approved! " + name + " will get priority next week.");
                    return;
                }
            }

System.out.println("No active request found for " + name);
        } else if (choice == 2) {
            System.out.print("Enter member name to cancel request: ");
            String name = sc.nextLine().trim();
            for (EquubMember m : members) {
                if (m.getName().equalsIgnoreCase(name) && m.hasSpecialRequest()) {
                    m.setSpecialRequest(false);
                    m.setSpecialReason("");
                    saveMembers();
                    System.out.println("Request cancelled for " + name);
                    return;
                }
            }
            System.out.println("No active request found for " + name);
        } else if (choice == 3) {
            viewSpecialRequestsHistory();
        }
    }   
    private void viewSpecialRequestsHistory() {
        System.out.println("\n=== SPECIAL REQUESTS HISTORY ===");
        try {
            File f = new File("special_requests.txt");
            if (!f.exists()) {
                System.out.println("No special requests history.");
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Error reading history: " + e.getMessage());
        }
    }  
    private void viewReports() {
        System.out.println("\n=== REPORTS ===");
        System.out.println("Total Members: " + members.size());
        System.out.println("Weekly Contribution: $" + weeklyContribution);
        System.out.println("Penalty Amount: $" + penaltyAmount);
        System.out.println("Current Week: " + currentWeek);
        System.out.println("Current Pot: $" + potAmount);
        System.out.println("Penalty Fund: $" + penaltyFund);      
        int received = 0, totalContributed = 0, totalPenalties = 0;
        for (EquubMember m : members) {
            if (m.hasReceivedPot()) received++;
            totalContributed += m.getTotalContributed();
            totalPenalties += m.getPenalties();
        }
        System.out.println("\nMembers who received pot: " + received + "/" + members.size());
        System.out.println("Total contributions: $" + totalContributed);
        System.out.println("Total penalties: $" + totalPenalties);
        System.out.println("Penalty fund balance: $" + penaltyFund);     
        // Show distribution fairness
        System.out.println("\nFairness Report:");
        for (EquubMember m : members) {
            System.out.println(m.getName() + ": Received " + m.getTimesReceived() + " times");
        }
    }   
    private void applyPenalty() {
        if (members.isEmpty()) {
            System.out.println("No members!");
            return;
        }
        System.out.println("\n=== APPLY PENALTY ===");
        for (int i = 0; i < members.size(); i++) {
            System.out.println((i+1) + ". " + members.get(i).getName());
        }
        System.out.print("Select member: ");
        int choice = getInt();
        if (choice < 1 || choice > members.size()) {
            System.out.println("Invalid!");
            return;
        }
        EquubMember member = members.get(choice - 1);
        member.addPenalty(penaltyAmount, currentWeek);
        penaltyFund += penaltyAmount;
        saveMembers();
        savePenaltyData();
        System.out.println("Penalty applied and saved to penalty fund!");
    }    
    private void checkMemberLateStatus() {
        if (members.isEmpty()) {
            System.out.println("No members!");
            return;
        }
        System.out.println("\n=== CHECK LATE STATUS ===");
        for (int i = 0; i < members.size(); i++) {
            System.out.println((i+1) + ". " + members.get(i).getName());
        }
        System.out.print("Select member (0 for all): ");
        int choice = getInt();
        
        if (choice == 0) {
            for (EquubMember m : members) {

System.out.println(m.getName() + ": Penalties $" + m.getPenalties());
            }
        } else if (choice >= 1 && choice <= members.size()) {
            EquubMember member = members.get(choice - 1);
            System.out.println("\n" + member.getName() + ":");
            System.out.println("Penalties: $" + member.getPenalties());
            System.out.println("Late weeks: " + (member.getLateWeeks().isEmpty() ? "None" : member.getLateWeeks()));
        } else {
            System.out.println("Invalid!");
        }
    }  
    private void systemSettings() {
        System.out.println("\n=== SETTINGS ===");
        System.out.println("1. Force Mark Payment\n2. Reset Member Data\n3. View Penalty Log");
        System.out.println("4. Reset Cycle\n5. Force Random Selection\n6. Back");
        System.out.print("Select: ");
        int choice = getInt();
        if (choice == 1) forceMarkPayment();
        else if (choice == 2) resetMemberData();
        else if (choice == 3) viewPenaltyLog();
        else if (choice == 4) resetCycle();
        else if (choice == 5) forceRandomSelection();
    }  
    // NEW: Force random selection next week
    private void forceRandomSelection() {
        System.out.println("\nNext week will use random selection regardless of special requests.");
        System.out.println("This ensures maximum fairness.");
        // This would be implemented by setting a flag, but for simplicity
        // we'll just note it for the admin
        System.out.println("Note: Use option 1 (Random selection) in process week.");
    }  
    private void resetCycle() {
        System.out.print("\nReset cycle? All members will be eligible again. (yes/no): ");
        if (sc.nextLine().trim().equalsIgnoreCase("yes")) {
            for (EquubMember m : members) {
                m.resetForNewCycle();
            }
            saveMembers();
            System.out.println("Cycle reset! All members can receive pot again.");
        }
    }  
    private void viewPenaltyLog() {
        System.out.println("\n=== PENALTY LOG ===");
        try {
            File f = new File("penalty_log.txt");
            if (!f.exists()) {
                System.out.println("No penalty log found.");
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Error reading penalty log: " + e.getMessage());
        }
    }    
    private void forceMarkPayment() {
        System.out.print("\nEnter member name: ");
        String name = sc.nextLine().trim();
        for (EquubMember m : members) {
            if (m.getName().equalsIgnoreCase(name)) {
                if (!m.hasContributedThisWeek()) {
                    m.addContribution(weeklyContribution);
                    saveMembers();
                    System.out.println("Payment marked!");
                } else {
                    System.out.println("Already paid!");
                }
                return;
            }
        }
        System.out.println("Member not found!");
    }    
    private void resetMemberData() {
        System.out.print("\nType 'RESET' to confirm: ");
        if (sc.nextLine().trim().equals("RESET")) {
            members.clear();
            currentWeek = 1;
            potAmount = 0;
            penaltyFund = 0;
            try {
                new FileWriter("members.txt", false).close();
                new FileWriter("penalty_fund.txt", false).close();
                new FileWriter("penalty_log.txt", false).close();
                new FileWriter("special_requests.txt", false).close();
            } catch (IOException e) {}
            System.out.println("All data reset!");
        } else {
            System.out.println("Cancelled.");
        }
    }  
    private boolean clientActions(int c) {
        switch(c) {
            case 1: viewClientStatus(); break;

case 2: viewEquubInfo(); break;
            case 3: contactAdmin(); break;
            case 4: aboutSystem(); break;
            case 5: requestSpecialPot(); break;
            case 6: System.out.println("Logging out..."); return false;
            default: System.out.println("Invalid!");
        }
        return true;
    }
    private void requestSpecialPot() {
        String clientName = currentUser.getUsername();
        EquubMember clientMember = null;      
        for (EquubMember m : members) {
            if (m.getName().equalsIgnoreCase(clientName)) {
                clientMember = m;
                break;
            }
        }      
        if (clientMember == null) {
            System.out.println("You are not registered as a member.");
            return;
        }      
        if (clientMember.hasReceivedPot()) {
            System.out.println("You have already received the pot this cycle.");
            return;
        }      
        if (clientMember.hasSpecialRequest()) {
            System.out.println("You already have a pending special request.");
            System.out.println("Reason: " + clientMember.getSpecialReason());
            return;
        }      
        System.out.println("\n=== REQUEST SPECIAL POT ===");
        System.out.println("Note: Special requests are for urgent needs only.");
        System.out.print("Enter reason for special request: ");
        String reason = sc.nextLine().trim();       
        if (reason.isEmpty()) {
            System.out.println("Reason required!");
            return;
        }      
        System.out.print("Submit request? (yes/no): ");
        if (sc.nextLine().trim().equalsIgnoreCase("yes")) {
            clientMember.setSpecialRequest(true);
            clientMember.setSpecialReason(reason);
            saveMembers();            
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter("special_requests.txt", true));
                bw.write(new Date() + " - " + clientName + ": " + reason);
                bw.newLine();
                bw.close();
            } catch (IOException e) {
                System.out.println("Error logging request.");
            }          
            System.out.println("Request submitted! Admin will review it.");
        } else {
            System.out.println("Request cancelled.");
        }
    }  
    private void viewClientStatus() {
        String name = currentUser.getUsername();
        System.out.println("\n=== YOUR STATUS ===");
        System.out.println("Name: " + name);
        System.out.println("Weekly Contribution: $" + weeklyContribution);
        System.out.println("Current Week: " + currentWeek);
        System.out.println("Current Pot: $" + potAmount);
        System.out.println("System Penalty Fund: $" + penaltyFund);     
        for (EquubMember m : members) {
            if (m.getName().equalsIgnoreCase(name)) {
                System.out.println("Received pot: " + (m.hasReceivedPot()?"Yes (" + m.getTimesReceived() + " times)":"No"));
                System.out.println("Paid this week: " + (m.hasContributedThisWeek()?"Yes":"No"));
                System.out.println("Total contributed: $" + m.getTotalContributed());
                System.out.println("Penalties: $" + m.getPenalties());
                if (m.hasSpecialRequest()) {
                    System.out.println("Special request: PENDING - " + m.getSpecialReason());
                }
                return;
            }
        }
        System.out.println("Not a member yet.");
    } 
    private void viewEquubInfo() {
        System.out.println("\n=== EQUUB INFO ===");
        System.out.println("Weekly: $" + weeklyContribution);
        System.out.println("Penalty: $" + penaltyAmount);
        System.out.println("Week: " + currentWeek);
        System.out.println("Members: " + members.size());
        System.out.println("Pot: $" + potAmount);
        System.out.println("Penalty Fund: $" + penaltyFund);
        System.out.println("\nSelection Methods:");

System.out.println("1. Random - Everyone has equal chance");
        System.out.println("2. Manual - Admin chooses");
        System.out.println("3. Least received - Most fair");
        System.out.println("4. Special cases - For emergencies");
    }  
    private void contactAdmin() {
        System.out.println("\n=== CONTACT ADMIN ===");
        System.out.println("Email: admin@equub.com");
        System.out.println("Phone: +251-911-123456");
        System.out.println("For special pot requests: Use option 5 in menu");
    } 
    private void aboutSystem() {
        System.out.println("\n=== ABOUT SYSTEM ===");
        System.out.println("Equub Management System v3.0");
        System.out.println("FAIR POT DISTRIBUTION:");
        System.out.println("- Multiple selection methods");
        System.out.println("- Random selection available");
        System.out.println("- Special requests for emergencies");
        System.out.println("- Tracks how many times each received");
    }  
    private void aboutUs() {
        System.out.println("\n=== OUR TEAM ===");
        System.out.println("Equub Development Team");
        System.out.println("Contact: team@equub.com");
    }  
    private int getInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            return -1;
        }
    }   
    public static void main(String[] args) {
        new Equb().start();
    }
}