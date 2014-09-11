package eu.valkyr.cubestats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class cubestats extends JavaPlugin implements Listener {
	
	
	private MySQL sql;
	private BukkitTask writer;
	private ArrayList<Object[]> sessions = new ArrayList<Object[]>();
	private boolean error = false;

	
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
					error = true;
					this.getLogger().severe("cubestats couldn't create table 'session'! will not work");
					this.getLogger().severe(e.toString());
				}
    		}
			if(sql.isTable("name") == false){
    			try {
					sql.insert("CREATE TABLE name (UUID VARCHAR(36) UNIQUE PRIMARY KEY, name VARCHAR(36));");
				} catch (SQLException e) {
					error = true;
					this.getLogger().severe("cubestats couldn't create table 'name'! will not work");
					this.getLogger().severe(e.toString());
				}
    		}
			if(sql.isTable("fish") == false){
    			try {
					sql.insert("CREATE TABLE name (fishid BIGINT PRIMARY KEY AUTO_INCREMENT, UUID VARCHAR(36), loot VARCHAR(36), time INT);");
				} catch (SQLException e) {
					error = true;
					this.getLogger().severe("cubestats couldn't create table 'fish'! will not work");
					this.getLogger().severe(e.toString());
				}
    		}
			if (!error) {
				writer = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
				    public void run() {
				    	update();
				   }},0,this.getConfig().getInt("interval")*20);
			}
		}
		else {
			error = true;
			this.getLogger().severe("cubestats got no db connection... will not work");
		}
		
		if (!error) {
			this.saveDefaultConfig();
			getServer().getPluginManager().registerEvents(this, this);
			addSession(Bukkit.getServerName());
			this.getLogger().info("cubestats is now tracking!");
		}
	}
	 
	public void onDisable() {
		
		writer.cancel();
		update();
		delSession(Bukkit.getServerName());
		sql.close();
		this.getLogger().info("cubestats stopped tracking!");
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLogin(PlayerLoginEvent event)  {
		
		try {
			sql.insert("IF NOT EXISTS (SELECT * FROM name WHERE UUID='"+ event.getPlayer().getUniqueId().toString() +"') "
					+ "INSERT INTO name (UUID, name) VALUES ("+ event.getPlayer().getUniqueId().toString() +", "+ event.getPlayer().getPlayerListName() +") END IF;");
		} catch (SQLException e) {
			this.getLogger().severe(e.getMessage());
		}
		addSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeave(PlayerQuitEvent event) {
		
		update();
		delSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onKick(PlayerKickEvent event) {
		
		update();
		delSession(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onFish(PlayerFishEvent event) {
		
		if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			
			Item loot = null;
			
			try {
				loot = (Item)event.getCaught();
			} catch (Exception e) {
				//TODO catch exception
			}
			
			if (loot != null) {
				//TODO log fish into db
				this.getLogger().info("Logging Fish!");
			}
		}
	}
	
	public void writeToDB() {
		
		session2db();
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
	
	synchronized public void delSession(String id) {
		
		for(int i=0; i<sessions.size(); i++) {
			if(sessions.get(i)[0].equals(id)) {
				sessions.remove(i);
				break;
			}
		}
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
}
