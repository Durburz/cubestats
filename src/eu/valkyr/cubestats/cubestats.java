package eu.valkyr.cubestats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
	private ArrayList<Object[]> fishes = new ArrayList<Object[]>();
	
	
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
					sql.insert("CREATE TABLE kills (killerUUID VARCHAR(36), time INT, victimUUID VARCHAR(36));");
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
		getServer().getPluginManager().registerEvents(this, this);
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
	public void onLogin(PlayerLoginEvent event)  {
		
		addSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(PlayerQuitEvent event) {
		
		this.getLogger().info(event.getPlayer().getUniqueId().toString());
		session2db();
		delSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEnchant(EnchantItemEvent event) {
		
		addEnchant(event.getEnchanter().getUniqueId().toString(), event.getItem().getType().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onCraft(CraftItemEvent event) {
		
		addCraft(event.getWhoClicked().getUniqueId().toString(), event.getResult().toString(), event.getCurrentItem().getAmount());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSmelt(FurnaceExtractEvent event)   {
		
		addSmelt(event.getPlayer().getUniqueId().toString(), event.getItemType().toString(), event.getItemAmount());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent event) {
		
		addKill(event.getEntity().getKiller().getUniqueId().toString(),event.getEntity().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onKill(EntityDeathEvent event) {
		
		if(event.getEntity().getKiller() instanceof Player) {
			addKill(event.getEntity().getKiller().getUniqueId().toString(), event.getEntity().getUniqueId().toString());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onFish(PlayerFishEvent event) {
		
	}
	
	public void writeToDB() {
		
		session2db();
		enchant2db();
		craft2db();
		smelt2db();
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

		Object[] session = new Object[3];
		
		session[0] = id;
		session[1] = getTime();
		session[2] = getTime();
		sessions.add(session);
	}
	
	synchronized public void delSession(String id) {
		
		for(int i=0; i<sessions.size(); i++) {
			if(sessions.get(i)[0].equals(id)) {
				sessions.remove(i);
				break;
			}
		}
	} 	
	
	synchronized public void addEnchant(String id, String item) {
		
		Object[] enchant = new Object[2];
		
		enchant[0] = id;
		enchant[1] = item;
		enchants.add(enchant);
	}
	
	synchronized public void addKill(String killerid, String victimid) {
		
		Object[] kill = new Object[3];
		
		kill[0] = killerid;
		kill[1] = victimid;
		kill[2] = getTime();
		kills.add(kill);
	}
	
	synchronized public void addCraft(String id, String item, int count) {

		Object[] craft = new Object[3];
		
		craft[0] = id;
		craft[1] = item;
		craft[2] = count;
		crafts.add(craft);
	}
	
	synchronized public void addSmelt(String id, String item, int count) {

		Object[] smelt = new Object[3];
		
		smelt[0] = id;
		smelt[1] = item;
		smelt[2] = count;
		smelts.add(smelt);
	}
	
	synchronized public void addFish(String id, String item) {

		Object[] fish = new Object[2];
		
		fish[0] = id;
		fish[1] = item;
		fishes.add(fish);
	}
	
	public void session2db() {
		for(int i=0; i < sessions.size(); i++) {
			
			if (sessions.get(i)[3] != null) {
				try {
					sql.insert("UPDATE session SET end='"+sessions.get(i)[2]+"' WHERE sessid='"+sessions.get(i)[3]+"';");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			else {
				try {
					sql.insert("INSERT INTO session (UUID,start,end) VALUES ('"+sessions.get(i)[0]+"','"+sessions.get(i)[1]+"','"+sessions.get(i)[2]+"');");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
				try {
					ResultSet re;
					re = sql.query("SELECT sessid FROM session WHERE UUID='"+sessions.get(i)[0]+"' AND start='"+sessions.get(i)[1]+"';");
					while(re.next()) {
						sessions.get(i)[3] = re.getInt("sessid");
					}
					re.close();
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
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
			} catch (SQLException e) {
				this.getLogger().severe(e.getMessage());
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE enchants SET count='"+(pre+1)+"' WHERE UUID='"+enchants.get(i)[0]+"' AND item='"+enchants.get(i)[1]+"';");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			else {
				try {
					sql.insert("INSERT INTO enchants (UUID,count,item) VALUES ('"+enchants.get(i)[0]+"','1','"+enchants.get(i)[1]+"');");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			synchronized (enchants){
				enchants.remove(i);
			}
		}
	}
	
	public void kill2db() {
		
		for(int i=0; i < crafts.size(); i++) {

			try {
				sql.insert("INSERT INTO kills (killerUUID,time,victimUUID) VALUES ('"+kills.get(i)[0]+"','"+kills.get(i)[2]+"','"+kills.get(i)[1]+"');");
			} catch (SQLException e) {
				this.getLogger().severe(e.getMessage());
			}

			synchronized (kills){
				kills.remove(i);
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
			} catch (SQLException e) {
				this.getLogger().severe(e.getMessage());
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE crafts SET count='"+(pre + (int)crafts.get(i)[2])+"' WHERE UUID='"+crafts.get(i)[0]+"' AND item='"+crafts.get(i)[1]+"';");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			else {
				try {
					sql.insert("INSERT INTO crafts (UUID,count,item) VALUES ('"+enchants.get(i)[0]+"','1','"+enchants.get(i)[1]+"');");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
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
			} catch (SQLException e) {
				this.getLogger().severe(e.getMessage());
			}
			if(pre != 0) {
				try {
					sql.insert("UPDATE smelts SET count='"+(pre + (int)smelts.get(i)[2])+"' WHERE UUID='"+smelts.get(i)[0]+"' AND item='"+smelts.get(i)[1]+"';");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			else {
				try {
					sql.insert("INSERT INTO smelts (UUID,count,item) VALUES ('"+smelts.get(i)[0]+"','1','"+smelts.get(i)[1]+"');");
				} catch (SQLException e) {
					this.getLogger().severe(e.getMessage());
				}
			}
			synchronized (smelts){
				smelts.remove(i);
			}
		}
	}
	
	public void fish2db() {
		
	}
	
}
