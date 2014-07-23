package eu.valkyr.cubestats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class cubestats extends JavaPlugin implements Listener {
	
	
	private MySQL sql;
	private BukkitTask writer;
	private ArrayList<Object[]> sessions = new ArrayList<Object[]>();
	private ArrayList<Object[]> enchants = new ArrayList<Object[]>();
	private ArrayList<Object[]> blocks = new ArrayList<Object[]>();
	private ArrayList<Object[]> kills = new ArrayList<Object[]>();
	
	
	public void onEnable() {
		
		this.reloadConfig();
		sql = new MySQL(Logger.getLogger("Minecraft"), 
				"[cubestats]",
	            this.getConfig().getString("dbhost"), 
	            this.getConfig().getInt("dbport"), 
	            this.getConfig().getString("db"), 
	            this.getConfig().getString("dbuser"), 
	            this.getConfig().getString("dbpw"));
		
		if (sql.open()) {
			this.getLogger().info("cubestats got db connection...");
			if(sql.isTable("session") == false){
    			try {
					sql.insert("CREATE TABLE session (sessid BIGINT PRIMARY KEY AUTO_INCREMENT, UUID VARCHAR(36), start INT, end INT);");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			if(sql.isTable("kills") == false){
    			try {
					sql.insert("CREATE TABLE kills (UUID VARCHAR(36), time INT, enemy VARCHAR(36));");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			if(sql.isTable("blocks") == false){
    			try {
					sql.insert("CREATE TABLE blocks (UUID VARCHAR(36), count INT, block INT);");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			if(sql.isTable("enchants") == false){
    			try {
					sql.insert("CREATE TABLE enchants (UUID VARCHAR(36), time INT, item INT);");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			writer = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			    public void run() {
			    	update();
			   }},0,this.getConfig().getInt("interval"));
		}
		else {
			this.getLogger().severe("cubestats got no db connection... will not work");
		}
		
		this.saveDefaultConfig();
		addSession(Bukkit.getServerName());
		this.getLogger().info("cubestats is now tracking!");
	}
	 
	public void onDisable() {
		
		writer.cancel();
		update();
		sql.close();
		this.getLogger().info("cubestats stopped tracking!");
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLogin(PlayerLoginEvent event) {
		
		addSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void EnchantItemEvent(Player enchanter, InventoryView view, Block table, ItemStack item, int level, Map<Enchantment,Integer> enchants, int i) {
		
	}
	
	public void writeToDB() {
		
		for(int i=0; i < sessions.size(); i++) {
			
			if (sessions.get(i)[3] != null) {
				try {
					sql.insert("UPDATE session SET end='"+sessions.get(i)[2]+"' WHERE sessid='"+sessions.get(i)[3]+"';");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				try {
					sql.insert("INSERT INTO session (UUID,start,end) VALUES ('"+sessions.get(i)[0]+"','"+sessions.get(i)[1]+"','"+sessions.get(i)[2]+"');");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ResultSet re;
				try {
					re = sql.query("SELECT sessid FROM session WHERE UUID='"+sessions.get(i)[0]+"' AND start='"+sessions.get(i)[1]+"';");
					while(re.next()) {
						sessions.get(i)[3] = re.getInt("sessid");
					}
					re.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void updateTimings() {
		
		for(int i=0; i<sessions.size(); i++) {
			sessions.get(i)[2] = getTime();
		}
	}
	
	synchronized public void updateTimingsAsynchronous() {
		
		for(int i=0; i < sessions.size(); i++) {
			sessions.get(i)[2] = getTime();
		}		
	}
	
	public int getTime() {
		
		int time;
		
		time = (int) (System.currentTimeMillis()/1000);
		
		return time;
	}
	
	public void update() {
		
		updateTimingsAsynchronous();
		writeToDB();
	}
	
	synchronized public void addSession(String id) {

		Object[] session = new Object[4];
		
		session[0] = id;
		session[1] = getTime();
		session[2] = getTime();
		sessions.add(session);
	}
	
	public void addEnchant(String id) {

	}
	
	public void addBlock(String id) {

	}
	
	public void addKill(String id) {

	}
	
}
