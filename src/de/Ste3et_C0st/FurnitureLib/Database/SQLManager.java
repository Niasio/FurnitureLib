package de.Ste3et_C0st.FurnitureLib.Database;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;

public class SQLManager {

	MySQL mysql;
	SQLite sqlite;
	FurnitureLib plugin;
	Integer sqlSaveIntervall;
	Connection con;
	
	public SQLManager(FurnitureLib plugin){
		this.plugin = plugin;initialize();
		initialize();
	}
	
	public void initialize(){
		if(plugin.getConfig().getString("config.Database.type").equalsIgnoreCase("SQLite")){
			String database = plugin.getConfig().getString("config.Database.database");
			this.sqlite = new SQLite(plugin, database);
			this.sqlite.load();
			this.con = this.sqlite.getSQLConnection();
		}else if(plugin.getConfig().getString("config.Database.type").equalsIgnoreCase("Mysql")){
			isExist();
			String database = plugin.getConfig().getString("config.Database.database");
			String user = plugin.getConfig().getString("config.Database.user");
			String password = plugin.getConfig().getString("config.Database.password");
			String port = plugin.getConfig().getString("config.Database.port");
			String host = plugin.getConfig().getString("config.Database.host");
			this.mysql = new MySQL(plugin, host, database, password, user, port);
			this.mysql.load();
			this.con = this.mysql.getSQLConnection();
		}else{
			plugin.getLogger().warning("Database Type not supported: Plugin shutdown");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}
	}
	
	public void loadALL(){
		if(this.sqlite!=null) this.sqlite.loadAll(SQLAction.NOTHING);
		if(this.mysql!=null) this.mysql.loadAll(SQLAction.NOTHING);
		FurnitureLib.getInstance().getFurnitureManager().sendAll();
	}
	
	private void isExist(){
		File fileDB = null;
		if(!plugin.getConfig().getBoolean("config.Database.importCheck")){return;}
		File folder = new File("plugins/" + plugin.getName());
		File[] array = folder.listFiles();
		if(array == null) return;
		for(File file : array){
			if(file==null) continue;
			if(file.getName().substring(file.getName().length() - 3, file.getName().length()).equalsIgnoreCase(".db")){
				plugin.getLogger().info("Old Database File found: " + file.getName());
				plugin.getLogger().info("Start importing");
				fileDB = file;
			}
		}
		if(fileDB!=null){
			this.sqlite = new SQLite(plugin, fileDB.getName().replace(".db", ""));
			this.sqlite.load();
			this.sqlite.loadAll(SQLAction.SAVE);
			plugin.getLogger().info("Import finish");
			this.sqlite.close();
			this.sqlite = null;
			plugin.getLogger().info("Make old Database unusable.");
			fileDB.renameTo(new File("plugins/" + plugin.getName(), fileDB.getName() + ".old"));
			fileDB.delete();
		}
	}
	
	public void save(){
		if(!plugin.getFurnitureManager().getObjectList().isEmpty()){
			List<ObjectID> objList = new ArrayList<ObjectID>();
			for(ObjectID obj : plugin.getFurnitureManager().getObjectList()){
				if(!objList.contains(obj)){
					switch (obj.getSQLAction()) {
					case UPDATE: remove(obj); save(obj); break;
					case SAVE: save(obj); break;
					case REMOVE:remove(obj);break;
					case NOTHING: break;
					case PURGE: break;
					}
					if(!obj.getSQLAction().equals(SQLAction.REMOVE)){obj.setSQLAction(SQLAction.NOTHING);}
					objList.add(obj);
					obj.setSQLAction(SQLAction.NOTHING);
				}
			}
		}
	}
	
	
	public void save(ObjectID obj){
		if(obj==null) return;
		try{
			if(this.sqlite!=null)this.sqlite.save(obj);
			if(this.mysql!=null)this.mysql.save(obj);
		}catch(Exception ex){
			initialize();
			if(this.sqlite!=null)this.sqlite.save(obj);
			if(this.mysql!=null)this.mysql.save(obj);
		}
	}
	
	public void remove(ObjectID obj){
		if(obj==null) return;
		try{
			if(this.sqlite!=null) this.sqlite.delete(obj);
			if(this.mysql!=null)this.mysql.delete(obj);
		}catch(Exception ex){
			initialize();
			if(this.sqlite!=null) this.sqlite.delete(obj);
			if(this.mysql!=null)this.mysql.delete(obj);
		}
	}
	
	public void saveIntervall(int time){
		sqlSaveIntervall=Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
				save();
				plugin.getLogger().info("Furniture Saved");
			}
		}, 0, 20*time);
	}

	public void stop() {
		if(sqlSaveIntervall!=null){
			plugin.getServer().getScheduler().cancelTask(sqlSaveIntervall);
			sqlSaveIntervall = null;
		}
	}

	public void close() {
		if(this.sqlite!=null){
			this.sqlite.close();
			this.sqlite=null;
		}else if(this.mysql!=null){
			this.mysql.close();
			this.mysql=null;
		}
	}
}
