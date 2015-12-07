package cn.edu.dufe.dufedata.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import cn.edu.dufe.dufedata.common.IModule;
import cn.edu.dufe.dufedata.plugin.IPlugin;
import cn.edu.dufe.dufedata.plugin.Plugin;

public interface IController extends IModule {
	public void loadPlugins();
	public void crawl(String PluginId,String[] args);
	public void crawl(Plugin plugin,String[] args);
	public void stop(Plugin plugin);
	public void stop(String pluginID);
}
