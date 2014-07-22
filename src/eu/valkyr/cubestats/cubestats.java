package eu.valkyr.cubestats;

import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class cubestats extends JavaPlugin implements Listener {
	
	private MySQL sql;
	private BukkitTask writer;
	
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
			        writeToDB();
			   }},0,this.getConfig().getInt("interval"));
		}
		else {
			this.getLogger().info("[ERROR]cubestats got no db connection... will not work");
		}
		this.saveDefaultConfig();
	}
	 
	public void onDisable() {
		writer.cancel();
		writeToDB();
		sql.close();
		this.getLogger().info("cubestats stopped tracking!");
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onLogin(PlayerLoginEvent event) {

	}
	
	public void writeToDB() {

	}
	
}
