package eu.valkyr.cubestats;

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
			this.getLogger().info("cubestats is now tracking!");
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

		Object[] session = new Object[3];
		
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
