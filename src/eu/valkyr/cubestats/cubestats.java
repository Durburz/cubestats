package eu.valkyr.cubestats;

import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class cubestats extends JavaPlugin implements Listener {
	
	private MySQL sql;
	
	public void onEnable() {
		sql = new MySQL(Logger.getLogger("Minecraft"), 
				"[cubestats]",
	            "localhost", 
	            3306, 
	            "cubestats", 
	            "mc", 
	            "Z0mbi3");
		if (sql.open()) {
			this.getLogger().info("cubestats got db connection...");
			this.getLogger().info("cubestats is now tracking!");
			Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			    public void run() {
			        writeToDB();
			   }},0,200);
		}
		else {
			this.getLogger().info("[ERROR]cubestats got no db connection... will not work");
		}
	}
	 
	public void onDisable() {
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
