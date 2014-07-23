package eu.valkyr.cubestats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class cubestats extends JavaPlugin implements Listener {
	
	
	private MySQL sql;
	private BukkitTask writer;
	private ArrayList<Object[]> sessions = new ArrayList<Object[]>();
	private ArrayList<Object[]> enchants = new ArrayList<Object[]>();
	private ArrayList<Object[]> kills = new ArrayList<Object[]>();
	private ArrayList<Object[]> crafts = new ArrayList<Object[]>();
	private ArrayList<Object[]> smelts = new ArrayList<Object[]>();
	
	
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
			if(sql.isTable("enchants") == false){
    			try {
					sql.insert("CREATE TABLE enchants (UUID VARCHAR(36), count INT, item VARCHAR(20));");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			if(sql.isTable("crafts") == false){
    			try {
					sql.insert("CREATE TABLE crafts (UUID VARCHAR(36), count INT, item VARCHAR(20));");
				} catch (SQLException e) {
					e.printStackTrace();
				}
    		}
			if(sql.isTable("smelts") == false){
    			try {
					sql.insert("CREATE TABLE smelts (UUID VARCHAR(36), count INT, item VARCHAR(20));");
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
		addEnchant(enchanter.getUniqueId().toString(), item.getType().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void CraftItemEvent(Recipe recipe, InventoryView what, InventoryType.SlotType type, int slot, ClickType click, InventoryAction action) {
		addCraft(what.getPlayer().getUniqueId().toString(),recipe.getResult().toString(), what.getItem(slot).getAmount());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void FurnaceExtractEvent(Player player, Block block, Material itemType, int itemAmount, int exp)   {
		addSmelt(player.getUniqueId().toString(),itemType.toString(), itemAmount);
	}
	
	public void writeToDB() {
		
		session2db();
		enchant2db();
		craft2db();
		smelt2db();
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
	
	synchronized public void addEnchant(String id, String item) {
		
		Object[] enchant = new Object[3];
		
		enchant[0] = id;
		enchant[1] = item;
		enchants.add(enchant);
	}
	
	public void addKill(String id) {

	}
	
	synchronized public void addCraft(String id, String item, int count) {

		Object[] craft = new Object[4];
		
		craft[0] = id;
		craft[1] = item;
		craft[2] = count;
		crafts.add(craft);
	}
	
	synchronized public void addSmelt(String id, String item, int count) {

		Object[] smelt = new Object[4];
		
		smelt[0] = id;
		smelt[1] = item;
		smelt[2] = count;
		smelts.add(smelt);
	}
	
	public void session2db() {
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
				try {
					ResultSet re;
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
	
	public void enchant2db() {
		
		int pre = 0;
		
		for(int i=0; i < enchants.size(); i++) {
			try {
				ResultSet re;
				re = sql.query("SELECT count FROM enchants WHERE UUID='"+enchants.get(i)[0]+"' AND item='"+enchants.get(i)[1]+"';");
				while(re.next()) {
					pre = re.getInt("count");
				}
				re.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE enchants SET count='"+(pre+1)+"' WHERE UUID='"+enchants.get(i)[0]+"' AND item='"+enchants.get(i)[1]+"';");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				try {
					sql.insert("INSERT INTO enchants (UUID,count,item) VALUES ('"+enchants.get(i)[0]+"','1','"+enchants.get(i)[1]+"');");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			synchronized (enchants){
				enchants.remove(i);
			}
		}
	}
	
	public void craft2db() {
		
		int pre = 0;
		
		for(int i=0; i < crafts.size(); i++) {
			try {
				ResultSet re;
				re = sql.query("SELECT count FROM crafts WHERE UUID='"+crafts.get(i)[0]+"' AND item='"+crafts.get(i)[1]+"';");
				while(re.next()) {
					pre = re.getInt("count");
				}
				re.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE crafts SET count='"+(pre + (int)crafts.get(i)[2])+"' WHERE UUID='"+crafts.get(i)[0]+"' AND item='"+crafts.get(i)[1]+"';");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				try {
					sql.insert("INSERT INTO crafts (UUID,count,item) VALUES ('"+enchants.get(i)[0]+"','1','"+enchants.get(i)[1]+"');");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			synchronized (crafts){
				crafts.remove(i);
			}
		}
	}
	
public void smelt2db() {
		
		int pre = 0;
		
		for(int i=0; i < smelts.size(); i++) {
			try {
				ResultSet re;
				re = sql.query("SELECT count FROM smelts WHERE UUID='"+smelts.get(i)[0]+"' AND item='"+smelts.get(i)[1]+"';");
				while(re.next()) {
					pre = re.getInt("count");
				}
				re.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE smelts SET count='"+(pre + (int)smelts.get(i)[2])+"' WHERE UUID='"+smelts.get(i)[0]+"' AND item='"+smelts.get(i)[1]+"';");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				try {
					sql.insert("INSERT INTO smelts (UUID,count,item) VALUES ('"+smelts.get(i)[0]+"','1','"+smelts.get(i)[1]+"');");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			synchronized (smelts){
				smelts.remove(i);
			}
		}
	}
	
}
